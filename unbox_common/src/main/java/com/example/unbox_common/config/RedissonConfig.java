package com.example.unbox_common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:redis}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + redisHost + ":" + redisPort)
              // Increase timeout to 10 seconds (default is 3s)
              .setTimeout(10000)
              .setConnectTimeout(10000)
              // Keep connection alive
              .setPingConnectionInterval(30000)
              // Retry settings
              .setRetryAttempts(3);
        
        return Redisson.create(config);
    }
}
