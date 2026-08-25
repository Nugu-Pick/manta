package com.chaean.manta.common.config;

import java.time.Instant;

import com.chaean.manta.common.serialization.InstantSerializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

    @Bean
    public JacksonModule instantModule() {
        return new SimpleModule("manta-instant").addSerializer(Instant.class, new InstantSerializer());
    }
}
