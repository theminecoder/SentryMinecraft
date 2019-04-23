package me.theminecoder.minecraft.sentry;

public class SentryConfigurationOptions {

    private boolean defaultClient;

    private String release;
    private String environment;
    private String serverName;

    private SentryConfigurationOptions() {
    }

    public boolean isDefaultClient() {
        return defaultClient;
    }

    public String getRelease() {
        return release;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getServerName() {
        return serverName;
    }

    public static final class Builder {
        private boolean defaultClient = false;
        private String release;
        private String environment;
        private String serverName;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder asDefaultClient() {
            this.defaultClient = true;
            return this;
        }

        public Builder withRelease(String release) {
            this.release = release;
            return this;
        }

        public Builder withEnvironment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder withServerName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        public SentryConfigurationOptions build() {
            SentryConfigurationOptions sentryConfigurationOptions = new SentryConfigurationOptions();
            sentryConfigurationOptions.release = this.release;
            sentryConfigurationOptions.defaultClient = this.defaultClient;
            sentryConfigurationOptions.environment = this.environment;
            sentryConfigurationOptions.serverName = this.serverName;
            return sentryConfigurationOptions;
        }
    }
}
