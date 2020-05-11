package me.theminecoder.minecraft.sentry;

import io.github.waterfallmc.waterfall.event.ProxyExceptionEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public final class SentryMinecraftBungeecord extends Plugin implements Listener {

    @Override
    public void onEnable() {
        try {
            Class.forName("io.github.waterfallmc.waterfall.event.ProxyExceptionEvent");
        } catch (ClassNotFoundException e) {
            getLogger().warning("SentryMinecraft no longer supports BungeeCord plugin manager wrapping. Please use Waterfall for global exception handling");
            return;
        }

        this.getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void exceptionEvent(ProxyExceptionEvent event) {
        this.handleException(event.getException());
    }

    private void handleException(Throwable cause) {
        Throwable e = cause;
        if (e.getCause() != null) { // Waterfall wraps the actual exception
            e = e.getCause();
        }
        Throwable finalE = e;
        getProxy().getScheduler().runAsync(this, () -> SentryMinecraft.sendIfActive(finalE));
    }
}
