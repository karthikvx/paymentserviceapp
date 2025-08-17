package org.example.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class VirtualThreadConfig {

    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        // Create virtual thread executor for @Async methods
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // Alternative: Traditional thread pool optimized for virtual threads
    @Bean(name = "optimizedExecutor")
    public ThreadPoolTaskExecutor optimizedTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("PaymentService-");
        executor.initialize();
        return executor;
    }
}