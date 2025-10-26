package de.theredend2000.advancedhunt.commands;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.configurations.PluginConfig;
import de.theredend2000.advancedhunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedhunt.protocollib.BlockChangingManager;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.enums.Permission;
import de.theredend2000.advancedhunt.util.messages.MenuManager;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class AdminCommands implements TabExecutor {

    private MessageManager messageManager;
    private MenuManager menuManager;
    private Main plugin;
    private EggManager eggManager;

    private static volatile AdminCommands instance;


    public AdminCommands() {
        messageManager = Main.getInstance().getMessageManager();
        menuManager = Main.getInstance().getMenuManager();
        plugin = Main.getInstance();
        eggManager = Main.getInstance().getEggManager();
    }

    public static AdminCommands getInstance() {
        if (instance == null) {
            synchronized (PluginConfig.class) {
                if (instance == null) {
                    instance = new AdminCommands();
                }
            }
        }
        return instance;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if (!(plugin.getDevs().contains(player.getUniqueId()))){
            player.sendMessage("You are not");
            return true;
        }
        if(!player.isOp()){
            player.sendMessage("You cant use admin commands outside of non testing servers.");
            return false;
        }
        if (args.length == 0) return true;

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "debug-egg":
                handleDebugEgg(player);
                break;
            case "uuid":
                TextComponent message = new TextComponent("Click to get UUID in chat.");
                message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.valueOf(player.getUniqueId())));
                player.spigot().sendMessage(message);
                break;
            case "log":
                plugin.getLogHelper().toggleSendLog();
                player.sendMessage("Set send log to "+plugin.getLogHelper().getSendLog());
                break;
            case "resend":
                //plugin.getBlockChangingManager().resendAllGhostBlocks(player); removed for update
                player.sendMessage("Done!");
                break;
        }
        return false;
    }

    private void handleDebugEgg(Player player){
        Location location = player.getLocation();
        Location blockCenter = location.getBlock().getLocation().add(0.5, -0.725, 0.5);
        ArmorStand armorStand = (ArmorStand) blockCenter.getWorld().spawnEntity(blockCenter, EntityType.ARMOR_STAND);
        armorStand.setSmall(true);
        armorStand.setVisible(true);
        armorStand.setAI(false);
        armorStand.setGravity(false);
        armorStand.setSilent(true);
        armorStand.setInvulnerable(true);
        armorStand.setBasePlate(false);
        armorStand.setArms(false);
        armorStand.addScoreboardTag("debug-egg");
        double angle = Math.toRadians(45);
        EulerAngle pose = new EulerAngle(0, angle, 0);
        armorStand.setHeadPose(pose);
        EulerAngle bodyPose = new EulerAngle(Math.toRadians(-180), Math.toRadians(1), 0);
        armorStand.setBodyPose(bodyPose);
        EulerAngle feedPose = new EulerAngle(Math.toRadians(-180), Math.toRadians(1), 0);
        armorStand.setLeftLegPose(feedPose);
        armorStand.setRightLegPose(feedPose);

        armorStand.setHelmet(new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("NTIzZDkyMmJlMGJhZTA1ZDBlN2I4OGU2NDljMTlmZTNiMmRhZTQzNjM5ZGRkMDljYjcxZTI4M2JmMDM0ZjY4OSJ9fX0=")).build());
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 1:

                break;
            case 2:

                break;
            case 3:

                break;
        }
        return filterArguments(completions, args);
    }

    private List<String> filterArguments(List<String> arguments, String[] args) {
        if (arguments == null || arguments.isEmpty())
            return Collections.emptyList();

        String lastArg = args[args.length - 1].toLowerCase();
        return arguments.stream()
                .filter(arg -> arg.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}
