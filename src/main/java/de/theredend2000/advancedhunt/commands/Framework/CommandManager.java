package de.theredend2000.advancedhunt.commands.Framework;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * An Advanced Command-Managing system allowing you to register subcommands under a core command.
 */
public class CommandManager {
    private final JavaPlugin plugin;
    private ICommandList defaultCommandList;
    private String defaultSyntax;
    private String defaultPermissionMessage;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.defaultCommandList = new CommandList();
        this.defaultSyntax = "/<coreCommand> <command>";
        this.defaultPermissionMessage = "You do not have permission to do that!";
    }

    public void setDefaultCommandList(ICommandList commandList) {
        this.defaultCommandList = commandList;
    }

    public void setDefaultSyntax(String syntax) {
        this.defaultSyntax = syntax;
    }

    public void setDefaultPermissionMessage(String permissionMessage) {
        this.defaultPermissionMessage = permissionMessage;
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
        ICommandList finalCommandList = (commandList != null) ? commandList : defaultCommandList;
        ArrayList<BaseCommand> subCommandList = new ArrayList<>(Arrays.asList(subCommands));

        CoreCommand coreCommand = new CoreCommand(finalCommandList, subCommandList);

        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(coreCommand);
            pluginCommand.setTabCompleter(coreCommand);
        }

        subCommandList.forEach(subCommand -> {
            if (subCommand.getUsage().isEmpty()) {
                subCommand.setUsage(defaultSyntax);
            }
            if (subCommand.getPermissionMessage().isEmpty()) {
                subCommand.setPermissionMessage(defaultPermissionMessage);
            }
        });
    }
}
