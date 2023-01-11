package com.angeltear.microthrottler.config;


import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    private final RedisClient redisClient;

    @Autowired
    public RedisConfig(@Value("${redis.url}") String redisUrl) {
        this.redisClient = RedisClient.create(redisUrl);
    }

    public RedisClient getClient() {
        return redisClient;
    }
}
