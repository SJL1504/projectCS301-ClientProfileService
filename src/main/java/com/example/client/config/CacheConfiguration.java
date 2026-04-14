package com.example.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import lombok.extern.slf4j.Slf4j;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfiguration {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${cache.default.ttl:300}")
    private long defaultTtlSeconds;

    @Bean
    public ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Value("${spring.redis.cluster.nodes:}")
    private String clusterNodes;

    @Value("${spring.redis.ssl:true}")
    private boolean useSsl;

    // Redis Configuration (Production - ElastiCache)
    @Bean
    @Profile("!local")
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory;
        
        // Check if cluster mode is enabled (ElastiCache Cluster Mode Enabled)
        if (clusterNodes != null && !clusterNodes.isEmpty()) {
            // Cluster mode configuration
            log.info("Configuring Redis in cluster mode with nodes: {}", clusterNodes);
            RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();
            String[] nodes = clusterNodes.split(",");
            for (String node : nodes) {
                String[] parts = node.split(":");
                clusterConfig.clusterNode(parts[0], Integer.parseInt(parts[1]));
            }
            clusterConfig.setMaxRedirects(3);
            
            factory = new LettuceConnectionFactory(clusterConfig);
        } else {
            // Standalone mode configuration (ElastiCache Cluster Mode Disabled)
            log.info("Configuring Redis in standalone mode: {}:{}", redisHost, redisPort);
            factory = new LettuceConnectionFactory(redisHost, redisPort);
        }
        
        factory.setTimeout(2000); // 2-second timeout in milliseconds
        factory.setUseSsl(useSsl);
        return factory;
    }

    @Bean
    @Profile("!local")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper()));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper()));
        return template;
    }

    @Bean
    @Primary
    @Profile("!local")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTtlSeconds))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(cacheObjectMapper())))
                .disableCachingNullValues();

        // Custom TTL for different cache regions
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("clientAccounts", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("clientAccountsByClient", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    // Local Cache Configuration (Development)
    @Bean
    @Primary
    @Profile("local")
    public CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager("clientAccounts", "clientAccountsByClient");
    }
}