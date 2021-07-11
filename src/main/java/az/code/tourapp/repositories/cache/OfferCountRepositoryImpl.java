package az.code.tourapp.repositories.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Repository
public class OfferCountRepositoryImpl implements OfferCountRepository {

    private static final String KEY = "offerCount";

    private final RedisTemplate<String, Map<String, Integer>> template;

    private HashOperations<String, String, Map<String, Integer>> hashOperations;

    public OfferCountRepositoryImpl(@Qualifier("userOfferTemplate") RedisTemplate<String, Map<String, Integer>> template) {
        this.template = template;
    }

    @PostConstruct
    private void init() {
        hashOperations = template.opsForHash();
    }

    @Override
    public Integer findOfferCount(String chatId, String uuid) {
        Map<String, Integer> userOfferCounts = hashOperations.get(KEY, chatId);
        if (userOfferCounts != null) {
            return userOfferCounts.get(uuid);
        }
        return null;
    }

    @Override
    public Integer incrementOfferCount(String chatId, String uuid) {
        Map<String, Integer> userOfferCounts = hashOperations.get(KEY, chatId);
        if (userOfferCounts != null) {
            Integer current = userOfferCounts.get(uuid);
            if (current != null) {
                current++;
                userOfferCounts.put(uuid, current);
                hashOperations.put(KEY, chatId, userOfferCounts);
            }
            return current;
        }
        return null;
    }

    @Override
    public void saveOfferCount(String chatId, String uuid, Integer offerCount) {
        hashOperations.putIfAbsent(KEY, chatId, new HashMap<>());
        Map<String, Integer> userOfferCounts = hashOperations.get(KEY, chatId);
        Objects.requireNonNull(userOfferCounts).put(uuid, offerCount);
        hashOperations.put(KEY, chatId, userOfferCounts);
    }

    @Override
    public void deleteOfferCount(String chatId, String uuid) {
        hashOperations.putIfAbsent(KEY, chatId, new HashMap<>());
        Map<String, Integer> userLastMessages = hashOperations.get(KEY, chatId);
        Objects.requireNonNull(userLastMessages).remove(uuid);
        hashOperations.put(KEY, chatId, userLastMessages);
    }

    @Override
    public boolean containsKey(String chatId, String uuid) {
        Map<String, Integer> userOfferCounts = hashOperations.get(KEY, chatId);
        if (userOfferCounts != null) {
            return userOfferCounts.containsKey(uuid);
        }
        hashOperations.put(KEY, chatId, new HashMap<>());
        return false;
    }

    @Override
    public void setExpire(Duration timeout) {
        template.expire(KEY, timeout);
    }
}
