package de.theredend2000.advancedegghunt.managers.inventorymanager.eggplacelist;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.CollectionSelectMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
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

public class EggPlaceMenu extends PlacePaginatedMenu {
    private MessageManager messageManager;

    public EggPlaceMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
        messageManager = Main.getInstance().getMessageManager();
    }

    @Override
    public String getMenuName() {
        return "Eggs place list";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player p = (Player) event.getWhoClicked();

        if(super.playerMenuUtility.getOwner().getInventory().equals(event.getClickedInventory())) { //TODO: Check working as intended
            Set<String> keys = Main.getInstance().getPluginConfig().getPlaceEggIds();
            for(String key : keys){
                if(event.getCurrentItem().getType().name().equalsIgnoreCase(Main.getInstance().getPluginConfig().getPlaceEggType(key))){
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
            Main.getInstance().getPluginConfig().saveData();
            this.open();
        }

        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().getPluginConfig().hasPlaceEggs()){
            keys.addAll(Main.getInstance().getPluginConfig().getPlaceEggIds());
            for(String id : keys){
                if(Objects.requireNonNull(event.getCurrentItem().getItemMeta()).getLocalizedName().equals(id)){;
                    p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    if(event.getCurrentItem().getType().equals(XMaterial.PLAYER_HEAD.parseMaterial()))
                        p.getInventory().addItem(new ItemBuilder(XMaterial.matchXMaterial(event.getCurrentItem().getType())).setSkullOwner(Main.getTexture(Main.getInstance().getPluginConfig().getPlaceEggTexture(id))).setDisplayname("§6Easter Egg").setLore("§7Place this egg around the map", "§7that everyone can search and find it.").build());
                    else
                        p.getInventory().addItem(new ItemBuilder(XMaterial.matchXMaterial(event.getCurrentItem().getType())).setDisplayname("§6Easter Egg").setLore("§7Place this egg around the map", "§7that everyone can search and find it.").build());
                }
            }
        }

        if(event.getCurrentItem().getType().equals(Material.PAPER) && ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Selected Collection")){
            new CollectionSelectMenu(Main.getPlayerMenuUtility(p)).open();
        }

        XMaterial material = XMaterial.matchXMaterial(event.getCurrentItem());
        switch (material) {
            case BARRIER:
                p.closeInventory();
                p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case EMERALD_BLOCK:
                if (Main.getInstance().getRefreshCooldown().containsKey(p.getName())) {
                    if (Main.getInstance().getRefreshCooldown().get(p.getName()) > System.currentTimeMillis()) {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.WAIT_REFRESH));
                        p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                        return;
                    }
                }
                Main.getInstance().getRefreshCooldown().put(p.getName(), System.currentTimeMillis() + (3 * 1000));
                new EggPlaceMenu(Main.getPlayerMenuUtility(p)).open();
                p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case PLAYER_HEAD:
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")) {
                    if (page == 0) {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                        p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        page = page - 1;
                        super.open();
                        p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= keys.size())) {
                        page = page + 1;
                        super.open();
                        p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                        p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Information")) {
                    TextComponent c = new TextComponent("§7Join the discord to get all information how to add custom egg textures. \n");
                    TextComponent clickme = new TextComponent("§6§l[CLICK HERE]");
                    clickme.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§7Click to join.")));
                    clickme.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/hNj9erE5EA"));
                    c.addExtra(clickme);
                    p.spigot().sendMessage(c);
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().getPluginConfig().hasPlaceEggs()){
            keys.addAll(Main.getInstance().getPluginConfig().getPlaceEggIds());
        }else
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Eggs").setLore("§7You can add commands by using", "§e/egghunt placeEggs§7.").build());
        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    XMaterial mat = Main.getInstance().getMaterial(Objects.requireNonNull(Main.getInstance().getPluginConfig().getPlaceEggType(keys.get(index))).toUpperCase());
                    if(mat.equals(XMaterial.PLAYER_HEAD))
                        inventory.addItem(new ItemBuilder(mat).setSkullOwner(Main.getTexture(Main.getInstance().getPluginConfig().getPlaceEggTexture(keys.get(index)))).setDisplayname("§b§lEggs Type #" + keys.get(index)).setLore("§eClick to get.").setLocalizedName(keys.get(index)).build());
                    else
                        inventory.addItem(new ItemBuilder(mat).setDisplayname("§b§lEggs Type #" + keys.get(index)).setLore("§eClick to get.").setLocalizedName(keys.get(index)).build());
                }
            }
        }
    }
}

