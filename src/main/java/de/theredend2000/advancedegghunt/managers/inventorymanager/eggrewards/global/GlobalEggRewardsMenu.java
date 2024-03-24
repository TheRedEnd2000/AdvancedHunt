package de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.global;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.individual.IndividualEggRewardsMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteItemNBT;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;

public class GlobalEggRewardsMenu extends PaginatedInventoryMenu {
    private MessageManager messageManager;
    private Main plugin;
    private String id;
    private String collection;

    public GlobalEggRewardsMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Global Egg Rewards", (short) 54, XMaterial.WHITE_STAINED_GLASS_PANE);
        this.plugin = Main.getInstance();
        messageManager = this.plugin.getMessageManager();

        super.addMenuBorder();
        addMenuBorderButtons();
    }

    public void open(String id, String collection) {
        this.id = id;
        this.collection = collection;

        menuContent(collection);

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    private void addMenuBorderButtons() {
        inventoryContent[45] = new ItemBuilder(XMaterial.EMERALD_BLOCK).setDisplayname("§5Save preset").setLore("", "§7Saves the current listed commands", "§7in a preset that you can load", "§7for other eggs again.", "", "§2Note: §7You need at least 1 command to save a preset!", "", "§eClick to save a new preset.").build();
        inventoryContent[46] = new ItemBuilder(XMaterial.EMERALD).setDisplayname("§5Load presets").setLore("§eClick to load or change presets.").build();
        inventoryContent[53] = new ItemBuilder(XMaterial.GOLD_INGOT).setDisplayname("§5Create new reward").setLore("", "§bYou can also add custom items:", "§7For that get your custom item in your", "§7inventory and click it when this", "§7menu is open. The item will", "§7get converted into an command", "§7and can then used as the other commands.", "", "§eClick to create a new reward").build();
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§cClose").build();
        inventoryContent[8] = new ItemBuilder(XMaterial.PLAYER_HEAD).setDisplayname("§bSwitch to Individual").setOwner(playerMenuUtility.getOwner().getName()).setLore("","§6Switch to Individual:","§7Switching to individual lets you manage","§7all commands and preset for","§7the funktion if a player has found","§7§lone §7egg.","","§eClick to switch").build();
        inventoryContent[7] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setSkullOwner(Main.getTexture("MTY0MzlkMmUzMDZiMjI1NTE2YWE5YTZkMDA3YTdlNzVlZGQyZDUwMTVkMTEzYjQyZjQ0YmU2MmE1MTdlNTc0ZiJ9fX0="))
                .setDisplayname("§9Information")
                .setLore("§7The global egg rewards count",
                        "§7as §7§lall egg reward§7.",
                        "§7This means if the player found §7§lall eggs§7,",
                        "§7all commands that are in this inventory",
                        "§7will get executed.",
                        "",
                        "§c§n§oThis commands will count for the entire collection.",
                        "",
                        "§7You can save all listed commands as present",
                        "§7to load it in other collections or setting it as default.").build();
    }

    private void menuContent(String collection) {
        getInventory().setContents(inventoryContent);

        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Left")
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.").setDisplayname("§2Right")
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).build());

        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("GlobalRewards.")){
            keys.addAll(placedEggs.getConfigurationSection("GlobalRewards.").getKeys(false));
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Rewards").setLore("§7Create new a new reward", "§7or load a preset.").build());
        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String command = placedEggs.getString("GlobalRewards." + keys.get(index) + ".command").replaceAll("§", "&");
                    boolean enabled = placedEggs.getBoolean("GlobalRewards." + keys.get(index) + ".enabled");
                    boolean startsWithGive = command.toLowerCase().startsWith("give") || command.toLowerCase().startsWith("minecraft:give");
                    ItemStack itemStack = XMaterial.PAPER.parseItem();
                    if (startsWithGive) {
                        String[] parts = command.split(" ", 3);

                        if (parts.length >= 2 && (parts[0].equalsIgnoreCase("minecraft:give") || parts[0].equalsIgnoreCase("give"))) {
                            String materialName = parts[2];

                            itemStack = getItem(materialName);
                        }
                    }
                    getInventory().addItem(new ItemBuilder(itemStack).setDisplayname("§b§lReward §7#" + keys.get(index)).setLore("", "§9Information:", "§7Command: §6" + command, "§7Command Enabled: " + (enabled ? "§atrue" : "§cfalse"), "", "§eLEFT-CLICK to toggle enabled.", "§eRIGHT-CLICK to delete.").setLocalizedName(keys.get(index)).build());
                }
            }
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Rewards").setLore("§7Create new a new reward", "§7or load a preset.").build());
    }

    public ItemStack getItem(String itemString) {
        int metaDataStartIndex = itemString.indexOf('{');
        int metaDataEndIndex = itemString.lastIndexOf('}');
        if (metaDataEndIndex == -1) metaDataEndIndex = itemString.length() - 1;
        else metaDataEndIndex += 1;
        ItemStack itemStack;

        Optional<XMaterial> material;
        if (metaDataStartIndex == -1){
            material = XMaterial.matchXMaterial(itemString);
            if (material.isEmpty()) return XMaterial.PAPER.parseItem();
            return material.get().parseItem();
        }

        material = XMaterial.matchXMaterial(itemString.substring(0, metaDataStartIndex));

        if (material.isEmpty()) return XMaterial.PAPER.parseItem();

        var json = itemString.substring(metaDataStartIndex, metaDataEndIndex);
        var item = material.get().parseItem();
        NBT.modify(item, (Consumer<ReadWriteItemNBT>) nbt -> nbt.mergeCompound(NBT.parseNBT(json)));

        return item;
    }

    public int getMaxPages(){
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("GlobalRewards.")){
            keys.addAll(placedEggs.getConfigurationSection("GlobalRewards.").getKeys(false));
        }
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / maxItemsPerPage);
    }

    public void convertItemIntoCommand(ItemStack itemStack, String collection){
        String itemNBT = NBT.get(itemStack, Object::toString);
        addCommand(MessageFormat.format("minecraft:give %PLAYER% {0}{1} {2}", itemStack.getType().name().toLowerCase(), itemNBT, itemStack.getAmount()), collection,"GlobalRewards.");
    }

    private void addCommand(String command, String collection, String path){
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        ConfigurationSection rewardsSection = placedEggs.getConfigurationSection("GlobalRewards.");
        int nextNumber = 0;
        if (rewardsSection != null) {
            Set<String> keys = rewardsSection.getKeys(false);
            for (int i = 0; i <= keys.size(); i++) {
                if (!keys.contains(Integer.toString(i))) {
                    nextNumber = i;
                    break;
                }
            }
        }
        plugin.getEggDataManager().setRewards(String.valueOf(nextNumber), command, collection, path);
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);

        if(event.getClickedInventory().equals(player.getInventory())){
            convertItemIntoCommand(event.getCurrentItem(), collection);
            player.sendMessage("§aSuccessfully added a new item.");
            menuContent(collection);
        }

        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("GlobalRewards.")){
            keys.addAll(placedEggs.getConfigurationSection("GlobalRewards.").getKeys(false));
            for(String commandID : keys){
                if (!Objects.requireNonNull(event.getCurrentItem().getItemMeta()).getLocalizedName().equals(commandID)) {
                    continue;
                }
                switch (event.getAction()) {
                    case PICKUP_ALL:
                        placedEggs.set("GlobalRewards." + commandID + ".enabled", !placedEggs.getBoolean("GlobalRewards." + commandID + ".enabled"));
                        plugin.getEggDataManager().savePlacedEggs(collection, placedEggs);
                        break;
                    case PICKUP_HALF:
                        player.sendMessage(messageManager.getMessage(MessageKey.COMMAND_DELETE).replaceAll("%ID%", commandID));
                        placedEggs.set("GlobalRewards." + commandID, null);
                        plugin.getEggDataManager().savePlacedEggs(collection, placedEggs);
                        break;
                }
                open(id, collection);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                return;
            }
        }

        XMaterial material = XMaterial.matchXMaterial(event.getCurrentItem());
        switch (material) {
            case BARRIER:
                player.closeInventory();
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case GOLD_INGOT:
                player.closeInventory();
                Main.getInstance().getPlayerAddCommand().put(player, 120);
                TextComponent textComponent = new TextComponent("\n\n\n\n\n" + Main.getInstance().getMessageManager().getMessage(MessageKey.NEW_COMMAND) + "\n\n");
                TextComponent clickme = new TextComponent("§9-----------§3§l[PLACEHOLDERS] §7(Hover)§9-----------");
                clickme.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§2Available placeholders:\n§b- %PLAYER% --> Name of the player\n§b- & --> For color codes (&6=gold)\n§b- %EGGS_FOUND% --> How many eggs the player has found\n§b- %EGGS_MAX% --> How many eggs are placed\n§b- %PREFIX% --> The prefix of the plugin")));
                textComponent.addExtra(clickme);
                player.spigot().sendMessage(textComponent);
                FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(player.getUniqueId());
                playerConfig.set("GlobalChange.collection", collection);
                playerConfig.set("GlobalChange.id", id);
                Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case EMERALD_BLOCK:
                if (placedEggs.getConfigurationSection("GlobalRewards.").getKeys(false).size() < 1) {
                    player.sendMessage(messageManager.getMessage(MessageKey.PRESET_FAILED_COMMANDS));
                    break;
                }
                new AnvilGUI.Builder()
                        .onClose(stateSnapshot -> {
                            if (!stateSnapshot.getText().isEmpty()) {
                                GlobalPresetDataManager presetDataManager = plugin.getGlobalPresetDataManager();
                                String preset = stateSnapshot.getText();
                                if (!presetDataManager.containsPreset(preset)) {
                                    presetDataManager.createPresetFile(stateSnapshot.getText());
                                    presetDataManager.loadCommandsIntoPreset(preset, collection);
                                    presetDataManager.addDefaultRewardCommands(preset);
                                    menuContent(collection);
                                    open(id,collection);
                                    player.sendMessage(messageManager.getMessage(MessageKey.PRESET_SAVED).replaceAll("%PRESET%", preset));
                                } else {
                                    player.sendMessage(messageManager.getMessage(MessageKey.PRESET_ALREADY_EXISTS).replaceAll("%PRESET%", preset));
                                }
                            }
                        })
                        .onClick((slot, stateSnapshot) -> {
                            return Collections.singletonList(AnvilGUI.ResponseAction.close());
                        })
                        .text("enter name")
                        .title("Preset name")
                        .plugin(Main.getInstance())
                        .open(player);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case EMERALD:
                new GlobalPresetsMenu(super.playerMenuUtility).open(id, collection);
                break;
            case PLAYER_HEAD:
                if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")) {
                    if (page == 0) {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    } else {
                        page = page - 1;
                        menuContent(collection);
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= keys.size())) {
                        page = page + 1;
                        menuContent(collection);
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    } else {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Switch to Individual")) {
                    new IndividualEggRewardsMenu(super.playerMenuUtility).open(id,collection);
                }
                break;
        }
    }
}
