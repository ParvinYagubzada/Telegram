package az.code.tourapp.repositories;

import az.code.tourapp.models.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {

    private static final String REDIS_ENTITY = "userdata";

    private final RedisTemplate<String, UserData> template;

    private HashOperations<String, String, UserData> hashOperations;

    @PostConstruct
    private void init() {
        hashOperations = template.opsForHash();
    }

    @Override
    public UserData findByChatId(String chatId) {
        return hashOperations.get(REDIS_ENTITY, chatId);
    }

    @Override
    public void deleteByChatId(String chatId) {
        hashOperations.delete(REDIS_ENTITY, chatId);
    }

    @Override
    public void updateByChatId(String chatId, UserData data) {
        hashOperations.put(REDIS_ENTITY, chatId, data);
    }

    @Override
    public void saveByChatId(String chatId, UserData data) {
        hashOperations.put(REDIS_ENTITY, chatId, data);
    }

    @Override
    public void setExpire(Duration timeout) {
        template.expire(REDIS_ENTITY, timeout);
    }
}
