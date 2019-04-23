package me.theminecoder.minecraft.sentry.deletgate;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author theminecoder
 * @version 1.0
 */
public class DelegateCommandMap extends SimpleCommandMap {
    private static final Pattern PATTERN_ON_SPACE = Pattern.compile(" ", 16);

    private SimpleCommandMap delegate;
    private Map<String, Command> knownCommands;
    private final Consumer<Throwable> errorConsumer;

    private boolean doRegister = false;

    public DelegateCommandMap(SimpleCommandMap bukkitCommandMap, Consumer<Throwable> errorConsumer) {
        super(Bukkit.getServer());
        this.errorConsumer = errorConsumer;
        doRegister = true;
        this.delegate = bukkitCommandMap;
        try {
            Field knownCommandsField = bukkitCommandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            this.knownCommands = (Map<String, Command>) knownCommandsField.get(this.delegate);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerAll(String s, List<Command> list) {
        if (doRegister) {
            delegate.registerAll(s, list);
        }
    }

    @Override
    public boolean register(String s, String s1, Command command) {
        return !doRegister || delegate.register(s, s1, command);
    }

    @Override
    public boolean register(String s, Command command) {
        return !doRegister || delegate.register(s, command);
    }

    @Override
    public boolean dispatch(CommandSender sender, String commandLine) throws CommandException {
        String[] args = PATTERN_ON_SPACE.split(commandLine);
        if (args.length == 0) {
            return false;
        } else {
            String sentCommandLabel = args[0].toLowerCase();
            Command target = this.getCommand(sentCommandLabel);
            boolean doTimings = false;
            Object timings = null;
            try {
                Field timingsField = target.getClass().getField("timings");
                timingsField.setAccessible(true);
                timings = timingsField.get(target);
                doTimings = true;
            } catch (Throwable ignored) {
            }
            if (target == null) {
                return false;
            } else {
                try {
                    if (doTimings)
                        invokeTimingsMethod(timings, "startTiming");
                    target.execute(sender, sentCommandLabel, Arrays.copyOfRange(args, 1, args.length));
                    if (doTimings)
                        invokeTimingsMethod(timings, "stopTiming");
                    return true;
                } catch (Throwable e) {
                    if (doTimings)
                        invokeTimingsMethod(timings, "stopTiming");
                    errorConsumer.accept(e);
                    throw e;
                }
            }
        }
    }

    private void invokeTimingsMethod(Object timings, String method) {
        try {
            Method timingsMethod = timings.getClass().getMethod(method);
            timingsMethod.invoke(timings);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearCommands() {
        if (doRegister) {
            delegate.clearCommands();
        }
    }

    @Override
    public Command getCommand(String s) {
        return delegate.getCommand(s);
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String s) throws IllegalArgumentException {
        return delegate.tabComplete(commandSender, s);
    }
}