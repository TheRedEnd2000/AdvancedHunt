package de.theredend2000.advancedegghunt.managers.inventorymanager.requirements;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.collection.CollectionEditor;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.enums.Requirements;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Random;

public class RequirementSelection extends InventoryMenu {
    private MessageManager messageManager;
    protected int maxItems;
    private Main plugin;

    public RequirementSelection(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Requirements - Selection", (short) 54, XMaterial.WHITE_STAINED_GLASS_PANE);
        this.plugin = Main.getInstance();
        messageManager = this.plugin.getMessageManager();
        this.maxItems = 7 * ((this.slots / 9) - 2);
    }

    public void open(String collection) {
        super.addMenuBorder();
        addMenuBorderButtons(collection);
        menuContent(collection);

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    private void addMenuBorderButtons(String collection) {
        inventoryContent[4] = new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7))).setDisplayname("§6" + collection).build();

        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD).setDisplayname("§eBack").setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).build();
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build();
    }

    private void menuContent(String collection) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);

        getInventory().setItem(10, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Hours").setLore("§7Active: §b" + Main.getInstance().getRequirementsManager().getActives(Requirements.Hours, collection), "", "§eClick to open.").build());
        getInventory().setItem(11, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Date").setLore("§7Active: §b" + Main.getInstance().getRequirementsManager().getActives(Requirements.Date, collection), "", "§eClick to open.").build());
        getInventory().setItem(12, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Weekday").setLore("§7Active: §b" + Main.getInstance().getRequirementsManager().getActives(Requirements.Weekday, collection), "", "§eClick to open.").build());
        getInventory().setItem(13, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Month").setLore("§7Active: §b" + Main.getInstance().getRequirementsManager().getActives(Requirements.Month, collection), "", "§eClick to open.").build());
        getInventory().setItem(14, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Year").setLore("§7Active: §b" + Main.getInstance().getRequirementsManager().getActives(Requirements.Year, collection), "", "§eClick to open.").build());
        getInventory().setItem(15, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§6Selection - Season").setLore("§7Active: §b" + Main.getInstance().getRequirementsManager().getActives(Requirements.Season, collection), "", "§eClick to open.").build());
        getInventory().setItem(37, new ItemBuilder(XMaterial.LIME_TERRACOTTA).setDisplayname("§aActivate all").setLore("§eClick to activate all.").build());
        getInventory().setItem(38, new ItemBuilder(XMaterial.RED_TERRACOTTA).setDisplayname("§cDeactivate all").setLore("§eClick to deactivate all.").build());
        String currentOrder = placedEggs.getString("RequirementsOrder");
        getInventory().setItem(43, new ItemBuilder(XMaterial.REDSTONE_TORCH).setDisplayname("§bRequirements Order").setLore("", "§7Current order: §6" + currentOrder, "", "§a§lINFO:", "§6OR", "§7Makes that only one think musst be right", "§7in the selection.", "§8Example: Weekday Monday and Hour 7 is enable.", "§8Now it must be Monday §8§lOR §8it must be 7.", "§6AND", "§7Makes that in all selections must be", "§7at least one right.", "§8Example: Weekday Monday and Friday is enabled", "§8and hour 7 and 10.", "§8Now it must be Monday or Friday §8§land", "§8it must be 7 or 10.", "", "§eClick to change.").build());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player  = (Player) event.getWhoClicked();
        SoundManager soundManager = plugin.getSoundManager();

        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        switch (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName())) {
            case "Close":
                player.closeInventory();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "Back":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new CollectionEditor(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "Selection - Hours":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementHours(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "Selection - Date":
                player.sendMessage("§cThis requirement section is currently unavailable.");
                break;
            case "Selection - Weekday":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementWeekday(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "Selection - Month":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementMonth(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "Selection - Year":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementYear(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "Selection - Season":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementSeason(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "Activate all":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                plugin.getRequirementsManager().changeActivity(collection, true);
                player.sendMessage(messageManager.getMessage(MessageKey.ACTIVATE_REQUIREMENTS));
                menuContent(collection);
                break;
            case "Deactivate all":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                plugin.getRequirementsManager().changeActivity(collection, false);
                player.sendMessage(messageManager.getMessage(MessageKey.DEACTIVATE_REQUIREMENTS));
                menuContent(collection);
                break;
            case "Requirements Order":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                player.sendMessage("§cThis feature is coming soon.");
                break;
        }
    }

    @Override
    public String getMenuName() {
        return null;
    }

    @Override
    public int getSlots() {
        return this.slots;
    }
}
