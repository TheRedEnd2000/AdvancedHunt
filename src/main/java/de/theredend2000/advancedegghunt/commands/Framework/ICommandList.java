package de.theredend2000.advancedegghunt.commands.Framework;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * A functional interface used to allow the dev to specify how the listing of the subcommands on a core command works.
 */
@FunctionalInterface
public interface ICommandList {
    /**
     * @param sender         The thing that ran the command
     * @param commandLabel   The coreCommand
     * @param subCommandList A list of all the subcommands you can display
     */
    void displayCommandList(CommandSender sender, String commandLabel, List<BaseCommand> subCommandList);
}
