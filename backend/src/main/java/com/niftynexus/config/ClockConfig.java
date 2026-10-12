package com.niftynexus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Injectable clock so market-hours logic can be unit-tested at any instant. */
@Configuration
public class ClockConfig {
    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Bean
    public Clock clock() { return Clock.system(IST); }
}
