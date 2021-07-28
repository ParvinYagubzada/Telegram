package az.code.tourapp.configs;

import az.code.tourapp.models.UserData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@SuppressWarnings("unchecked")
@Configuration
@EnableRedisRepositories
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration(@Value("${telegram.bot.expiration-days}") Integer days) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(days + 1));
    }

    @Bean
    public RedisTemplate<String, UserData> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, UserData> template = new RedisTemplate<>();
        return configRedisTemplate(factory, template);
    }

    @Bean
    public RedisTemplate<String, Map<String, Integer>> userOfferTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Map<String, Integer>> template = new RedisTemplate<>();
        return configRedisTemplate(factory, template);
    }

    @Bean
    public RedisTemplate<String, Map<String, Integer>> lastMessageTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Map<String, Integer>> template = new RedisTemplate<>();
        return configRedisTemplate(factory, template);
    }

    @Bean
    public RedisTemplate<String, Integer> contactMessageTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Map<String, Integer>> template = new RedisTemplate<>();
        return configRedisTemplate(factory, template);
    }

    @SuppressWarnings({"DuplicatedCode", "rawtypes"})
    private RedisTemplate configRedisTemplate(LettuceConnectionFactory factory, RedisTemplate template) {
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
