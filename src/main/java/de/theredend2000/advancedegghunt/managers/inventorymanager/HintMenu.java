package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class HintMenu extends InventoryMenu {
    private boolean active;
    private int currentSlot;
    private int currentCount;
    private boolean clickedRight;
    private int lastClicked;
    private Random random;

    public HintMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Eggs Hint", (short) 54);
    }

    public void open(boolean active) {
        if (playerMenuUtility.getOwner() == null) {
            return;
        }

        this.active = active;
        this.clickedRight = true;
        this.currentCount = 0;
        this.lastClicked = -1;
        random = new Random();

        playerMenuUtility.getOwner().openInventory(getInventory());

        for (int i = 0; i < getInventory().getSize(); i++){getInventory().setItem(i, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        startAnimating();
        Main.getInstance().getCooldownManager().setCooldown(playerMenuUtility.getOwner());
    }

    private void startAnimating(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if(active){
                    if(!clickedRight) {
                        fail(playerMenuUtility.getOwner());
                        playerMenuUtility.getOwner().sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.CLICKED_SAME));
                        return;
                    }

                    getInventory().setItem(currentSlot, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());
                    currentSlot = getRandomSlot();
                    getInventory().setItem(currentSlot, new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE).setDisplayname("§aConfirm").setLore("§6" + (currentCount + 1) + "§7/§6" + Main.getInstance().getPluginConfig().getHintCount()).build());
                    clickedRight = false;
                }else{
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 40, Main.getInstance().getPluginConfig().getHintUpdateTime());
    }

    private int getRandomSlot(){
        int nextNum;
        do {
            nextNum = random.nextInt(getInventory().getSize());
        } while (nextNum == lastClicked);
        return nextNum;
    }

    public void fail(Player player){
        player.closeInventory();
        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playErrorSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
        active = false;
    }

    public String getReward(Player player){
        Main plugin = Main.getInstance();
        for(String collection : plugin.getEggDataManager().savedEggCollections()) {
            if (!Main.getInstance().getEggManager().containsPlayer(player.getName())) {
                continue;
            }
            if (Main.getInstance().getEggManager().checkFoundAll(player, collection)) continue;
            int number = Main.getInstance().getEggManager().getRandomNotFoundEgg(player, collection);
            ConfigLocationUtil location = new ConfigLocationUtil(plugin, "PlacedEggs." + number + ".");
            if (location.loadLocation(collection) != null) {
                int random = new Random().nextInt(2);
                return Main.getInstance().getMessageManager().getMessage(MessageKey.EGG_HINT).replaceAll("%X%", random == 1 ? String.valueOf(location.loadLocation(collection).getBlockX()) : "§k1").replaceAll("%Y%", String.valueOf(location.loadLocation(collection).getBlockY())).replaceAll("%Z%", random == 0 ? String.valueOf(location.loadLocation(collection).getBlockZ()) : "§k1");
            }
        }
        return null;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        SoundManager soundManager = Main.getInstance().getSoundManager();

        if (currentSlot == lastClicked) {
            fail(playerMenuUtility.getOwner());
            playerMenuUtility.getOwner().sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.CLICKED_SAME));
            return;
        }
        if (event.getSlot() == currentSlot) {
            currentCount++;
            playerMenuUtility.getOwner().playSound(playerMenuUtility.getOwner().getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
            if (currentCount == Main.getInstance().getPluginConfig().getHintCount()) {
                playerMenuUtility.getOwner().closeInventory();
                active = false;
                playerMenuUtility.getOwner().playSound(playerMenuUtility.getOwner().getLocation(), soundManager.playAllEggsFound(), soundManager.getSoundVolume(), 1);
                playerMenuUtility.getOwner().sendMessage(getReward(playerMenuUtility.getOwner()));
            }
            clickedRight = true;
            lastClicked = currentSlot;
        } else {
            fail(playerMenuUtility.getOwner());
            playerMenuUtility.getOwner().sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.CLICKED_SAME));
        }
    }
}

