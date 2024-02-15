package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.DateTimeUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.enums.Requirements;
import de.theredend2000.advancedegghunt.util.enums.Seasons;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class InventoryRequirementsManager {

    public void createSelectInventory(Player player, String section) {
        Inventory inventory = Bukkit.createInventory(player,54,"Requirements - Selection");
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        inventory.setItem(4,new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7))).setDisplayname("§6"+section).build());
        inventory.setItem(10, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Hours").setLore("§7Active: §b"+Main.getInstance().getRequirementsManager().getActives(Requirements.Hours,section),"","§eClick to open.").build());
        inventory.setItem(11, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Date").setLore("§7Active: §b"+Main.getInstance().getRequirementsManager().getActives(Requirements.Date,section),"","§eClick to open.").build());
        inventory.setItem(12, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Weekday").setLore("§7Active: §b"+Main.getInstance().getRequirementsManager().getActives(Requirements.Weekday,section),"","§eClick to open.").build());
        inventory.setItem(13, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Month").setLore("§7Active: §b"+Main.getInstance().getRequirementsManager().getActives(Requirements.Month,section),"","§eClick to open.").build());
        inventory.setItem(14, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Year").setLore("§7Active: §b"+Main.getInstance().getRequirementsManager().getActives(Requirements.Year,section),"","§eClick to open.").build());
        inventory.setItem(15, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Season").setLore("§7Active: §b"+Main.getInstance().getRequirementsManager().getActives(Requirements.Season,section),"","§eClick to open.").build());
        inventory.setItem(37, new ItemBuilder(XMaterial.LIME_TERRACOTTA).setDisplayname("§aActivate all").setLore("§eClick to activate all.").build());
        inventory.setItem(38, new ItemBuilder(XMaterial.RED_TERRACOTTA).setDisplayname("§cDeactivate all").setLore("§eClick to deactivate all.").build());
        String currentOrder = placedEggs.getString("RequirementsOrder");
        inventory.setItem(43, new ItemBuilder(XMaterial.REDSTONE_TORCH).setDisplayname("§bRequirements Order").setLore("","§7Current order: §6"+currentOrder,"","§a§lINFO:","§6OR","§7Makes that only one think musst be right","§7in the selection.","§8Example: Weekday Monday and Hour 7 is enable.","§8Now it must be Monday §8§lOR §8it must be 7.","§6AND","§7Makes that in all selections must be","§7at least one right.","§8Example: Weekday Monday and Friday is enabled","§8and hour 7 and 10.","§8Now it must be Monday or Friday §8§land","§8it must be 7 or 10.","","§eClick to change.").build());
        inventory.setItem(45, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build());
        player.openInventory(inventory);
    }

    public void createHourInventory(Player player, String section) {
        Inventory inventory = Bukkit.createInventory(player,54,"Requirements - Hours");
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        inventory.setItem(4,new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7))).setDisplayname("§6"+section).build());
        for(int i = 0; i < 24; i++){
            boolean enabled = placedEggs.getBoolean("Requirements.Hours."+i);
            inventory.addItem(new ItemBuilder(enabled ? XMaterial.CLOCK : XMaterial.RED_STAINED_GLASS).setDisplayname("§6Hour "+i).setLore("§8Hours: ("+i+":00-"+(i+1)+":00)","§7Makes that the eggs are only","§7available in the hour that starts with "+i,"","§7Currently: "+(enabled ? "§aEnabled" : "§cDisabled"),"","§eClick to add hour "+i+" to the requirements.").withGlow(enabled).build());
        }
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build());
        inventory.setItem(45, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
        player.openInventory(inventory);
    }

    public void createWeekdayInventory(Player player, String section) {
        Inventory inventory = Bukkit.createInventory(player,54,"Requirements - Weekday");
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        inventory.setItem(4,new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7))).setDisplayname("§6"+section).build());
        for(String weekdays : new ArrayList<>(DateTimeUtil.getWeekList())){
            boolean enabled = placedEggs.getBoolean("Requirements.Weekday."+weekdays);
            inventory.addItem(new ItemBuilder(enabled ? XMaterial.LIME_BED : XMaterial.RED_STAINED_GLASS).setDisplayname("§6"+weekdays).setLore("§7Makes that the eggs are only","§7available on the weekday "+weekdays,"","§7Currently: "+(enabled ? "§aEnabled" : "§cDisabled"),"","§eClick to add "+weekdays+" to the requirements.").withGlow(enabled).build());
        }
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build());
        inventory.setItem(45, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
        player.openInventory(inventory);
    }

    public void createMonthInventory(Player player, String section) {
        Inventory inventory = Bukkit.createInventory(player,54,"Requirements - Month");
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        inventory.setItem(4,new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7))).setDisplayname("§6"+section).build());
        for(String month : new ArrayList<>(DateTimeUtil.getMonthList())){
            boolean enabled = placedEggs.getBoolean("Requirements.Month."+month);
            inventory.addItem(new ItemBuilder(enabled ? XMaterial.GRASS_BLOCK : XMaterial.RED_STAINED_GLASS).setDisplayname("§6"+month).setLore("§7Makes that the eggs are only","§7available in the month "+month,"","§7Currently: "+(enabled ? "§aEnabled" : "§cDisabled"),"","§eClick to add "+month+" to the requirements.").withGlow(enabled).build());
        }
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build());
        inventory.setItem(45, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
        player.openInventory(inventory);
    }

    public void createYearInventory(Player player, String section) {
        Inventory inventory = Bukkit.createInventory(player,54,"Requirements - Year");
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        inventory.setItem(4,new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7))).setDisplayname("§6"+section).build());
        int currentYear = DateTimeUtil.getCurrentYear();
        for(int year = currentYear; year < (currentYear+28);year++){
            boolean enabled = placedEggs.getBoolean("Requirements.Year."+year);
            inventory.addItem(new ItemBuilder(enabled ? XMaterial.BEACON : XMaterial.RED_STAINED_GLASS).setDisplayname("§6Year "+year).setLore("§7Makes that the eggs are only","§7available in the year "+year,"","§7Currently: "+(enabled ? "§aEnabled" : "§cDisabled"),"","§eClick to add "+year+" to the requirements.").withGlow(enabled).build());
        }
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build());
        inventory.setItem(45, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
        player.openInventory(inventory);
    }

    public void createSeasonInventory(Player player, String section) {
        Inventory inventory = Bukkit.createInventory(player,54,"Requirements - Season");
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        inventory.setItem(4,new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7))).setDisplayname("§6"+section).build());
        for(String season : new ArrayList<>(DateTimeUtil.getSeasonList())){
            boolean enabled = placedEggs.getBoolean("Requirements.Season."+season);
            inventory.addItem(new ItemBuilder(enabled ? XMaterial.OAK_LEAVES : XMaterial.RED_STAINED_GLASS).setDisplayname("§6"+season).setLore(getSeasonInformation(Seasons.valueOf(season)),"§7Makes that the eggs are only","§7available in the season "+season,"","§7Currently: "+(enabled ? "§aEnabled" : "§cDisabled"),"","§eClick to add "+season+" to the requirements.").withGlow(enabled).build());
        }
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build());
        inventory.setItem(45, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
        player.openInventory(inventory);
    }

    private String getSeasonInformation(Seasons seasons){
        switch (seasons){
            case Winter:
                return "§8December | January | February";
            case Summer:
                return "§8June | July | August";
            case Spring:
                return "§8March | April | May";
            case Fall:
                return "§8September | October | November";
            case Unknown:
                return "§4UNKNOWN";
        }
        return null;
    }
}
