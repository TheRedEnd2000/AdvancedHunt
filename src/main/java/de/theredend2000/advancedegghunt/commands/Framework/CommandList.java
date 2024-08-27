package de.theredend2000.advancedegghunt.commands.Framework;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public final class CommandList implements ICommandList {
    private static final String SEPARATOR = ChatColor.YELLOW + "-------------------------------";
    private static final String COMMAND_FORMAT = ChatColor.GREEN + "%s" + ChatColor.WHITE + " - %s";

    @Override
    public void displayCommandList(CommandSender sender, String commandLabel, List<BaseCommand> subCommandList) {
        StringBuilder message = new StringBuilder(SEPARATOR).append("\n");

        String commands = subCommandList.stream()
                .filter(cmd -> cmd.testPermissionSilent(sender))
                .map(cmd -> formatCommand(cmd, commandLabel))
                .collect(Collectors.joining("\n"));

        message.append(commands).append("\n").append(SEPARATOR);

        sender.sendMessage(message.toString());
    }

    private String formatCommand(BaseCommand cmd, String commandLabel) {
        String usage = cmd.getUsage()
                .replace("<coreCommand>", commandLabel)
                .replace("<command>", cmd.getName());
        return String.format(COMMAND_FORMAT, usage, cmd.getDescription());
    }
}
