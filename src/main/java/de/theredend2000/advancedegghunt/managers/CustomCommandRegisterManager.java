package de.theredend2000.advancedegghunt.managers;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.commands.AdvancedEggHuntCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;

import java.lang.reflect.Field;
import java.util.List;

public class CustomCommandRegisterManager {

    private Main plugin;

    public CustomCommandRegisterManager(){
        plugin = Main.getInstance();
    }

    public void registerDynamicCommand() {
        String commandName = plugin.getPluginConfig().getCommandFirstEntry();
        List<String> aliases = plugin.getPluginConfig().getCommandAlias();

        CommandMap commandMap = getCommandMap();
        if (commandMap != null) {
            Command command = plugin.getServer().getPluginCommand(commandName);

            if (command == null) {
                command = new BukkitCommand(commandName) {
                    @Override
                    public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
                        return new AdvancedEggHuntCommand().onCommand(sender, this, label, args);
                    }
                };
                command.setAliases(aliases);
                commandMap.register(plugin.getDescription().getName(), command);
            } else
                command.getAliases().addAll(aliases);
        }
    }

    private CommandMap getCommandMap() {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(plugin.getServer());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
