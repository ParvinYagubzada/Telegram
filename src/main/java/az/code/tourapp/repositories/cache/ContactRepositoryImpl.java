package az.code.tourapp.repositories.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

@Repository
public class ContactRepositoryImpl implements ContactRepository {

    private static final String KEY = "contactMessages";

    private final RedisTemplate<String, Integer> template;

    private HashOperations<String, String, Integer> hashOperations;

    public ContactRepositoryImpl(@Qualifier("contactMessageTemplate") RedisTemplate<String, Integer> template) {
        this.template = template;
    }

    @PostConstruct
    private void init() {
        hashOperations = template.opsForHash();
    }

    @Override
    public Integer findMessageId(String chatId) {
        return hashOperations.get(KEY, chatId);
    }

    @Override
    public void saveMessageId(String chatId, Integer messageId) {
        hashOperations.put(KEY, chatId, messageId);
    }

    @Override
    public void deleteMessageId(String chatId) {
        hashOperations.delete(KEY, chatId);
    }

    @Override
    public boolean containsKey(String chatId) {
        return hashOperations.hasKey(KEY, chatId);
    }
}
