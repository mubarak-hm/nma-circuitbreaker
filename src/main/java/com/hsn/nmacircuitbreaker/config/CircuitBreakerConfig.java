package com.hsn.nmacircuitbreaker.config;

import com.hsn.nmacircuitbreaker.breaker.CircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class CircuitBreakerConfig {
    @Bean
    ScheduledExecutorService circuitBreakerScheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }


    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }


    @Bean
    CircuitBreaker circuitBreaker(ScheduledExecutorService circuitBreakerScheduler) {
        return new CircuitBreaker(circuitBreakerScheduler, 50.0, 2, Duration.ofSeconds(30), 20, 5);
    }
}
