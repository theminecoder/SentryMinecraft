package me.theminecoder.minecraft.sentry;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.sentry.SentryClient;
import io.sentry.connection.EventSendCallback;
import io.sentry.context.Context;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.helper.EventBuilderHelper;
import io.sentry.event.helper.ShouldSendEventCallback;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryStackTraceElement;
import io.sentry.event.interfaces.StackTraceInterface;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

class ForwardingSentryClient extends SentryClient {

    private static SentryClient defaultClient;
    private static Map<ClassLoader, SentryClient> pluginClients = Maps.newHashMap();
    private static Set<String> apiClasses = Sets.newHashSet();

    ForwardingSentryClient() {
        super(null, null);
    }

    private SentryClient determineClient(SentryStackTraceElement[] stackTrace) {
        if (stackTrace == null) return defaultClient;

        SentryClient apiClient = null;
        SentryClient client = null;

        for (SentryStackTraceElement element : stackTrace) {
            boolean apiClass = apiClasses.contains(element.getModule());
            try {
                ClassLoader loader = Class.forName(element.getModule()).getClassLoader();
                if(apiClass) apiClient = getClient(loader);
                else client = getClient(loader);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (client != null) break;
        }

        if(client == null) client = apiClient;
        return client != null ? client : defaultClient;
    }

    private void forAllClients(Consumer<SentryClient> consumer) {
        pluginClients.values().forEach(consumer);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void sendEvent(Event event) {
        SentryStackTraceElement[] stackTrace = null;
        if (event.getSentryInterfaces().containsKey(ExceptionInterface.EXCEPTION_INTERFACE))
            stackTrace = ((ExceptionInterface) event.getSentryInterfaces().get(ExceptionInterface.EXCEPTION_INTERFACE)).getExceptions().getFirst().getStackTraceInterface().getStackTrace();
        if (stackTrace == null && event.getSentryInterfaces().containsKey(StackTraceInterface.STACKTRACE_INTERFACE))
            stackTrace = ((StackTraceInterface) event.getSentryInterfaces().get(StackTraceInterface.STACKTRACE_INTERFACE)).getStackTrace();

        SentryClient client = determineClient(stackTrace);
        if (client == null) throw new RuntimeException("No default sentry client available!");
        client.sendEvent(event);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void sendEvent(EventBuilder eventBuilder) {
        Event event = eventBuilder.getEvent();
        SentryStackTraceElement[] stackTrace = null;
        if (event.getSentryInterfaces().containsKey(ExceptionInterface.EXCEPTION_INTERFACE))
            stackTrace = ((ExceptionInterface) event.getSentryInterfaces().get(ExceptionInterface.EXCEPTION_INTERFACE)).getExceptions().getFirst().getStackTraceInterface().getStackTrace();
        if (stackTrace == null && event.getSentryInterfaces().containsKey(StackTraceInterface.STACKTRACE_INTERFACE))
            stackTrace = ((StackTraceInterface) event.getSentryInterfaces().get(StackTraceInterface.STACKTRACE_INTERFACE)).getStackTrace();

        SentryClient client = determineClient(stackTrace);
        if (client == null) throw new RuntimeException("No default sentry client available!");
        client.sendEvent(eventBuilder);
    }

    @Override
    public void sendMessage(String message) {
        SentryClient client = determineClient(null);
        if (client == null) throw new RuntimeException("No default sentry client available!");
        client.sendMessage(message);
    }

    @Override
    public void sendException(Throwable throwable) {
        SentryClient client = determineClient(new ExceptionInterface(throwable).getExceptions().getFirst().getStackTraceInterface().getStackTrace());
        if (client == null) throw new RuntimeException("No default sentry client available!");
        client.sendException(throwable);
    }

    @Override
    public void runBuilderHelpers(EventBuilder eventBuilder) {
        SentryClient client = determineClient(null);
        if (client == null) throw new RuntimeException("No default sentry client available!");
        client.runBuilderHelpers(eventBuilder);
    }

    @Override
    public void removeBuilderHelper(EventBuilderHelper builderHelper) {
        forAllClients(client -> client.removeBuilderHelper(builderHelper));
    }

    @Override
    public void addBuilderHelper(EventBuilderHelper builderHelper) {
        forAllClients(client -> client.addBuilderHelper(builderHelper));
    }

    @Override
    public List<EventBuilderHelper> getBuilderHelpers() {
        SentryClient client = determineClient(null);
        if (client == null) throw new RuntimeException("No default sentry client available!");
        return client.getBuilderHelpers();
    }

    @Override
    public void closeConnection() {
        forAllClients(SentryClient::closeConnection);
    }

    @Override
    public void clearContext() {
        forAllClients(SentryClient::clearContext);
    }

    @Override
    public Context getContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRelease() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDist() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEnvironment() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getServerName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> getTags() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getMdcTags() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getExtra() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRelease(String release) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDist(String dist) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEnvironment(String environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setServerName(String serverName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addTag(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTags(Map<String, String> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExtraTags(Set<String> extraTags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMdcTags(Set<String> mdcTags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addExtraTag(String extraName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMdcTag(String tagName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addExtra(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExtra(Map<String, Object> extra) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addEventSendCallback(EventSendCallback eventSendCallback) {
        forAllClients(client -> client.addEventSendCallback(eventSendCallback));
    }

    @Override
    public void addShouldSendEventCallback(ShouldSendEventCallback shouldSendEventCallback) {
        forAllClients(client -> client.addShouldSendEventCallback(shouldSendEventCallback));
    }

    static void setDefaultClient(SentryClient client) {
        ForwardingSentryClient.defaultClient = client;
    }

    static void registerClient(ClassLoader loader, SentryClient client) {
        pluginClients.put(loader, client);
    }

    static SentryClient getClient(ClassLoader loader) {
        return pluginClients.get(loader);
    }

    static void addAPIClass(Class clazz) {
        String qualifiedClassName = clazz.getSimpleName();
        Class outerClass = clazz.getEnclosingClass();

        while (outerClass != null) {
            qualifiedClassName = outerClass.getSimpleName() + "$" + qualifiedClassName;
            outerClass = outerClass.getEnclosingClass();
        }
        qualifiedClassName = clazz.getPackage().getName() + "." + qualifiedClassName;

        apiClasses.add(qualifiedClassName);
    }

}
