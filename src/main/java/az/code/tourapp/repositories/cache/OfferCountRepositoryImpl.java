package az.code.tourapp.repositories.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

@Repository
public class OfferCountRepositoryImpl implements OfferCountRepository {

    private static final String KEY = "offerCount";

    private final RedisTemplate<String, Integer> template;

    private HashOperations<String, String, Integer> hashOperations;

    public OfferCountRepositoryImpl(@Qualifier("userOfferTemplate") RedisTemplate<String, Integer> template) {
        this.template = template;
    }

    @PostConstruct
    private void init() {
        hashOperations = template.opsForHash();
    }

    @Override
    public Integer findOfferCount(String chatId) {
        return hashOperations.get(KEY, chatId);
    }

    @Override
    public void incrementOfferCount(String chatId) {
        Integer current = hashOperations.get(KEY, chatId);
        int value = 1;
        if (current != null) {
            value = current + 1;
        }
        hashOperations.put(KEY, chatId, value);
    }

    @Override
    public void deleteOfferCount(String chatId) {
        hashOperations.delete(KEY, chatId);
    }

    @Override
    public boolean containsKey(String chatId) {
        return hashOperations.hasKey(KEY, chatId);
    }
}
