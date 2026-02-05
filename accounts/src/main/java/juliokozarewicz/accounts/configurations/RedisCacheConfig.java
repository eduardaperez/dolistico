package juliokozarewicz.accounts.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {

        // 1. Create and configure ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        // Enable polymorphic type handling for proper serialization/deserialization
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        // Register Java 8 Time module for proper date/time handling
        objectMapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps (use ISO-8601 format instead)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 2. Create JSON serializer using the configured ObjectMapper
        RedisSerializationContext.SerializationPair<Object> serializationPair =
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(objectMapper)
            );

        // Default cache configuration (5 minutes TTL)
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5)) // Set default expiration time
            .disableCachingNullValues()      // Don't cache null values
            .serializeValuesWith(serializationPair); // Use JSON serialization

        // Profile cache configuration
        RedisCacheConfiguration profileCacheConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofDays(180)) // Set default expiration time
            .disableCachingNullValues()
            .serializeValuesWith(serializationPair);

        // Address cache configuration
        RedisCacheConfiguration addressCacheConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofDays(180)) // Set default expiration time
            .disableCachingNullValues()
            .serializeValuesWith(serializationPair);

        // Array login tokens cache configuration
        RedisCacheConfiguration ArrayLoginsCacheConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .disableCachingNullValues()
            .serializeValuesWith(serializationPair);

        // Refresh login verification cache configuration
        RedisCacheConfiguration refreshLoginCacheConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofDays(15))
            .disableCachingNullValues()
            .serializeValuesWith(serializationPair);

        // Pin verification cache configuration
        RedisCacheConfiguration pinVerificationCacheConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(serializationPair);

        // Verification Token cache configuration
        RedisCacheConfiguration verificationCacheConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(serializationPair);

        // Login Token cache configuration
        RedisCacheConfiguration loginCacheConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeValuesWith(serializationPair);

        // Not activated account cache configuration
        RedisCacheConfiguration notActivatedAccountConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofDays(3))
            .disableCachingNullValues()
            .serializeValuesWith(serializationPair);

        // Deleted account cache configuration
        RedisCacheConfiguration deletedAccountByUserConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofDays(32))
            .disableCachingNullValues()
            .serializeValuesWith(serializationPair);

        // Create cache configurations map for specific caches
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("accounts-profileCache", profileCacheConfig);
        cacheConfigs.put("accounts-addressCache", addressCacheConfig);
        cacheConfigs.put("accounts-refreshLoginCache", refreshLoginCacheConfig);
        cacheConfigs.put("accounts-pinVerificationCache", pinVerificationCacheConfig);
        cacheConfigs.put("accounts-arrayLoginsCache", ArrayLoginsCacheConfig);
        cacheConfigs.put("accounts-verificationCache", verificationCacheConfig);
        cacheConfigs.put("accounts-loginCache", loginCacheConfig);
        cacheConfigs.put("accounts-notActivatedAccountCache", notActivatedAccountConfig);
        cacheConfigs.put("accounts-deletedAccountByUserCache", deletedAccountByUserConfig);

        // Build and return the CacheManager instance
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfig) // Default configuration
            .withInitialCacheConfigurations(cacheConfigs) // Custom cache configurations
            .build();

    }

}