package de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.global;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.individual.IndividualEggRewardsMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.ItemHelper;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteItemNBT;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
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
        inventoryContent[45] = new ItemBuilder(XMaterial.EMERALD_BLOCK)
                .setDisplayName("§5Save preset")
                .setLore("", "§7Saves the current listed commands", "§7in a preset that you can load", "§7for other collections again.", "", "§2Note: §7You need at least 1 command to save a preset!", "", "§eClick to save a new preset.")
                .build();
        inventoryContent[46] = new ItemBuilder(XMaterial.EMERALD)
                .setDisplayName("§5Load presets")
                .setLore("§eClick to load or change presets.")
                .build();
        inventoryContent[53] = new ItemBuilder(XMaterial.GOLD_INGOT)
                .setDisplayName("§5Create new reward")
                .setLore("", "§bYou can also add custom items:", "§7For that get your custom item in your", "§7inventory and click it when this", "§7menu is open. The item will", "§7get converted into an command", "§7and can then used as the other commands.", "", "§eClick to create a new reward")
                .build();
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setDisplayName("§cClose")
                .build();
        inventoryContent[8] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setDisplayName("§bSwitch to Individual").setOwner(playerMenuUtility.getOwner().getName())
                .setLore("","§6Switch to Individual:","§7Switching to individual lets you manage","§7all commands and preset for","§7the funktion if a player has found","§7§lone §7egg.","","§eClick to switch")
                .build();
        inventoryContent[7] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setSkullOwner(Main.getTexture("MTY0MzlkMmUzMDZiMjI1NTE2YWE5YTZkMDA3YTdlNzVlZGQyZDUwMTVkMTEzYjQyZjQ0YmU2MmE1MTdlNTc0ZiJ9fX0="))
                .setDisplayName("§9Information")
                .setLore("§7The global egg rewards count",
                        "§7as §7§lall egg reward§7.",
                        "§7This means if the player found §7§lall eggs§7,",
                        "§7all commands that are in this inventory",
                        "§7will get executed.",
                        "",
                        "§c§n§oThese commands will count for the entire collection.",
                        "",
                        "§7You can save all listed commands as present",
                        "§7to load it in other collections or setting it as default.")
                .build();
    }

    private void menuContent(String collection) {
        getInventory().setContents(inventoryContent);

        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.")
                .setDisplayName(menuMessageManager.getMenuMessage(MenuMessageKey.PREVIOUS_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19"))
                .build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore("§6Page: §7(§b" + (page + 1) + "§7/§b" + getMaxPages() + "§7)", "", "§eClick to scroll.")
                .setDisplayName(menuMessageManager.getMenuMessage(MenuMessageKey.NEXT_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ=="))
                .build());

        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("GlobalRewards.")){
            keys.addAll(placedEggs.getConfigurationSection("GlobalRewards.").getKeys(false));
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                    .setDisplayName("§4§lNo Rewards")
                    .setLore("§7Create new a new reward", "§7or load a preset.")
                    .build());
        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String command = placedEggs.getString("GlobalRewards." + keys.get(index) + ".command").replaceAll("§", "&");
                    boolean enabled = placedEggs.getBoolean("GlobalRewards." + keys.get(index) + ".enabled");
                    boolean startsWithGive = command.toLowerCase().startsWith("give") || command.toLowerCase().startsWith("minecraft:give");
                    double chance = placedEggs.getDouble("GlobalRewards." + keys.get(index) + ".chance");
                    String rarity = plugin.getRarityManager().getRarity(chance);
                    ItemStack itemStack = XMaterial.PAPER.parseItem();
                    if (startsWithGive) {
                        String[] parts = command.split(" ", 3);

                        if (parts.length >= 2 && (parts[0].equalsIgnoreCase("minecraft:give") || parts[0].equalsIgnoreCase("give"))) {
                            String materialName = parts[2];

                            itemStack = getItem(materialName);
                        }
                    }
                    getInventory().addItem(new ItemBuilder(itemStack)
                            .setDisplayName("§b§lReward §7#" + keys.get(index))
                            .setLore("", "§9Information:", "§7Command: §6" + command, "§7Command Enabled: " + (enabled ? "§atrue" : "§cfalse"),"§7Chance: §6"+new DecimalFormat("0.##############").format(chance) +"% §7("+plugin.getExtraManager().decimalToFraction(chance/100)+")","§7Rarity: "+rarity, "", "§eLEFT-CLICK to toggle enabled.", "§eMIDDLE-CLICK to change chance.", "§eRIGHT-CLICK to delete.", "§eDROP to execute command.")
                            .setCustomId(keys.get(index))
                            .build());
                }
            }
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                    .setDisplayName("§4§lNo Rewards")
                    .setLore("§7Create new a new reward", "§7or load a preset.")
                    .build());
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
            messageManager.sendMessage(player, MessageKey.ITEM_ADDED_SUCCESS);
            menuContent(collection);
            return;
        }

        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("GlobalRewards.")){
            keys.addAll(placedEggs.getConfigurationSection("GlobalRewards.").getKeys(false));
            for(String commandID : keys){
                if (!ItemHelper.hasItemId(event.getCurrentItem()) ||
                        !ItemHelper.getItemId(event.getCurrentItem()).equals(commandID)) {
                    continue;
                }
                switch (event.getAction()) {
                    case PICKUP_ALL:
                        placedEggs.set("GlobalRewards." + commandID + ".enabled", !placedEggs.getBoolean("GlobalRewards." + commandID + ".enabled"));
                        plugin.getEggDataManager().savePlacedEggs(collection, placedEggs);
                        open(id, collection);
                        break;
                    case CLONE_STACK:
                        new AnvilGUI.Builder()
                                .onClose(stateSnapshot -> {
                                    if (stateSnapshot.getText().isEmpty()) {
                                        return;
                                    }
                                    if(stateSnapshot.getText().matches("[0-9.]+")){
                                        if(!(Double.parseDouble(stateSnapshot.getText()) < 0.0000001 || Double.parseDouble(stateSnapshot.getText()) > 100)) {
                                            placedEggs.set("GlobalRewards." + commandID + ".chance", Double.valueOf(stateSnapshot.getText()));
                                            plugin.getEggDataManager().savePlacedEggs(collection, placedEggs);
                                            messageManager.sendMessage(player, MessageKey.CHANCED_CHANCE, "%CHANCE%", stateSnapshot.getText());
                                        }else
                                            messageManager.sendMessage(player, MessageKey.INVALID_CHANCE);
                                    }else
                                        messageManager.sendMessage(player, MessageKey.NOT_NUMBER);
                                    open(id,collection);
                                })
                                .onClick((slot, stateSnapshot) -> Collections.singletonList(AnvilGUI.ResponseAction.close()))
                                .text(String.valueOf(placedEggs.getDouble("GlobalRewards." + commandID + ".chance")))
                                .title("Change chance")
                                .plugin(Main.getInstance())
                                .open(player);
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                        break;
                    case PICKUP_HALF:
                        messageManager.sendMessage(player, MessageKey.COMMAND_DELETE, "%ID%", commandID);
                        placedEggs.set("GlobalRewards." + commandID, null);
                        plugin.getEggDataManager().savePlacedEggs(collection, placedEggs);
                        open(id, collection);
                        break;
                    case DROP_ONE_SLOT:
                        EggManager eggManager = plugin.getEggManager();
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Objects.requireNonNull(placedEggs.getString("GlobalRewards." + commandID + ".command")).replaceAll("%PLAYER%", player.getName()).replaceAll("&", "§").replaceAll("%EGGS_FOUND%", String.valueOf(eggManager.getEggsFound(player, collection))).replaceAll("%EGGS_MAX%", String.valueOf(eggManager.getMaxEggs(collection))).replaceAll("%PREFIX%", Main.PREFIX));
                        break;
                }
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
                TextComponent textComponent = new TextComponent("\n\n\n\n\n" + messageManager.getMessage(MessageKey.NEW_COMMAND) + "\n\n");
                TextComponent clickme = new TextComponent(messageManager.getMessage(MessageKey.PLACEHOLDERS_HOVER_TEXT));
                clickme.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(messageManager.getMessage(MessageKey.PLACEHOLDERS_HOVER_CONTENT))));
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
                    messageManager.sendMessage(player, MessageKey.PRESET_FAILED_COMMANDS);
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
                                    messageManager.sendMessage(player, MessageKey.PRESET_SAVED, "%PRESET%", preset);
                                } else {
                                    messageManager.sendMessage(player, MessageKey.PRESET_ALREADY_EXISTS, "%PRESET%", preset);
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
                        messageManager.sendMessage(player, MessageKey.FIRST_PAGE);
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
                        messageManager.sendMessage(player, MessageKey.LAST_PAGE);
                        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Switch to Individual")) {
                    new IndividualEggRewardsMenu(super.playerMenuUtility).open(id,collection);
                }
                break;
        }
    }
}
