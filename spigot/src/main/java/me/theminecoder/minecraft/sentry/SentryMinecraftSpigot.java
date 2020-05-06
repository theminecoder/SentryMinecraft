package me.theminecoder.minecraft.sentry;

import me.theminecoder.minecraft.sentry.deletgate.DelegateCommandMap;
import me.theminecoder.minecraft.sentry.deletgate.DelegatePluginManager;
import org.apache.commons.lang.UnhandledException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;

public final class SentryMinecraftSpigot extends JavaPlugin {

    private boolean hasPaperEvents_18 = false;
    private boolean hasPaperEvents_19 = false;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        if (getConfig().getBoolean("use-builtin-handlers", true)) {
            try {
                Class.forName("com.destroystokyo.paper.event.server.ServerExceptionEvent");
                hasPaperEvents_19 = true;
            } catch (Throwable ignored) {
            }
            try {
                Class.forName("org.github.paperspigot.event.ServerExceptionEvent");
                hasPaperEvents_18 = true;
            } catch (Throwable ignored) {
            }

            if (!hasPaperEvents_18 && !hasPaperEvents_19) { //Inject handlers
                try {
                    Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                    commandMapField.setAccessible(true);
                    if (Modifier.isFinal(commandMapField.getModifiers())) {
                        Field modifierField = Field.class.getDeclaredField("modifiers");
                        modifierField.setAccessible(true);
                        modifierField.set(commandMapField, commandMapField.getModifiers() & ~Modifier.FINAL);
                    }
                    commandMapField.set(Bukkit.getServer(), new DelegateCommandMap((SimpleCommandMap) commandMapField.get(Bukkit.getServer()), this::handleException));
                } catch (ReflectiveOperationException e) {
                    this.getLogger().severe("Could not install proxy command map! Auto-handling of command exceptions will not work!");
                    e.printStackTrace();
                }

                try {
                    Field pluginManagerField = Bukkit.getServer().getClass().getDeclaredField("pluginManager");
                    pluginManagerField.setAccessible(true);
                    if (Modifier.isFinal(pluginManagerField.getModifiers())) {
                        Field modifierField = Field.class.getDeclaredField("modifiers");
                        modifierField.setAccessible(true);
                        modifierField.set(pluginManagerField, pluginManagerField.getModifiers() & ~Modifier.FINAL);
                    }
                    pluginManagerField.set(Bukkit.getServer(), new DelegatePluginManager(Bukkit.getPluginManager(), this::handleException));
                } catch (ReflectiveOperationException e) {
                    this.getLogger().severe("Could not install proxy plugin manager! Auto-handling of event exceptions will not work!");
                    e.printStackTrace();
                }

                this.getLogger().severe("Due to api issues, we are unable to install a proxy scheduler into the server. Auto-handling of scheduler exceptions will not work!");
            }
        }

        if (getConfig().getString("default-dsn", "").trim().length() > 0) {
            //I really don't get why they removed this....
            Properties properties = new Properties();
            try (FileReader reader = new FileReader(new File("server.properties"))) {
                properties.load(reader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            SentryMinecraft.init(ClassLoader.getSystemClassLoader(), getConfig().getString("default-dsn"), SentryConfigurationOptions.Builder.create()
                    .withServerName(properties.getProperty("server-id", getConfig().getString("server-name", "Unknown Server")).replace(" ", "+"))
                    .asDefaultClient()
                    .build());
        }
    }

    @Override
    public void onEnable() {
        if (getConfig().getBoolean("use-builtin-handlers", true)) {
            if (hasPaperEvents_19) {
                this.getServer().getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void onServerException(com.destroystokyo.paper.event.server.ServerExceptionEvent event) {
                        handleException(event.getException().getCause());
                    }
                }, this);
            } else if (hasPaperEvents_18) {
                this.getServer().getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void onServerException(org.github.paperspigot.event.ServerExceptionEvent event) {
                        handleException(event.getException().getCause());
                    }
                }, this);
            }
        }
    }

    private void handleException(Throwable cause) {
        Throwable e = cause;
        if (e.getCause() != null) { // Paper wraps the actual exception
            e = e.getCause();
        }
        if (e instanceof UnhandledException && e.getCause() != null) {
            e = e.getCause();
        }
        if (e instanceof CommandException && e.getCause() != null) {
            e = e.getCause();
        }
        Throwable finalE = e;
        getServer().getScheduler().runTaskAsynchronously(SentryMinecraftSpigot.this, () -> SentryMinecraft.sendIfActive(finalE));
    }

}
