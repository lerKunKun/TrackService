package com.logistics.track17.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisStateRepository {

    private static final String OAUTH_STATE_PREFIX = "oauth:state:";
    private static final long STATE_EXPIRE_SECONDS = 300; // 5 minutes

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String generateAndStoreState(String shopDomain) {
        String state = UUID.randomUUID().toString();
        String key = OAUTH_STATE_PREFIX + state;
        redisTemplate.opsForValue().set(key, shopDomain, STATE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return state;
    }

    public String getShopDomainForState(String state) {
        String key = OAUTH_STATE_PREFIX + state;
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void deleteState(String state) {
        String key = OAUTH_STATE_PREFIX + state;
        redisTemplate.delete(key);
    }
}
