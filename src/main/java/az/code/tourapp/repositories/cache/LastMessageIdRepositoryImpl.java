package az.code.tourapp.repositories.cache;

import az.code.tourapp.exceptions.OfferExpiredException;
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
public class LastMessageIdRepositoryImpl implements LastMessageIdRepository {

    private static final String KEY = "lastMessage";

    private final RedisTemplate<String, Map<String, Integer>> template;

    private HashOperations<String, String, Map<String, Integer>> hashOperations;

    public LastMessageIdRepositoryImpl(@Qualifier("lastMessageTemplate") RedisTemplate<String, Map<String, Integer>> template) {
        this.template = template;
    }

    @PostConstruct
    private void init() {
        hashOperations = template.opsForHash();
    }

    @Override
    public Integer findLastMessageId(String chatId, String uuid) {
        Map<String, Integer> userLastMessages = hashOperations.get(KEY, chatId);
        if (userLastMessages != null) {
            return userLastMessages.get(uuid);
        }
        throw new OfferExpiredException();
    }

    @Override
    public void saveLastMessageId(String chatId, String uuid, Integer messageId) {
        hashOperations.putIfAbsent(KEY, chatId, new HashMap<>());
        Map<String, Integer> userLastMessages = hashOperations.get(KEY, chatId);
        Objects.requireNonNull(userLastMessages).put(uuid, messageId);
        hashOperations.put(KEY, chatId, userLastMessages);
    }


    @Override
    public void deleteLastMessageId(String chatId, String uuid) {
        hashOperations.putIfAbsent(KEY, chatId, new HashMap<>());
        Map<String, Integer> userLastMessages = hashOperations.get(KEY, chatId);
        Objects.requireNonNull(userLastMessages).remove(uuid);
        hashOperations.put(KEY, chatId, userLastMessages);
    }

    @Override
    public boolean containsKey(String chatId, String uuid) {
        hashOperations.putIfAbsent(KEY, chatId, new HashMap<>());
        Map<String, Integer> userLastMessages = hashOperations.get(KEY, chatId);
        return Objects.requireNonNull(userLastMessages).containsKey(uuid);
    }

    @Override
    public void setExpire(Duration timeout) {
        template.expire(KEY, timeout);
    }
}
