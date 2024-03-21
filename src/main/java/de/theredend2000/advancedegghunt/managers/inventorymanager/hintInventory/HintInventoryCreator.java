package de.theredend2000.advancedegghunt.managers.inventorymanager.hintInventory;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class HintInventoryCreator implements Listener {
    private HintInventoryCreator self;
    private Player player;
    private Inventory inventory;
    private boolean active;
    private int currentSlot;
    private int currentCount;
    private boolean clickedRight;
    private int lastClicked;
    private Random random;

    public HintInventoryCreator(Player player, Inventory inventory, boolean active){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        self = this;
        this.player = player;
        this.inventory = inventory;
        this.active = active;
        this.clickedRight = true;
        this.currentCount = 0;
        this.lastClicked = 100;
        random = new Random();
        open(player);
    }

    private void open(Player player){
        if(player != null) {
            player.openInventory(inventory);
            for (int i = 0; i<inventory.getSize();i++){inventory.setItem(i, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
            startAnimating();
            Main.getInstance().getCooldownManager().setCooldown(player);
        }
    }

    private void startAnimating(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if(active){
                    if(!clickedRight) {
                        fail(player);
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.CLICKED_SAME));
                        return;
                    }
                    inventory.clear();
                    currentSlot = getRandomSlot();
                    for (int i = 0; i<inventory.getSize();i++){inventory.setItem(i, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
                    inventory.setItem(currentSlot, new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE).setDisplayname("§aConfirm").setLore("§6" + (currentCount + 1) + "§7/§6" + Main.getInstance().getPluginConfig().getHintCount()).build());
                    clickedRight = false;
                }else{
                    HandlerList.unregisterAll(self);
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 40, Main.getInstance().getPluginConfig().getHintUpdateTime());
    }

    private int getRandomSlot(){
        int nextNum;
        do {
            nextNum = random.nextInt(inventory.getSize());
        } while (nextNum == lastClicked);
        return nextNum;
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event){
        SoundManager soundManager = Main.getInstance().getSoundManager();
        if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null || !event.getInventory().equals(inventory)) {
            return;
        }
        event.setCancelled(true);
        if (currentSlot == lastClicked) {
            fail(player);
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.CLICKED_SAME));
            return;
        }
        if (event.getSlot() == currentSlot) {
            currentCount++;
            player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
            if (currentCount == Main.getInstance().getPluginConfig().getHintCount()) {
                player.closeInventory();
                active = false;
                player.playSound(player.getLocation(), soundManager.playAllEggsFound(), soundManager.getSoundVolume(), 1);
                player.sendMessage(getReward(player));
            }
            clickedRight = true;
            lastClicked = currentSlot;
        } else {
            fail(player);
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.CLICKED_SAME));
        }
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

    public void fail(Player player){
        player.closeInventory();
        player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playErrorSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
        active = false;
    }
}
