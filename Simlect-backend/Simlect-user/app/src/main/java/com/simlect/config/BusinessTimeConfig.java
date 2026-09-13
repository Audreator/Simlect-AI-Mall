package com.simlect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class BusinessTimeConfig {

    @Bean
    public Clock businessClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
