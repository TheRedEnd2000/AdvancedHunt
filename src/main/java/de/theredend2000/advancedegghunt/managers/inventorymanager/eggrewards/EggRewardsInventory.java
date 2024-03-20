package de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.presets.PresetDataManager;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import de.tr7zw.changeme.nbtapi.NBT;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class EggRewardsInventory implements Listener {

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

    public EggRewardsInventory(){
        this.plugin = Main.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        messageManager = plugin.getMessageManager();
        title = "Egg Rewards";
    }

    public void open(Player owner, String id, String collection){
        this.owner = owner;
        this.collection = collection;
        this.id = id;
        inventory = Bukkit.createInventory(owner, 54, title);
        fill();
        this.owner.openInventory(inventory);
    }

    public void fill(){
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        inventory.setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Left").build());

        inventory.setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Right").build());
        inventory.setItem(45, new ItemBuilder(XMaterial.EMERALD_BLOCK).setDisplayname("§5Save preset").setLore("", "§7Saves the current listed commands", "§7in a preset that you can load", "§7for other eggs again.", "", "§2Note: §7You need at least 1 command to save a preset!", "", "§eClick to save a new preset.").build());
        inventory.setItem(46, new ItemBuilder(XMaterial.EMERALD).setDisplayname("§5Load presets").setLore("§eClick to load or change presets.").build());
        inventory.setItem(53, new ItemBuilder(XMaterial.GOLD_INGOT).setDisplayname("§5Create new reward").setLore("", "§bYou can also add custom items:", "§7For that get your custom item in your", "§7inventory and click it when this", "§7menu is open. The item will", "§7get converted into an command", "§7and can then used as the other commands.", "", "§eClick to create a new reward").build());
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§cClose").build());
        setMenuItems();
    }

    public void setMenuItems() {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs." + id + ".Rewards")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs." + id + ".Rewards").getKeys(false));
        }else
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Rewards").setLore("§7Create new a new reward","§7or load a preset.").build());
        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String command = placedEggs.getString("PlacedEggs." + id + ".Rewards." + keys.get(index) + ".command").replaceAll("§","&");
                    boolean enabled = placedEggs.getBoolean("PlacedEggs." + id + ".Rewards." + keys.get(index) + ".enabled");
                    boolean foundAll = placedEggs.getBoolean("PlacedEggs." + id + ".Rewards." + keys.get(index) + ".foundAll");
                    inventory.addItem(new ItemBuilder(XMaterial.PAPER).setDisplayname("§b§lReward §7#" + keys.get(index)).setLore("","§9Information:","§7Command: §6" + command,"§7Command Enabled: " + (enabled ? "§atrue" : "§cfalse"),"§7Action on: " + (foundAll ? "§6Found all eggs" : "§6Found one egg"),"","§eLEFT-CLICK to toggle enabled.","§eMIDDLE-CLICK to toggle action.","§eRIGHT-CLICK to delete.").setLocalizedName(keys.get(index)).build());
                }
            }
        }else
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Rewards").setLore("§7Create new a new reward","§7or load a preset.").build());
    }

    @EventHandler
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        if(!e.getView().getTitle().equals(title)) return;
        if(e.getCurrentItem() == null) return;
        e.setCancelled(true);

        if(e.getClickedInventory().equals(p.getInventory())){
            convertItemIntoCommand(e.getCurrentItem(),id,collection);
            p.sendMessage("§aSuccessfully added a new item.");
            reopen();
        }

        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs." + id + ".Rewards.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs." + id + ".Rewards.").getKeys(false));
            for(String commandID : placedEggs.getConfigurationSection("PlacedEggs." + id + ".Rewards.").getKeys(false)){
                if(Objects.requireNonNull(e.getCurrentItem().getItemMeta()).getLocalizedName().equals(commandID)){
                    if(e.getAction() == InventoryAction.PICKUP_ALL){
                        placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".enabled", !placedEggs.getBoolean("PlacedEggs." + id + ".Rewards." + commandID + ".enabled"));
                        plugin.getEggDataManager().savePlacedEggs(collection,placedEggs);
                    }else if(e.getAction() == InventoryAction.PICKUP_HALF){
                        p.sendMessage(messageManager.getMessage(MessageKey.COMMAND_DELETE).replaceAll("%ID%",commandID));
                        placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID,null);
                        plugin.getEggDataManager().savePlacedEggs(collection,placedEggs);
                    }else if(e.getAction() == InventoryAction.CLONE_STACK){
                        placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".foundAll", !placedEggs.getBoolean("PlacedEggs." + id + ".Rewards." + commandID + ".foundAll"));
                        plugin.getEggDataManager().savePlacedEggs(collection,placedEggs);
                    }
                    reopen();
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
            }
        }

        XMaterial material = XMaterial.matchXMaterial(e.getCurrentItem());
        switch (material) {
            case BARRIER:
                p.closeInventory();
                p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case GOLD_INGOT:
                p.closeInventory();
                Main.getInstance().getPlayerAddCommand().put(p, 120);
                TextComponent c = new TextComponent("\n\n\n\n\n" + Main.getInstance().getMessageManager().getMessage(MessageKey.NEW_COMMAND) + "\n\n");
                TextComponent clickme = new TextComponent("§9-----------§3§l[PLACEHOLDERS] §7(Hover)§9-----------");
                clickme.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§2Available placeholders:\n§b- %PLAYER% --> Name of the player\n§b- & --> For color codes (&6=gold)\n§b- %EGGS_FOUND% --> How many eggs the player has found\n§b- %EGGS_MAX% --> How many eggs are placed\n§b- %PREFIX% --> The prefix of the plugin")));
                c.addExtra(clickme);
                p.spigot().sendMessage(c);
                FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(p.getUniqueId());
                playerConfig.set("Change.collection", collection);
                playerConfig.set("Change.id", id);
                Main.getInstance().getPlayerEggDataManager().savePlayerData(p.getUniqueId(), playerConfig);
                p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case EMERALD_BLOCK:
                if (placedEggs.contains("PlacedEggs." + id + ".Rewards.0")) {
                    new AnvilGUI.Builder()
                            .onClose(stateSnapshot -> {
                                if (!stateSnapshot.getText().isEmpty()) {
                                    PresetDataManager presetDataManager = Main.getInstance().getPresetDataManager();
                                    String preset = stateSnapshot.getText();
                                    if (!presetDataManager.containsPreset(preset)) {
                                        presetDataManager.createPresetFile(stateSnapshot.getText());
                                        presetDataManager.loadCommandsIntoPreset(preset, collection, id);
                                        reopen();
                                        p.sendMessage(messageManager.getMessage(MessageKey.PRESET_SAVED).replaceAll("%PRESET%", preset));
                                    } else {
                                        p.sendMessage(messageManager.getMessage(MessageKey.PRESET_ALREADY_EXISTS).replaceAll("%PRESET%", preset));
                                    }
                                }
                            })
                            .onClick((slot, stateSnapshot) -> {
                                return Collections.singletonList(AnvilGUI.ResponseAction.close());
                            })
                            .text("enter name")
                            .title("Preset name")
                            .plugin(Main.getInstance())
                            .open(p);
                    p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else
                    p.sendMessage(messageManager.getMessage(MessageKey.PRESET_FAILED_COMMANDS));
                break;
            case EMERALD:
                Main.getInstance().getPresetsInventory().open(owner, id, collection);
                break;
            case PLAYER_HEAD:
                if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")) {
                    if (page == 0) {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                        p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    } else {
                        page = page - 1;
                        reopen();
                        p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= keys.size())) {
                        page = page + 1;
                        reopen();
                        p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    } else {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                        p.playSound(p.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    }
                }
                break;
        }
    }

    public void reopen(){
        open(owner,id,collection);
    }


    public int getMaxPages(){
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs." + id + ".Rewards")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs." + id + ".Rewards.").getKeys(false));
        }
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / maxItemsPerPage);
    }

    public void convertItemIntoCommand(ItemStack itemStack,String id,String collection){
        String itemNBT = NBT.get(itemStack, Object::toString);
        addCommand(id,MessageFormat.format("give %PLAYER% {0}{1}", itemStack.getType().name().toLowerCase(), itemNBT),collection);
    }

    private void addCommand(String id, String command, String collection){
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        if (placedEggs.contains("PlacedEggs." + id + ".Rewards.")) {
            ConfigurationSection rewardsSection = placedEggs.getConfigurationSection("PlacedEggs." + id + ".Rewards.");
            int nextNumber = 0;
            Set<String> keys = rewardsSection.getKeys(false);
            if (!keys.isEmpty()) {
                for (int i = 0; i <= keys.size(); i++) {
                    String key = Integer.toString(i);
                    if (!keys.contains(key)) {
                        nextNumber = i;
                        break;
                    }
                }
            }
            setConfiguration(String.valueOf(nextNumber),id , command, collection);
        } else {
            setConfiguration("0",id , command, collection);
        }
    }

    private void setConfiguration(String commandID,String id, String command,String collection){
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".command", command);
        placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".enabled", true);
        placedEggs.set("PlacedEggs." + id + ".Rewards." + commandID + ".foundAll", false);
        Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
    }

}
