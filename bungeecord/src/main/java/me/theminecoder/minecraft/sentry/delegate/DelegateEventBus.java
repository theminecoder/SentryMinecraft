package me.theminecoder.minecraft.sentry.delegate;

import net.md_5.bungee.event.EventBus;
import net.md_5.bungee.event.EventHandlerMethod;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author theminecoder
 * @version 1.0
 */
public class DelegateEventBus extends EventBus {

    private Logger logger;
    private EventBus delegate;
    private final Map<Class<?>, EventHandlerMethod[]> byEventBaked;
    private final Consumer<Throwable> errorConsumer;

    public DelegateEventBus(EventBus delegate, Consumer<Throwable> errorConsumer) {
        this.delegate = delegate;
        this.errorConsumer = errorConsumer;
        try {
            Field bakedField = EventBus.class.getDeclaredField("byEventBaked");
            bakedField.setAccessible(true);
            this.byEventBaked = (Map<Class<?>, EventHandlerMethod[]>) bakedField.get(delegate);

            Field loggerField = EventBus.class.getDeclaredField("logger");
            loggerField.setAccessible(true);
            this.logger = (Logger) loggerField.get(delegate);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void post(Object event) {
        EventHandlerMethod[] handlers = byEventBaked.get(event.getClass());
        if (handlers != null) {
            for (EventHandlerMethod method : handlers) {
                try {
                    try {
                        method.invoke(event);
                    } catch (IllegalAccessException ex) {
                        throw new Error("Method became inaccessible: " + event, ex);
                    } catch (IllegalArgumentException ex) {
                        throw new Error("Method rejected target/argument: " + event, ex);
                    } catch (InvocationTargetException ex) {
                        logger.log( Level.WARNING, MessageFormat.format( "Error dispatching event {0} to listener {1}", event, method.getListener() ), ex.getCause() );
                        throw ex.getCause();
                    }
                } catch (Throwable e) {
                    errorConsumer.accept(e);
                }
            }
        }
    }

    @Override
    public void register(Object listener) {
        delegate.register(listener);
    }

    @Override
    public void unregister(Object listener) {
        delegate.unregister(listener);
    }
}