package me.theminecoder.minecraft.sentry;

import me.theminecoder.minecraft.sentry.delegate.DelegateEventBus;
import me.theminecoder.minecraft.sentry.delegate.DelegatePluginManager;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventBus;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class SentryMinecraftBungeecord extends Plugin {

    @Override
    public void onEnable() {
        try {
            Field eventBusField = PluginManager.class.getDeclaredField("eventBus");
            eventBusField.setAccessible(true);
            eventBusField.set(this.getProxy().getPluginManager(), new DelegateEventBus((EventBus) eventBusField.get(this.getProxy().getPluginManager()), SentryMinecraft::sendIfActive));

            Field pluginManagerField = this.getProxy().getClass().getDeclaredField("pluginManager");
            pluginManagerField.setAccessible(true);
            if (Modifier.isFinal(pluginManagerField.getModifiers())) {
                Field modifierField = Field.class.getDeclaredField("modifiers");
                modifierField.setAccessible(true);
                modifierField.set(pluginManagerField, pluginManagerField.getModifiers() & ~Modifier.FINAL);
            }
            pluginManagerField.set(this.getProxy(), new DelegatePluginManager(this.getProxy().getPluginManager(), this.getProxy(), SentryMinecraft::sendIfActive));
        } catch (ReflectiveOperationException e) {
            this.getLogger().severe("Could not install proxy plugin manager! Auto-handling of event or command exceptions will not work!");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
