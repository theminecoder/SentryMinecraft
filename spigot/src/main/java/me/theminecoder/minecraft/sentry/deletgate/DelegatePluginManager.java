package me.theminecoder.minecraft.sentry.deletgate;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.event.*;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.*;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author theminecoder
 * @version 1.0
 */
public class DelegatePluginManager implements PluginManager {

    private PluginManager delegate;
    private CommandMap commandMap;
    private Object timings;
    private final Consumer<Throwable> errorConsumer;

    public DelegatePluginManager(PluginManager delegate, Consumer<Throwable> errorConsumer) {
        this.delegate = delegate;
        this.errorConsumer = errorConsumer;
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            try {
                Field timingsField = delegate.getClass().getDeclaredField("timings");
                timingsField.setAccessible(true);
                timings = timingsField.get(delegate);
            } catch (ReflectiveOperationException ignored) {
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerInterface(Class<? extends PluginLoader> loader) throws IllegalArgumentException {
        delegate.registerInterface(loader);
    }

    @Override
    public Plugin getPlugin(String name) {
        return delegate.getPlugin(name);
    }

    @Override
    public Plugin[] getPlugins() {
        return delegate.getPlugins();
    }

    @Override
    public boolean isPluginEnabled(String name) {
        return delegate.isPluginEnabled(name);
    }

    @Override
    public boolean isPluginEnabled(Plugin plugin) {
        return delegate.isPluginEnabled(plugin);
    }

    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        return delegate.loadPlugin(file);
    }

    @Override
    public Plugin[] loadPlugins(File directory) {
        return delegate.loadPlugins(directory);
    }

    @Override
    public void disablePlugins() {
        delegate.disablePlugins();
    }

    @Override
    public void clearPlugins() {
        delegate.clearPlugins();
    }

    @Override
    public void callEvent(Event event) throws IllegalStateException {
        delegate.callEvent(event);
    }

    @Override
    public void registerEvents(Listener listener, Plugin plugin) {
        if (!plugin.isEnabled())
            throw new IllegalPluginAccessException("Plugin attempted to register " + listener + " while not enabled");

        // Just in case Bukkit decides to validate the parameters in the future
        EventExecutor nullExecutor = (arg0, arg1) -> {
            throw new IllegalStateException("This method should never be called!");
        };

        for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : plugin.getPluginLoader().createRegisteredListeners(listener, plugin).entrySet()) {

            Collection<RegisteredListener> listeners = entry.getValue();
            Collection<RegisteredListener> modified = Lists.newArrayList();

            // Use our plugin specific logger instead
            for (final RegisteredListener delegate : listeners) {
                RegisteredListener customListener = new RegisteredListener(delegate.getListener(), nullExecutor, delegate.getPriority(), delegate.getPlugin(), delegate.isIgnoringCancelled()) {
                    @Override
                    public void callEvent(Event event) throws EventException {
                        try {
                            delegate.callEvent(event);
                        } catch (AuthorNagException e) {
                            // Let Bukkit handle that one
                            throw e;
                        } catch (Throwable e) {
                            errorConsumer.accept(e);
                            throw e;
                        }
                    }
                };

                modified.add(customListener);
            }

            getEventListeners(getRegistrationClass(entry.getKey())).registerAll(modified);
        }
    }

    @Override
    public void registerEvent(Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, Plugin plugin) {
        this.registerEvent(event, listener, priority, executor, plugin, false);
    }

    @Override
    public void registerEvent(Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, Plugin plugin, boolean ignoreCancelled) {
        delegate.registerEvent(event, listener, priority, getWrappedExecutor(executor), plugin, ignoreCancelled);
    }

    private EventExecutor getWrappedExecutor(EventExecutor executor) {
        return (listenerObject, eventObject) -> {
            try {
                executor.execute(listenerObject, eventObject);
            } catch (AuthorNagException e) {
                throw e;
            } catch (Throwable e) {
                errorConsumer.accept(e);
                throw e;
            }
        };
    }

    private HandlerList getEventListeners(Class<? extends Event> type) {
        try {
            Method method = getRegistrationClass(type).getDeclaredMethod("getHandlerList");
            method.setAccessible(true);
            return (HandlerList) method.invoke(null);
        } catch (Exception e) {
            throw new IllegalPluginAccessException(e.toString());
        }
    }

    private Class<? extends Event> getRegistrationClass(Class<? extends Event> clazz) {
        try {
            clazz.getDeclaredMethod("getHandlerList");
            return clazz;

        } catch (NoSuchMethodException e) {
            if ((clazz.getSuperclass() != null)
                    && (!clazz.getSuperclass().equals(Event.class))
                    && (Event.class.isAssignableFrom(clazz.getSuperclass()))) {
                return getRegistrationClass(clazz.getSuperclass().asSubclass(
                        Event.class));
            }
        }
        throw new IllegalPluginAccessException("Unable to find handler list for event " + clazz.getName());
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        delegate.enablePlugin(plugin);
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        delegate.enablePlugin(plugin);
    }

    @Override
    public void disablePlugin(Plugin plugin, boolean closeClassloader) {
        delegate.disablePlugin(plugin, closeClassloader);
    }

    @Override
    public Permission getPermission(String name) {
        return delegate.getPermission(name);
    }

    @Override
    public void addPermission(Permission perm) {
        delegate.addPermission(perm);
    }

    @Override
    public void removePermission(Permission perm) {
        delegate.removePermission(perm);
    }

    @Override
    public void removePermission(String name) {
        delegate.removePermission(name);
    }

    @Override
    public Set<Permission> getDefaultPermissions(boolean op) {
        return delegate.getDefaultPermissions(op);
    }

    @Override
    public void recalculatePermissionDefaults(Permission perm) {
        delegate.recalculatePermissionDefaults(perm);
    }

    @Override
    public void subscribeToPermission(String permission, Permissible permissible) {
        delegate.subscribeToPermission(permission, permissible);
    }

    @Override
    public void unsubscribeFromPermission(String permission, Permissible permissible) {
        delegate.unsubscribeFromPermission(permission, permissible);
    }

    @Override
    public Set<Permissible> getPermissionSubscriptions(String permission) {
        return delegate.getPermissionSubscriptions(permission);
    }

    @Override
    public void subscribeToDefaultPerms(boolean op, Permissible permissible) {
        delegate.subscribeToDefaultPerms(op, permissible);
    }

    @Override
    public void unsubscribeFromDefaultPerms(boolean op, Permissible permissible) {
        delegate.unsubscribeFromDefaultPerms(op, permissible);
    }

    @Override
    public Set<Permissible> getDefaultPermSubscriptions(boolean op) {
        return delegate.getDefaultPermSubscriptions(op);
    }

    @Override
    public Set<Permission> getPermissions() {
        return delegate.getPermissions();
    }

    @Override
    public boolean useTimings() {
        return delegate.useTimings();
    }

    public void useTimings(boolean timings) {
        try {
            Method useTimingsDelegate = delegate.getClass().getMethod("useTimings", boolean.class);
            useTimingsDelegate.invoke(delegate, timings);
        } catch (ReflectiveOperationException e){
        }
    }
}