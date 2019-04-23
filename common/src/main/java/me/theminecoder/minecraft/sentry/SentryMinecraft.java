package me.theminecoder.minecraft.sentry;

import io.sentry.Sentry;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;

public class SentryMinecraft {

    private static ForwardingSentryClient forwardingSentryClient;

    public static void init(ClassLoader loader, String dsn) {
        SentryMinecraft.init(loader, dsn, SentryConfigurationOptions.Builder.create().build());
    }

    public static void init(ClassLoader loader, String dsn, SentryConfigurationOptions options) {
        if (options.getEnvironment() != null && !dsn.contains("environment")) {
            dsn += (dsn.contains("?") ? "&" : "?") + "environment=" + options.getRelease();
        }
        if (options.getRelease() != null && !dsn.contains("release")) {
            dsn += (dsn.contains("?") ? "&" : "?") + "release=" + options.getRelease();
        }
        if (options.getRelease() != null && !dsn.contains("servername")) {
            dsn += (dsn.contains("?") ? "&" : "?") + "servername=" + options.getRelease();
        }

        if (!dsn.contains("uncaught.handler.enabled")) {
            dsn += (dsn.contains("?") ? "&" : "?") + "uncaught.handler.enabled=" + options.isDefaultClient();
        }

        SentryClient client = SentryClientFactory.sentryClient(dsn);

        if (forwardingSentryClient == null) {
            Sentry.setStoredClient(forwardingSentryClient = new ForwardingSentryClient());
        }

        ForwardingSentryClient.registerClient(loader, client);
        if (options.isDefaultClient()) {
            ForwardingSentryClient.setDefaultClient(client);
        }
    }

    public static void addAPIClass(Class... classes) {
        for (Class clazz : classes) {
            ForwardingSentryClient.addAPIClass(clazz);
        }
    }

    public static void sendIfActive(Throwable e) {
        SentryClient client = Sentry.getStoredClient();
        if (client != null) client.sendException(e);
    }

}
