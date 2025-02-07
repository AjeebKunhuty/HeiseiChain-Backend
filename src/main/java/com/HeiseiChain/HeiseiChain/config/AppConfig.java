package com.HeiseiChain.HeiseiChain.config;

import com.HeiseiChain.HeiseiChain.model.Blockchain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public Blockchain blockchain() {
        return new Blockchain();
    }
}
