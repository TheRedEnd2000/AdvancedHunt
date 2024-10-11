package de.theredend2000.advancedhunt.commands.Framework;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents a main command that has subcommands within it. Like /stafftools freeze is /[corecommand] [subcommand]
 */
public final class CoreCommand extends BaseCommand {
    private final Map<String, BaseCommand> subCommandMap;
    private final ICommandList commandList;

    /**
     * @param commandList
     * @param subCommands Class reference to each SubCommand you create for this core command
     */
    public CoreCommand(@Nullable ICommandList commandList, List<BaseCommand> subCommands) {
        this.subCommandMap = new HashMap<>();
        for (BaseCommand cmd : subCommands) {
            this.subCommandMap.put(cmd.getName().toLowerCase(), cmd);
        }
        this.commandList = commandList;
    }

    public List<BaseCommand> getSubCommands() {
        return new ArrayList<>(subCommandMap.values());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (args.length > 0) {
            BaseCommand subCommand = subCommandMap.get(args[0].toLowerCase());
            if (subCommand != null && subCommand.testPermission(sender)) {
                return subCommand.onCommand(sender, command, args[0], Arrays.copyOfRange(args, 1, args.length));
            }
        }
        if (commandList != null) {
            commandList.displayCommandList(sender, commandLabel, getSubCommands());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterArguments(subCommandMap.keySet(), args[0], sender);
        } else if (args.length >= 2) {
            BaseCommand subCommand = subCommandMap.get(args[0].toLowerCase());
            if (subCommand != null && subCommand.testPermissionSilent(sender)) {
                List<String> subCommandArgs = subCommand.onTabComplete(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
                return subCommandArgs != null ? filterArguments(subCommandArgs, args[args.length - 1], null) : Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    private List<String> filterArguments(Collection<String> arguments, String lastArg, CommandSender sender) {
        if (arguments == null || arguments.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        String lowerLastArg = lastArg.toLowerCase();
        for (String arg : arguments) {
            if (arg.toLowerCase().startsWith(lowerLastArg) &&
                    (sender == null || subCommandMap.get(arg.toLowerCase()) == null || subCommandMap.get(arg.toLowerCase()).testPermissionSilent(sender))) {
                result.add(arg);
            }
        }
        return result;
    }
}
