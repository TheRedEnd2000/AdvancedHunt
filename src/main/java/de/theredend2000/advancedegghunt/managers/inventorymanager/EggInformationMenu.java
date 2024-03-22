package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class EggInformationMenu extends PaginatedInventoryMenu {

    public EggInformationMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Egg information", (short) 54);

        super.addMenuBorder();
        addMenuBorderButtons();
    }

    public void open(String eggId) {
        getInventory().setContents(inventoryContent);
        setMenuItems(eggId);

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuBorderButtons(){
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK).setDisplayname("§aRefresh").build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("NWYxMzNlOTE5MTlkYjBhY2VmZGMyNzJkNjdmZDg3YjRiZTg4ZGM0NGE5NTg5NTg4MjQ0NzRlMjFlMDZkNTNlNiJ9fX0=")).setDisplayname("§eBack").build();
        String selectedSection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventoryContent[46] = new ItemBuilder(XMaterial.PAPER).setDisplayname("§bSelected Collection").setLore("§7Shows your currently selected collection.", "", "§7Current: §6" + selectedSection, "", "§eClick to change.").build();
    }

    public void setMenuItems(String eggId) {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages(eggId) + "§7)", "", "§eClick to scroll.").setDisplayname("§2Left")
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages(eggId) + "§7)", "", "§eClick to scroll.").setDisplayname("§2Right")
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).build());

        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> uuid = new ArrayList<>();
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids);
            if(playerConfig.contains("FoundEggs." + collection + "." + eggId)){
                Collections.addAll(keys, playerConfig.getString("FoundEggs." + collection + ".Name"));
                Collections.addAll(uuid, String.valueOf(uuids));
            }
        }

        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String maxEggs = String.valueOf(Main.getInstance().getEggManager().getMaxEggs(collection));
                    String date = Main.getInstance().getEggManager().getEggDateCollected(uuid.get(index), eggId, collection);
                    String time = Main.getInstance().getEggManager().getEggTimeCollected(uuid.get(index), eggId, collection);
                    String eggsFound = Main.getInstance().getPlayerEggDataManager().getPlayerData(UUID.fromString(uuid.get(index))).getString("FoundEggs." + collection + ".Count");
                    getInventory().addItem(new ItemBuilder(XMaterial.PLAYER_HEAD)
                            .setOwner(keys.get(index))
                            .setDisplayname("§6§l" + keys.get(index) + " §7(" + uuid.get(index) + ")")
                            .setLore("§7" + keys.get(index) + " has found the §2egg #" + eggId + "§7.", "", "§9Information of " + keys.get(index) + ":", "§7Eggs found: §6" + eggsFound + "/" + maxEggs, "", "§9Collected:", "§7Date: §6" + date, "§7Time: §6" + time)
                            .setLocalizedName(keys.get(index)).build());
                }
            }
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Founds").setLore("§7No player has found this egg yet.").build());
    }


    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        String id = getInventory().getItem(0).getItemMeta().getLocalizedName();
        ArrayList<String> keys = new ArrayList<>();
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()){
            if(Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).contains("FoundEggs." + id)){
                Collections.addAll(keys, Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getString("FoundEggs." + collection + ".Name"));
            }
        }

        if(event.getCurrentItem().getType().equals(Material.PAPER) && ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Selected Collection")){
            new CollectionSelectMenu(Main.getPlayerMenuUtility(p)).open();
        }

        XMaterial material = XMaterial.matchXMaterial(event.getCurrentItem());
        switch (material) {
            case BARRIER:
                p.closeInventory();
                p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case EMERALD_BLOCK:
                if (Main.getInstance().getRefreshCooldown().containsKey(p.getName())) {
                    if (Main.getInstance().getRefreshCooldown().get(p.getName()) > System.currentTimeMillis()) {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.WAIT_REFRESH));
                        p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                        return;
                    }
                }
                Main.getInstance().getRefreshCooldown().put(p.getName(), System.currentTimeMillis() + (3 * 1000));
                this.open(getInventory().getItem(0).getItemMeta().getLocalizedName());
                p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case PLAYER_HEAD:
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")) {
                    if (page == 0) {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                        p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    } else {
                        page = page - 1;
                        this.open(id);
                        p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= keys.size())) {
                        page = page + 1;
                        this.open(id);
                        p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    } else {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                        p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Back")) {
                    new EggListMenu(Main.getPlayerMenuUtility(p)).open();
                    p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }

    public int getMaxPages(String eggId){
        ArrayList<String> keys = new ArrayList<>();
        for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids);
            if(playerConfig.contains("FoundEggs." + eggId)){
                Collections.addAll(keys, playerConfig.getString("FoundEggs.Name"));
            }
        }
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / getMaxItemsPerPage());
    }
}

