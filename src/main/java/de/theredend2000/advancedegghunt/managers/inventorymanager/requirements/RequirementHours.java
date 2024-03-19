package de.theredend2000.advancedegghunt.managers.inventorymanager.requirements;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Random;

public class RequirementHours extends InventoryMenu {
    private MessageManager messageManager;
    protected int maxItems;
    private Main plugin;

    public RequirementHours(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Requirements - Hours", (short) 54, XMaterial.WHITE_STAINED_GLASS_PANE);
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
        for(int i = 0; i < 24; i++){
            int index = ((9 + 1) + ((i / 7) * 9) + (i % 7));
            boolean enabled = placedEggs.getBoolean("Requirements.Hours." + i);
            getInventory().setItem(index, new ItemBuilder(enabled ? XMaterial.CLOCK : XMaterial.RED_STAINED_GLASS).setDisplayname("§6Hour " + i).setLore("§8Hours: (" + i + ":00-" + (i + 1) + ":00)", "§7Makes that the eggs are only", "§7available in the hour that starts with " + i, "", "§7Currently: " + (enabled ? "§aEnabled" : "§cDisabled"), "", "§eClick to add hour " + i + " to the requirements.").withGlow(enabled).build());
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player  = (Player) event.getWhoClicked();

        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        for (int i = 0; i < 24; i++) {
            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Hour " + i)) {
                boolean enabled = placedEggs.getBoolean("Requirements.Hours." + i);
                placedEggs.set("Requirements.Hours." + i, !enabled);
                plugin.getEggDataManager().savePlacedEggs(collection, placedEggs);
                menuContent(collection);
            }
        }
        if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Close"))
            player.closeInventory();
        if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back"))
            new RequirementSelection(Main.getPlayerMenuUtility(player)).open(collection);
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
