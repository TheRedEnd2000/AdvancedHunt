package de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Objects;

public class EggRewardsInventory implements Listener {

    private Main plugin;
    private Inventory inventory;
    private Player owner;
    private String id;
    private String section;
    private String title;
    int page = 0;
    int maxItemsPerPage = 28;
    int index = 0;

    public EggRewardsInventory(){
        this.plugin = Main.getInstance();
        Bukkit.getPluginManager().registerEvents(this,plugin);
        title = "Egg Rewards";
    }

    public void open(Player owner,String id, String section){
        this.owner = owner;
        this.section = section;
        this.id = id;
        inventory = Bukkit.createInventory(owner,54,title);
        fill();
        this.owner.openInventory(inventory);
    }

    public void fill(){
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.WHITE_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        inventory.setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19")).setLore("§6Page: §7(§b"+(page+1)+"§7/§b"+getMaxPages()+"§7)","","§eClick to scroll.").setDisplayname("§2Left").build());

        inventory.setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ==")).setLore("§6Page: §7(§b"+(page+1)+"§7/§b"+getMaxPages()+"§7)","","§eClick to scroll.").setDisplayname("§2Right").build());
        inventory.setItem(53, new ItemBuilder(XMaterial.GOLD_INGOT).setDisplayname("§5Create new Reward").setLore("§eClick to create a new reward").build());
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§cClose").build());
        setMenuItems();
    }

    public void setMenuItems() {
        String section = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(owner.getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs."+id+".Rewards")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs."+id+".Rewards").getKeys(false));
        }else
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Rewards").setLore("§7Create new a new reward","§7or load a preset.").build());
        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String command = placedEggs.getString("PlacedEggs."+id+".Rewards."+keys.get(index)+".command").replaceAll("§","&");
                    boolean enabled = placedEggs.getBoolean("PlacedEggs."+id+".Rewards."+keys.get(index)+".enabled");
                    inventory.addItem(new ItemBuilder(Main.getInstance().getMaterial(Main.getInstance().getConfig().getString("Settings.RewardInventoryMaterial"))).setDisplayname("§b§lReward §7#"+keys.get(index)).setLore("","§9Information:","§7Command: §6"+command,"§7Command Enabled: "+(enabled ? "§atrue" : "§cfalse"),"","§eClick to configure the command.").setLocalizedName(keys.get(index)).build());
                }
            }
        }else{
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Commands").setLore("§7You can add commands by using","§e/egghunt placeEggs§7.").build());
        }
    }

    @EventHandler
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        String section = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(p.getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        if(!e.getView().getTitle().equals(title)) return;
        if(e.getCurrentItem() == null) return;
        e.setCancelled(true);

        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs."+id+".Rewards.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs."+id+".Rewards.").getKeys(false));
            for(String id : placedEggs.getConfigurationSection("PlacedEggs."+id+".Rewards.").getKeys(false)){
                if(Objects.requireNonNull(e.getCurrentItem().getItemMeta()).getLocalizedName().equals(id)){;
                    Main.getInstance().getInventoryManager().createCommandSettingsMenu(p,id);
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
            }
        }

        if (e.getCurrentItem().getType().equals(Material.BARRIER)) {
            p.closeInventory();
            p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
        }else if(e.getCurrentItem().getType().equals(Material.PLAYER_HEAD)){
            if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")){
                if (page == 0){
                    p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventoryFailedSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }else{
                    page = page - 1;
                    reopen();
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
            }else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")){
                if (!((index + 1) >= keys.size())){
                    page = page + 1;
                    reopen();
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }else{
                    p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventoryFailedSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
            }else if(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Add command")){
                p.sendMessage("new");
            }
        }
    }

    public void reopen(){
        open(owner,id,section);
    }


    public int getMaxPages(){
        String section = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(owner.getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs."+id+".Rewards")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs."+id+".Rewards.").getKeys(false));
        }
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / maxItemsPerPage);
    }

}
