package de.theredend2000.advancedegghunt.commands.Framework;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a main command that has subcommands within it. Like /stafftools freeze is /[corecommand] [subcommand]
 */
public final class CoreCommand extends BaseCommand {
    private final List<BaseCommand> subCommands;
    private final ICommandList commandList;

    /**
     * @param commandList
     * @param subCommands Class reference to each SubCommand you create for this core command
     */
    public CoreCommand(@Nullable ICommandList commandList, List<BaseCommand> subCommands) {
        this.subCommands = new ArrayList<>(subCommands);
        this.commandList = commandList;
    }

    public List<BaseCommand> getSubCommands() {
        return Collections.unmodifiableList(subCommands);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (args.length > 0) {
            for (BaseCommand subCommand : subCommands) {
                if (args[0].equalsIgnoreCase(subCommand.getName())) {
                    if (!subCommand.testPermission(sender)) {
                        return true;
                    }

                    String sentCommandLabel = args[0].toLowerCase(java.util.Locale.ENGLISH);
                    String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                    return subCommand.onCommand(sender, command, sentCommandLabel, newArgs);
                }
            }
        }
        if (commandList != null) {
            commandList.displayCommandList(sender, commandLabel, subCommands);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterArguments(subCommands.stream()
                    .filter(cmd -> cmd.testPermissionSilent(sender))
                    .map(BaseCommand::getName)
                    .collect(Collectors.toList()), args);
        } else if (args.length >= 2) {
            for (BaseCommand subCommand : subCommands) {
                if (args[0].equalsIgnoreCase(subCommand.getName())) {
                    if (!subCommand.testPermissionSilent(sender)) {
                        return Collections.emptyList();
                    }

                    String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                    List<String> subCommandArgs = subCommand.onTabComplete(sender, command, label, newArgs);

                    return filterArguments(subCommandArgs, newArgs);
                }
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterArguments(List<String> arguments, String[] args) {
        if (arguments == null || arguments.isEmpty()) {
            return Collections.emptyList();
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return arguments.stream()
                .filter(arg -> arg.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}
