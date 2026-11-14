package com.library.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Injected wherever "today" is needed, so tests can use a fixed date. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
