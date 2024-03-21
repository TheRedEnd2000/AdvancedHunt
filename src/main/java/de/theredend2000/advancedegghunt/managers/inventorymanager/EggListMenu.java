package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class EggListMenu extends PaginatedInventoryMenu {

    public EggListMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Eggs list", (short) 54);
    }

    public void open() {
        super.addMenuBorder();
        addMenuBorder();
        setMenuItems();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuBorder() {
        inventoryContent[48] = new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Left").build();

        inventoryContent[50] = new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Right").build();

        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK).setDisplayname("§aRefresh").build();
        String selectedSection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventoryContent[45] = new ItemBuilder(XMaterial.PAPER).setDisplayname("§bSelected Collection").setLore("§7Shows your currently selected collection.", "", "§7Current: §6" + selectedSection, "", "§eClick to change.").build();
    }

    public void setMenuItems() {
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Eggs Placed").setLore("§7You can add eggs by using", "§e/egghunt placeEggs§7.").build());

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

            String x = placedEggs.getString("PlacedEggs." + keys.get(index) + ".X");
            String y = placedEggs.getString("PlacedEggs." + keys.get(index) + ".Y");
            String z = placedEggs.getString("PlacedEggs." + keys.get(index) + ".Z");
            int random = new Random().nextInt(7);
            String date = Main.getInstance().getEggManager().getEggDatePlaced(keys.get(index), collection);
            String time = Main.getInstance().getEggManager().getEggTimePlaced(keys.get(index), collection);
            String timesFound = String.valueOf(Main.getInstance().getEggManager().getTimesFound(keys.get(index), collection));
            getInventory().setItem(slotIndex, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(random)).setDisplayname("§2§lEgg §7(ID#" + keys.get(index) + ")").setLore("§9Location:", "§7X: §e" + x, "§7Y: §e" + y, "§7Z: §e" + z, "", "§9Information:", "§7Times found: §6" + timesFound, "", "§9Placed:", "§7Date: §6" + date, "§7Time: §6" + time, "", "§eLEFT-CLICK to teleport.", "§eRIGHT-CLICK for information.").setLocalizedName(keys.get(index)).build());
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }
    public int getMaxPages(){
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        int keys = Main.getInstance().getEggManager().getMaxEggs(collection);
        if(keys == 0) return 1;
        return (int) Math.ceil((double) keys / getMaxItemsPerPage());
    }

    @Override
    public String getMenuName() {
        return null;
    }

    @Override
    public int getSlots() {
        return 0;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player player = (Player) event.getWhoClicked();

        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
            for(String id : placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false)){
                if (!Objects.requireNonNull(event.getCurrentItem().getItemMeta()).getLocalizedName().equals(id)) {
                    continue;
                }
                if(event.getAction() == InventoryAction.PICKUP_ALL){
                    ConfigLocationUtil location = new ConfigLocationUtil(Main.getInstance(), "PlacedEggs." + id);
                    if (location.loadLocation(collection) != null)
                        player.teleport(location.loadLocation(collection).add(0.5, 0, 0.5));
                    player.closeInventory();
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.TELEPORT_TO_EGG).replaceAll("%ID%", id));
                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                }else if(event.getAction() == InventoryAction.PICKUP_HALF){
                    new EggInformationMenu(Main.getPlayerMenuUtility(player)).open(id);
                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                }
            }
        }

        if(event.getCurrentItem().getType().equals(Material.PAPER) &&
                ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Selected Collection")){
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
                }
                break;
        }
    }
}

