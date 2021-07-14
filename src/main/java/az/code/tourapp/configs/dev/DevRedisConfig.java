package az.code.tourapp.configs.dev;

import az.code.tourapp.models.UserData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@SuppressWarnings("unchecked")
@Profile("dev")
@Configuration
@EnableRedisRepositories
public class DevRedisConfig {

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName("localhost");
        configuration.setPort(6379);
        return new JedisConnectionFactory();
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(14))
                .disableCachingNullValues();
    }

    @Bean
    public RedisTemplate<String, UserData> redisTemplate(JedisConnectionFactory factory) {
        RedisTemplate<String, UserData> template = new RedisTemplate<>();
        return configRedisTemplate(factory, template);
    }

    @Bean
    public RedisTemplate<String, Map<String, Integer>> userOfferTemplate(JedisConnectionFactory factory) {
        RedisTemplate<String, Map<String, Integer>> template = new RedisTemplate<>();
        return configRedisTemplate(factory, template);
    }

    @Bean
    public RedisTemplate<String, Map<String, Integer>> lastMessageTemplate(JedisConnectionFactory factory) {
        RedisTemplate<String, Map<String, Integer>> template = new RedisTemplate<>();
        return configRedisTemplate(factory, template);
    }

    @Bean
    public RedisTemplate<String, Integer> contactMessageTemplate(JedisConnectionFactory factory) {
        RedisTemplate<String, Map<String, Integer>> template = new RedisTemplate<>();
        return configRedisTemplate(factory, template);
    }

    @SuppressWarnings({"DuplicatedCode", "rawtypes"})
    private RedisTemplate configRedisTemplate(JedisConnectionFactory factory, RedisTemplate template) {
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new JdkSerializationRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        return template;
    }
}
