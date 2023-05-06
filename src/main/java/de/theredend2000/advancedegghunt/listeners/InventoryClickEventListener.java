package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.InventoryManager_1_19_R1;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.eggfoundrewardmenu.EggRewardMenu;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.eggfoundrewardmenu.RewardMenu;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.egginformation.InformationMenu;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.eggprogress.ProgressMenu;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.paginatedMenu.ListMenu;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;

public class InventoryClickEventListener implements Listener {

    public InventoryClickEventListener(){
        Bukkit.getPluginManager().registerEvents(this,Main.getInstance());
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event){
        if(event.getWhoClicked() instanceof Player){
            Player player = (Player) event.getWhoClicked();
            if(event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null){
                if(Main.getInstance().getPlaceEggsPlayers().contains(player)){
                    if(event.getInventory().getViewers().contains(player)){
                        if(event.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) event.setCancelled(true);
                        event.setCancelled(true);
                    }
                }
                InventoryHolder holder = event.getInventory().getHolder();
                if (holder instanceof ListMenu) {
                    event.setCancelled(true);
                    if(event.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) event.setCancelled(true);
                    if (event.getCurrentItem() == null) {
                        return;
                    }
                    ListMenu menu = (ListMenu) holder;
                    menu.handleMenu(event);
                }
                if (holder instanceof ProgressMenu) {
                    if(event.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) event.setCancelled(true);
                    event.setCancelled(true);
                    if (event.getCurrentItem() == null) {
                        return;
                    }
                    ProgressMenu menu = (ProgressMenu) holder;
                    menu.handleMenu(event);
                }
                if (holder instanceof InformationMenu) {
                    if(event.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) event.setCancelled(true);
                    event.setCancelled(true);
                    if (event.getCurrentItem() == null) {
                        return;
                    }
                    InformationMenu menu = (InformationMenu) holder;
                    menu.handleMenu(event);
                }
                if (holder instanceof RewardMenu) {
                    if(event.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) event.setCancelled(true);
                    event.setCancelled(true);
                    if (event.getCurrentItem() == null) {
                        return;
                    }
                    RewardMenu menu = (RewardMenu) holder;
                    menu.handleMenu(event);
                }
                if(event.getView().getTitle().equals("Advanced Egg Settings")){
                    if(event.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) event.setCancelled(true);
                    event.setCancelled(true);
                    if(event.getCurrentItem().getItemMeta().hasLocalizedName()){
                        switch (event.getCurrentItem().getItemMeta().getLocalizedName()){
                            case "settings.close":
                                player.closeInventory();
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.foundoneegg":
                                Main.getInstance().getConfig().set("Settings.PlayerFoundOneEggRewards",!Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundOneEggRewards"));
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.foundalleggs":
                                Main.getInstance().getConfig().set("Settings.PlayerFoundAllEggsReward",!Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundAllEggsReward"));
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.updater":
                                Main.getInstance().getConfig().set("Settings.Updater",!Main.getInstance().getConfig().getBoolean("Settings.Updater"));
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.commandfeedback":
                                Main.getInstance().getConfig().set("Settings.DisableCommandFeedback",!Main.getInstance().getConfig().getBoolean("Settings.DisableCommandFeedback"));
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.soundvolume":
                                int currentVolume = Main.getInstance().getConfig().getInt("Settings.SoundVolume");
                                if(event.getAction() == InventoryAction.PICKUP_ALL){
                                    if(currentVolume == 15) {
                                        player.sendMessage(Main.getInstance().getMessage("SoundCanOnlyBetween"));
                                        return;
                                    }
                                    Main.getInstance().getConfig().set("Settings.SoundVolume", currentVolume + 1);

                                }else if(event.getAction() == InventoryAction.PICKUP_HALF){
                                    if(currentVolume == 0) {
                                        player.sendMessage(Main.getInstance().getMessage("SoundCanOnlyBetween"));
                                        return;
                                    }
                                    Main.getInstance().getConfig().set("Settings.SoundVolume",currentVolume-1);
                                }
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.armorstandglow":
                                int currentTime = Main.getInstance().getConfig().getInt("Settings.ArmorstandGlow");
                                if(event.getAction() == InventoryAction.PICKUP_ALL){
                                    if(currentTime == 120) {
                                        player.sendMessage(Main.getInstance().getMessage("ArmorStandGlowCanOnlyBetween"));
                                        return;
                                    }
                                    Main.getInstance().getConfig().set("Settings.ArmorstandGlow", currentTime + 1);

                                }else if(event.getAction() == InventoryAction.PICKUP_HALF){
                                    if(currentTime == 0) {
                                        player.sendMessage(Main.getInstance().getMessage("ArmorStandGlowCanOnlyBetween"));
                                        return;
                                    }
                                    Main.getInstance().getConfig().set("Settings.ArmorstandGlow", currentTime - 1);
                                }
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "settings.showcoordinates":
                                Main.getInstance().getConfig().set("Settings.ShowCoordinatesWhenEggFoundInProgressInventory",!Main.getInstance().getConfig().getBoolean("Settings.ShowCoordinatesWhenEggFoundInProgressInventory"));
                                Main.getInstance().saveConfig();
                                VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                        }
                    }
                }else if(event.getView().getTitle().equals("Command configuration")){
                    event.setCancelled(true);
                    if(event.getCurrentItem().getItemMeta().hasLocalizedName()){
                        String id = event.getInventory().getItem(22).getItemMeta().getLocalizedName();
                        switch (event.getCurrentItem().getItemMeta().getLocalizedName()){
                            case "command.close":
                                player.closeInventory();
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                break;
                            case "command.delete":
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                TextComponent c = new TextComponent(Main.getInstance().getMessage("CommandDeleteMessage").replaceAll("%ID%",id)+"\n");
                                TextComponent clickme = new TextComponent("§6§l[SHOW COMMAND INFORMATION] §7(Hover)");

                                String command = Main.getInstance().getConfig().getString("Rewards."+id+".command").replaceAll("§","&");
                                boolean enabled = Main.getInstance().getConfig().getBoolean("Rewards."+id+".enabled");
                                int type = Main.getInstance().getConfig().getInt("Rewards."+id+".type");
                                clickme.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§9Information:\n§7Command: §6"+command+"\n§7Command Enabled: "+(enabled ? "§atrue" : "§cfalse")+"\n§7Type: §6"+type+"\n\n§a§lNote:\n§2Type 0:\n§7Type 0 means that this command will be\n§7be executed if the player found §7§lone §7egg.\n§2Type 1:\n§7Type 1 means that this command will be\n§7be executed if the player had found §7§lall §7egg.")));
                                c.addExtra(clickme);
                                player.spigot().sendMessage(c);
                                Main.getInstance().getConfig().set("Rewards."+id,null);
                                Main.getInstance().saveConfig();
                                new EggRewardMenu(Main.getPlayerMenuUtility(player)).open();
                                break;
                            case "command.type":
                                int type2 = Main.getInstance().getConfig().getInt("Rewards."+id+".type");
                                if(type2 == 0){
                                    Main.getInstance().getConfig().set("Rewards."+id+".type",1);
                                }else if(type2 == 1)
                                    Main.getInstance().getConfig().set("Rewards."+id+".type",0);
                                Main.getInstance().saveConfig();
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                VersionManager.getInventoryManager().createCommandSettingsMenu(player,id);
                                player.sendMessage(Main.getInstance().getMessage("CommandTypeChangeMessage").replaceAll("%ID%",id).replaceAll("%TYPE%", String.valueOf((type2 == 1 ? 0 : 1))));
                                break;
                            case "command.enabled":
                                boolean enabled2 = Main.getInstance().getConfig().getBoolean("Rewards."+id+".enabled");
                                Main.getInstance().getConfig().set("Rewards."+id+".enabled",!enabled2);
                                Main.getInstance().saveConfig();
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                VersionManager.getInventoryManager().createCommandSettingsMenu(player,id);
                                player.sendMessage(Main.getInstance().getMessage("CommandEnabledChangeMessage").replaceAll("%ID%",id).replaceAll("%ENABLED_WITH_COLOR%", (!enabled2 ? "§aenabled" : "§cdisabled")));
                                break;
                            case "command.back":
                                player.playSound(player.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                                new EggRewardMenu(Main.getPlayerMenuUtility(player)).open();
                                break;
                            case "command.command":
                                if(Main.getInstance().getPlayerAddCommand().containsKey(player)){
                                    player.sendMessage(Main.getInstance().getMessage("OnlyOneCommandMessage"));
                                    return;
                                }
                                player.closeInventory();
                                Main.getInstance().getPlayerAddCommand().put(player,120);
                                TextComponent c2 = new TextComponent("\n\n\n\n\n"+Main.getInstance().getMessage("EnterNewCommandMessage")+"\n\n");
                                TextComponent clickme2 = new TextComponent("§9-----------§3§l[PLACEHOLDERS] §7(Hover)§9-----------");
                                clickme2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§2Available placeholders:\n§b- %PLAYER% --> Name of the player\n§b- & --> For color codes (&6=gold)\n§b- %EGGS_FOUND% --> How many eggs the player has found\n§b- %EGGS_MAX% --> How many eggs are placed\n§b- %PREFIX% --> The prefix of the plugin")));
                                c2.addExtra(clickme2);
                                player.spigot().sendMessage(c2);
                                Main.getInstance().eggs.set("Edit."+player.getUniqueId()+".commandID",id);
                                Main.getInstance().saveEggs();
                                break;
                        }
                    }
                }
            }
        }
    }

}
