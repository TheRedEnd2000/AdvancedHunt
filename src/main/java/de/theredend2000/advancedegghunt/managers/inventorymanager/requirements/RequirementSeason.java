package de.theredend2000.advancedegghunt.managers.inventorymanager.requirements;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedegghunt.util.DateTimeUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.enums.Seasons;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Random;

public class RequirementSeason extends InventoryMenu {
    private MessageManager messageManager;
    protected int maxItems;
    private Main plugin;

    public RequirementSeason(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Requirements - Season", (short) 54, XMaterial.WHITE_STAINED_GLASS_PANE);
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
        inventoryContent[4]  = new ItemBuilder(XMaterial.PLAYER_HEAD).setDisplayname("§6" + collection).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7))).build();
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD).setDisplayname("§eBack").setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).build();
    }

    private void menuContent(String collection) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        getInventory().setContents(inventoryContent);
        for(String season : new ArrayList<>(DateTimeUtil.getSeasonList())){
            boolean enabled = placedEggs.getBoolean("Requirements.Season." + season);
            getInventory().addItem(new ItemBuilder(enabled ? XMaterial.OAK_LEAVES : XMaterial.RED_STAINED_GLASS).setDisplayname("§6" + season).setLore(getSeasonInformation(Seasons.valueOf(season)), "§7Makes that the eggs are only", "§7available in the season " + season, "", "§7Currently: " + (enabled ? "§aEnabled" : "§cDisabled"), "", "§eClick to add " + season + " to the requirements.").withGlow(enabled).build());
        }
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

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player  = (Player) event.getWhoClicked();

        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        for (String season : new ArrayList<>(DateTimeUtil.getSeasonList())) {
            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals(season)) {
                boolean enabled = placedEggs.getBoolean("Requirements.Season." + season);
                placedEggs.set("Requirements.Season." + season, !enabled);
                plugin.getEggDataManager().savePlacedEggs(collection, placedEggs);
                menuContent(collection);
            }
        }
        if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
            player.closeInventory();
        if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
            new RequirementSelection(Main.getPlayerMenuUtility(player)).open(collection);
    }
}
