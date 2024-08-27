package de.theredend2000.advancedegghunt.commands.Framework;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * An Advanced Command-Managing system allowing you to register subcommands under a core command.
 */
public class CommandManager {
    private final JavaPlugin plugin;
    private final Map<String, Object> defaults;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.defaults = new HashMap<>();
        defaults.put("commandList", new CommandList());
        defaults.put("syntax", "/<coreCommand> <command>");
        defaults.put("permissionMessage", "You do not have permission to do that!");
    }

    public void setDefault(String key, Object value) {
        defaults.put(key, value);
    }

    /**
     * @param name    Command name
     * @param command The command that is being registered.
     */
    public void setupCommand(String name, BaseCommand command) {
        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    /**
     * @param name        Command name
     * @param commandList Custom command list or null to use default
     * @param subCommands Class reference to each SubCommand you create for this core command
     */
    public void setupCoreCommand(String name, ICommandList commandList, BaseCommand... subCommands) {
        ICommandList finalCommandList = (commandList != null) ? commandList : (ICommandList) defaults.get("commandList");
        ArrayList<BaseCommand> subCommandList = new ArrayList<>(Arrays.asList(subCommands));

        CoreCommand coreCommand = new CoreCommand(finalCommandList, subCommandList);

        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(coreCommand);
            pluginCommand.setTabCompleter(coreCommand);
        }

        subCommandList.forEach(subCommand -> {
            if (subCommand.getUsage().isEmpty()) {
                subCommand.setUsage((String) defaults.get("syntax"));
            }
            if (subCommand.getPermissionMessage().isEmpty()) {
                subCommand.setPermissionMessage((String) defaults.get("permissionMessage"));
            }
        });
    }
}
