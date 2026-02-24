package com.example.unbox_common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ConstantDelay;
import org.redisson.config.SslVerificationMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:redis}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl:false}")
    private boolean redisSsl;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        String protocol = redisSsl ? "rediss://" : "redis://";
        String address = protocol + redisHost + ":" + redisPort;

        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        if (redisSsl) {
            // AWS ElastiCache용: endpoint verification 비활성화
            config.setSslVerificationMode(SslVerificationMode.NONE);
        }

        config.useSingleServer()
              .setAddress(address)
              // Increase timeout to 30 seconds for cold start
              .setTimeout(30000)
              .setConnectTimeout(30000)
              // Keep connection alive
              .setPingConnectionInterval(30000)
              // Retry settings
              .setRetryAttempts(5)
              .setRetryDelay(new ConstantDelay(Duration.ofMillis(3000)));
        
        return Redisson.create(config);
    }
}
