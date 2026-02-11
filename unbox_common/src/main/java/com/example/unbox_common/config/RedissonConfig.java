package com.example.unbox_common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:redis}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.ssl:false}")
    private boolean redisSsl;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String protocol = redisSsl ? "rediss://" : "redis://";

        var serverConfig = config.useSingleServer()
              .setAddress(protocol + redisHost + ":" + redisPort)
              // Increase timeout to 10 seconds (default is 3s)
              .setTimeout(10000)
              .setConnectTimeout(10000)
              // Keep connection alive
              .setPingConnectionInterval(30000)
              // Retry settings
              .setRetryAttempts(3);

        if (StringUtils.hasText(redisPassword)) {
            serverConfig.setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}
