package de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.presets;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;

public class PresetsInventory implements Listener {

    private Main plugin;
    private Inventory inventory;
    private MessageManager messageManager;
    private Player owner;
    private String id;
    private String collection;
    private String title;
    int page = 0;
    int maxItemsPerPage = 28;
    int index = 0;

    public PresetsInventory(){
        this.plugin = Main.getInstance();
        Bukkit.getPluginManager().registerEvents(this,plugin);
        messageManager = plugin.getMessageManager();
        title = "Presets";
    }

    public void open(Player owner,String id, String collection){
        this.owner = owner;
        this.collection = collection;
        this.id = id;
        inventory = Bukkit.createInventory(owner,54,title);
        fill();
        this.owner.openInventory(inventory);
    }

    public void fill(){
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        inventory.setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)","","§eClick to scroll.").setDisplayname("§2Left").build());

        inventory.setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)","","§eClick to scroll.").setDisplayname("§2Right").build());
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§cClose").build());
        inventory.setItem(45, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
        setMenuItems();
    }

    public void setMenuItems() {
        PresetDataManager presetDataManager = Main.getInstance().getPresetDataManager();
        ArrayList<String> keys = new ArrayList<>();
        if(presetDataManager.savedPresets().size() >= 1){
            keys.addAll(presetDataManager.savedPresets());
        }else
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Presets").setLore("§7Create new one to select them.").build());
        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String defaultPreset = plugin.getPluginConfig().getDefaultLoadingPreset();
                    inventory.addItem(new ItemBuilder(XMaterial.PAPER).setDisplayname("§b§l" + keys.get(index)).setDefaultLore(presetDataManager.getAllCommandsAsLore(keys.get(index),keys.get(index).equals(defaultPreset))).setLocalizedName(keys.get(index)).build());
                }
            }
        }else{
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Presets").setLore("§7Create new one to select them.").build());
        }
    }

    @EventHandler
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        PresetDataManager presetDataManager = Main.getInstance().getPresetDataManager();
        if(!e.getView().getTitle().equals(title)) return;
        if(e.getCurrentItem() == null) return;
        e.setCancelled(true);

        for(String presetName : presetDataManager.savedPresets()){
            if(e.getCurrentItem() == null || e.getCurrentItem().getItemMeta() == null) continue;
            if(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equals(presetName)){
                if(e.getAction() == InventoryAction.PICKUP_ALL){
                    p.sendMessage(messageManager.getMessage(MessageKey.PRESET_LOADED).replaceAll("%PRESET%",presetName));
                    presetDataManager.loadPresetIntoEggCommands(presetName,collection,id);
                    plugin.getEggRewardsInventory().open(owner,id,collection);
                }else if(e.getAction() == InventoryAction.PICKUP_HALF){
                    if(!plugin.getPluginConfig().getDefaultLoadingPreset().equals(presetName)) {
                        presetDataManager.deletePreset(presetName);
                        p.sendMessage(messageManager.getMessage(MessageKey.PRESET_DELETE).replaceAll("%PRESET%",presetName));
                    }else
                        p.sendMessage(messageManager.getMessage(MessageKey.PRESET_NOT_DELETE_DEFAULT).replaceAll("%PRESET%",presetName));
                    reopen();
                }else if(e.getAction() == InventoryAction.CLONE_STACK){
                    plugin.getPluginConfig().setDefaultLoadingPreset(presetName);
                    p.sendMessage(messageManager.getMessage(MessageKey.PRESET_DEFAULT).replaceAll("%PRESET%",presetName));
                    reopen();
                }
                p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
            }
        }

        if (e.getCurrentItem().getType().equals(Material.BARRIER)) {
            p.closeInventory();
            p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
        }else if(e.getCurrentItem().getType().equals(Material.PLAYER_HEAD)){
            if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")){
                if (page == 0){
                    p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventoryFailedSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }else{
                    page = page - 1;
                    reopen();
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
            }else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")){
                if (!((index + 1) >= presetDataManager.savedPresets().size())){
                    page = page + 1;
                    reopen();
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }else{
                    p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventoryFailedSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
            }else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Back")){
                p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                plugin.getEggRewardsInventory().open(owner,id,collection);
            }
        }
    }

    public void reopen(){
        open(owner,id,collection);
    }


    public int getMaxPages(){
        PresetDataManager presetDataManager = Main.getInstance().getPresetDataManager();
        ArrayList<String> keys = new ArrayList<>(presetDataManager.savedPresets());
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / maxItemsPerPage);
    }

}
