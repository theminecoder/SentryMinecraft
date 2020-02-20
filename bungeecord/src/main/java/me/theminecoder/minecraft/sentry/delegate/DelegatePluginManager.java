package me.theminecoder.minecraft.sentry.delegate;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * @author theminecoder
 * @version 1.0
 */
public class DelegatePluginManager extends PluginManager {

    private static final Pattern argsSplit = Pattern.compile(" ");

    private PluginManager delegate;

    private final ProxyServer proxy;
    private final Map<String, Command> commandMap;
    private final Consumer<Throwable> errorConsumer;

    public DelegatePluginManager(PluginManager delegate, ProxyServer proxy, Consumer<Throwable> errorConsumer) {
        super(proxy, null, null);
        this.delegate = delegate;
        this.proxy = proxy;
        this.errorConsumer = errorConsumer;
        try {
            Field commandMapField = PluginManager.class.getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (Map<String, Command>) commandMapField.get(delegate);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerCommand(Plugin plugin, Command command) {
        delegate.registerCommand(plugin, command);
    }

    @Override
    public void unregisterCommand(Command command) {
        delegate.unregisterCommand(command);
    }

    @Override
    public void unregisterCommands(Plugin plugin) {
        delegate.unregisterCommands(plugin);
    }

    public boolean isExecutableCommand(String commandName, CommandSender sender) {
        return delegate.isExecutableCommand(commandName, sender);
    }

    @Override
    public boolean dispatchCommand(CommandSender sender, String commandLine) {
        return this.dispatchCommand(sender, commandLine, null);
    }

    @Override
    public boolean dispatchCommand(CommandSender sender, String commandLine, List<String> tabResults) {
        String[] split = commandLine.split(" ", -1);
        // Check for chat that only contains " "
        if (split.length == 0 || split[0].isEmpty()) {
            return false;
        }

        Command command = this.getCommandIfEnabled(split[0], sender);
        if (command == null) {
            return false;
        }

        if (!command.hasPermission(sender)) {
            if (tabResults == null) {
                sender.sendMessage(proxy.getTranslation("no_permission"));
            }
            return true;
        }

        String[] args = Arrays.copyOfRange(split, 1, split.length);
        try {
            if (tabResults == null) {
                if (proxy.getConfig().isLogCommands()) {
                    proxy.getLogger().log(Level.INFO, "{0} executed command: /{1}", new Object[]
                            {
                                    sender.getName(), commandLine
                            });
                }
                command.execute(sender, args);
            } else if (commandLine.contains(" ") && command instanceof TabExecutor) {
                for (String s : ((TabExecutor) command).onTabComplete(sender, args)) {
                    tabResults.add(s);
                }
            }
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "An internal error occurred whilst executing this command.");
            ProxyServer.getInstance().getLogger().log(Level.WARNING, "Error in dispatching command", ex);
            errorConsumer.accept(ex);
        }
        return true;
    }

    public List<String> tabCompleteCommand(CommandSender sender, String commandLine) {
        return delegate.tabCompleteCommand(sender, commandLine);
    }

    private Command getCommandIfEnabled(String commandName, CommandSender sender) {
        String commandLower = commandName.toLowerCase(Locale.ROOT);

        // Check if command is disabled when a player sent the command
        if ((sender instanceof ProxiedPlayer) && proxy.getDisabledCommands().contains(commandLower)) {
            return null;
        }

        return commandMap.get(commandLower);
    }

    @Override
    public Collection<Plugin> getPlugins() {
        return delegate.getPlugins();
    }

    @Override
    public Plugin getPlugin(String name) {
        return delegate.getPlugin(name);
    }

    @Override
    public void loadPlugins() {
        delegate.loadPlugins();
    }

    @Override
    public void enablePlugins() {
        delegate.enablePlugins();
    }

    @Override
    public void detectPlugins(File folder) {
        delegate.detectPlugins(folder);
    }

    @Override
    public <T extends Event> T callEvent(T event) {
        return delegate.callEvent(event);
    }

    @Override
    public void registerListener(Plugin plugin, Listener listener) {
        delegate.registerListener(plugin, listener);
    }

    @Override
    public void unregisterListener(Listener listener) {
        delegate.unregisterListener(listener);
    }

    @Override
    public void unregisterListeners(Plugin plugin) {
        delegate.unregisterListeners(plugin);
    }

    public Collection<Map.Entry<String, Command>> getCommands() {
        return delegate.getCommands();
    }
}