package de.theredend2000.advancedhunt.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class DynamicCommandRegistrar {

    private final JavaPlugin plugin;
    private final Map<String, PluginCommand> registeredCommands = new HashMap<>();

    public DynamicCommandRegistrar(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public CommandBuilder command(String name) {
        return new CommandBuilder(name);
    }

    public class CommandBuilder {
        private final String name;
        private List<String> aliases = new ArrayList<>();
        private String description = "";
        private String usage = "";
        private TabCompleter tabCompleter = null;
        private CommandExecutor executor = null;

        private CommandBuilder(String name) {
            this.name = name;
        }

        public CommandBuilder aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        public CommandBuilder aliases(String... aliases) {
            this.aliases = Arrays.asList(aliases);
            return this;
        }

        public CommandBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CommandBuilder usage(String usage) {
            this.usage = usage;
            return this;
        }

        public CommandBuilder tabExecuter(TabExecutor tabExecutor) {
            this.executor = tabExecutor;
            this.tabCompleter = tabExecutor;
            return this;
        }

        public CommandBuilder tabCompleter(TabCompleter tabCompleter) {
            this.tabCompleter = tabCompleter;
            return this;
        }

        public CommandBuilder executor(CommandExecutor executor) {
            this.executor = executor;
            return this;
        }

        public void register() {
            PluginCommand command = createPluginCommand(name, plugin);

            if (command != null) {
                command.setDescription(description);
                command.setUsage(usage);
                command.setAliases(aliases);
                command.setExecutor(executor != null ? executor : (sender, cmd, label, args) -> false);
                command.setTabCompleter(tabCompleter);

                registerCommand(command);
                registeredCommands.put(name, command);
            }
        }
    }

    private PluginCommand createPluginCommand(String name, JavaPlugin plugin) {
        PluginCommand command = null;

        try {
            Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            c.setAccessible(true);
            command = c.newInstance(name, plugin);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return command;
    }

    private void registerCommand(PluginCommand command) {
        CommandMap commandMap = getCommandMap();
        if (commandMap != null) {
            commandMap.register(plugin.getDescription().getName(), command);
        }
    }

    private CommandMap getCommandMap() {
        CommandMap commandMap = null;

        try {
            Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return commandMap;
    }

    public void unregisterCommands() {
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            final Field knownCommands = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommands.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> commands = (Map<String, Command>) knownCommands.get(commandMap);

            for (PluginCommand command : registeredCommands.values()) {
                command.unregister(commandMap);
                commands.remove(command.getName());
                command.getAliases().forEach(commands::remove);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to unregister commands: " + e.getMessage());
            e.printStackTrace();
        }
        registeredCommands.clear();
    }

    public void reregisterCommands() {
        for (PluginCommand command : registeredCommands.values()) {
            registerCommand(command);
        }
    }
}