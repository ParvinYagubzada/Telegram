package az.code.tourapp.repositories.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

@Repository
public class LastMessageIdRepositoryImpl implements LastMessageIdRepository {

    private static final String KEY = "lastMessage";

    private final RedisTemplate<String, Integer> template;

    private HashOperations<String, String, Integer> hashOperations;

    public LastMessageIdRepositoryImpl(@Qualifier("lastMessageTemplate") RedisTemplate<String, Integer> template) {
        this.template = template;
    }

    @PostConstruct
    private void init() {
        hashOperations = template.opsForHash();
    }

    @Override
    public Integer findLastMessageId(String chatId) {
        return hashOperations.get(KEY, chatId);
    }

    @Override
    public void saveLastMessageId(String chatId, Integer messageId) {
        hashOperations.put(KEY, chatId, messageId);
    }

    @Override
    public void deleteLastMessageId(String chatId) {
        hashOperations.delete(KEY, chatId);
    }

    @Override
    public boolean containsKey(String chatId) {
        return hashOperations.hasKey(KEY, chatId);
    }
}
