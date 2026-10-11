package io.blossom.erp.config;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    /** Fail fast (10s instead of 30s) when Atlas is unreachable, so problems show up quickly in the Render logs. */
    @Bean
    public MongoClientSettingsBuilderCustomizer mongoTimeouts() {
        return builder -> builder.applyToClusterSettings(c -> c.serverSelectionTimeout(10, TimeUnit.SECONDS));
    }
}
