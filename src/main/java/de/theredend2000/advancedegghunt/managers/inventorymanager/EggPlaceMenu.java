package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBTList;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class EggPlaceMenu extends PaginatedInventoryMenu {
    private MessageManager messageManager;

    public EggPlaceMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Eggs place list", (short) 54);
        messageManager = Main.getInstance().getMessageManager();

        super.addMenuBorder();
        addMenuBorderButtons();
    }

    public void open() {
        getInventory().setContents(inventoryContent);
        setMenuItems();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuBorderButtons() {
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK).setDisplayname("§aRefresh").build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setSkullOwner(Main.getTexture("MTY0MzlkMmUzMDZiMjI1NTE2YWE5YTZkMDA3YTdlNzVlZGQyZDUwMTVkMTEzYjQyZjQ0YmU2MmE1MTdlNTc0ZiJ9fX0="))
                .setDisplayname("§9Information")
                .setLore("§7If you do not know how you can add your",
                        "§7own egg textures. Click here",
                        "§7to get to the discord channel",
                        "§7where you can see how it will work.",
                        "",
                        "§aYou can find the post under:",
                        "§a§lfaq -> How to add Custom egg textures in the AdvancedEggHunt plugin§a",
                        "",
                        "§9Click any block in your inventory",
                        "§9to add it into the list.",
                        "",
                        "§eClick to get the discord link.").build();
        String selectedSection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventoryContent[46] = new ItemBuilder(XMaterial.PAPER)
                .setDisplayname("§bSelected Collection")
                .setLore("§7Shows your currently selected collection.", "", "§7Current: §6" + selectedSection, "", "§eClick to change.").build();
    }

    public void setMenuItems() {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Left")
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Right")
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).build());

        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().getPluginConfig().hasPlaceEggs()){
            keys.addAll(Main.getInstance().getPluginConfig().getPlaceEggIds());
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Eggs").setLore("§7You can add commands by using", "§e/egghunt placeEggs§7.").build());
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for(int i = 0; i < getMaxItemsPerPage(); i++) {
            index = getMaxItemsPerPage() * page + i;
            if(index >= keys.size()) break;
            if (keys.get(index) == null) {
                continue;
            }
            int slotIndex = ((9 + 1) + ((i / 7) * 9) + (i % 7));

            XMaterial mat = Main.getInstance().getMaterial(Objects.requireNonNull(Main.getInstance().getPluginConfig().getPlaceEggType(keys.get(index))).toUpperCase());
            if(mat.equals(XMaterial.PLAYER_HEAD))
                getInventory().setItem(slotIndex, new ItemBuilder(mat).setSkullOwner(Main.getTexture(Main.getInstance().getPluginConfig().getPlaceEggTexture(keys.get(index)))).setDisplayname("§b§lEggs Type #" + keys.get(index)).setLore("§eClick to get.").setLocalizedName(keys.get(index)).build());
            else
                getInventory().setItem(slotIndex, new ItemBuilder(mat).setDisplayname("§b§lEggs Type #" + keys.get(index)).setLore("§eClick to get.").setLocalizedName(keys.get(index)).build());
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }
    public int getMaxPages(){
        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().getPluginConfig().hasPlaceEggs()){
            keys.addAll(Main.getInstance().getPluginConfig().getPlaceEggIds());
        }
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / getMaxItemsPerPage());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player player = (Player) event.getWhoClicked();

        if(super.playerMenuUtility.getOwner().getInventory().equals(event.getClickedInventory())) { //TODO: Check working as intended
            Set<String> keys = Main.getInstance().getPluginConfig().getPlaceEggIds();
            String fullTexture = NBT.get(event.getCurrentItem(), nbt -> {
                final ReadableNBT skullOwnerCompound = nbt.getCompound("SkullOwner");
                if (skullOwnerCompound == null) return null;
                ReadableNBT skullOwnerPropertiesCompound = skullOwnerCompound.getCompound("Properties");
                if (skullOwnerPropertiesCompound == null) return null;
                ReadableNBTList<ReadWriteNBT> skullOwnerPropertiesTexturesCompound = skullOwnerPropertiesCompound.getCompoundList("textures");
                if (skullOwnerPropertiesTexturesCompound == null) return null;

                return skullOwnerPropertiesTexturesCompound.get(0).getString("Value");
            });

            if (fullTexture != null) fullTexture = fullTexture.replaceFirst(".+?mUv", "");
            for(String key : keys){
                if(event.getCurrentItem().getType().name().equalsIgnoreCase(Main.getInstance().getPluginConfig().getPlaceEggType(key)) &&
                        !(event.getCurrentItem().getType().name().equalsIgnoreCase(XMaterial.PLAYER_HEAD.name()) &&
                                fullTexture != null &&
                                !Objects.equals(Main.getInstance().getPluginConfig().getPlaceEggTexture(key), fullTexture))) {
                    super.playerMenuUtility.getOwner().sendMessage(messageManager.getMessage(MessageKey.BLOCK_LISTED));
                    return;
                }
            }

            int nextNumber = 0;
            if (!keys.isEmpty()) {
                for (int i = 0; i <= keys.size(); i++) {
                    String key = Integer.toString(i);
                    if (!keys.contains(key)) {
                        nextNumber = i;
                        break;
                    }
                }
            }
            Main.getInstance().getPluginConfig().setPlaceEggType(nextNumber, event.getCurrentItem().getType().name().toUpperCase());
            if (fullTexture != null) {
                Main.getInstance().getPluginConfig().setPlaceEggTexture(nextNumber, fullTexture);
            }
            Main.getInstance().getPluginConfig().saveData();
            this.open();
            return;
        }

        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().getPluginConfig().hasPlaceEggs()){
            keys.addAll(Main.getInstance().getPluginConfig().getPlaceEggIds());
            for(String id : keys){
                if (!Objects.requireNonNull(event.getCurrentItem().getItemMeta()).getLocalizedName().equals(id)) {
                    continue;
                }
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                if(event.getCurrentItem().getType().equals(XMaterial.PLAYER_HEAD.parseMaterial()))
                    player.getInventory().addItem(new ItemBuilder(XMaterial.matchXMaterial(event.getCurrentItem().getType())).setSkullOwner(Main.getTexture(Main.getInstance().getPluginConfig().getPlaceEggTexture(id))).setDisplayname("§6Easter Egg").setLore("§7Place this egg around the map", "§7that everyone can search and find it.").build());
                else
                    player.getInventory().addItem(new ItemBuilder(XMaterial.matchXMaterial(event.getCurrentItem().getType())).setDisplayname("§6Easter Egg").setLore("§7Place this egg around the map", "§7that everyone can search and find it.").build());
                return;
            }
        }

        if(event.getCurrentItem().getType().equals(Material.PAPER) && ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Selected Collection")){
            new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
            return;
        }

        XMaterial material = XMaterial.matchXMaterial(event.getCurrentItem());
        switch (material) {
            case BARRIER:
                player.closeInventory();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case EMERALD_BLOCK:
                if (Main.getInstance().getRefreshCooldown().containsKey(player.getName())) {
                    if (Main.getInstance().getRefreshCooldown().get(player.getName()) > System.currentTimeMillis()) {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.WAIT_REFRESH));
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                        return;
                    }
                }
                Main.getInstance().getRefreshCooldown().put(player.getName(), System.currentTimeMillis() + (3 * 1000));
                open();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case PLAYER_HEAD:
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")) {
                    if (page == 0) {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        page = page - 1;
                        open();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= keys.size())) {
                        page = page + 1;
                        open();
                        player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Information")) {
                    TextComponent c = new TextComponent("§7Join the discord to get all information how to add custom egg textures. \n");
                    TextComponent clickme = new TextComponent("§6§l[CLICK HERE]");
                    clickme.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§7Click to join.")));
                    clickme.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/hNj9erE5EA"));
                    c.addExtra(clickme);
                    player.spigot().sendMessage(c);
                }
                break;
        }
    }
}

