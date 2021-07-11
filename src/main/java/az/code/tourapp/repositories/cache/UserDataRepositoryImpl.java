package az.code.tourapp.repositories.cache;

import az.code.tourapp.models.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class UserDataRepositoryImpl implements UserDataRepository {

    private static final String KEY = "userdata";

    private final RedisTemplate<String, UserData> template;

    private HashOperations<String, String, UserData> hashOperations;

    @PostConstruct
    private void init() {
        hashOperations = template.opsForHash();
    }

    @Override
    public UserData findByChatId(String chatId) {
        return hashOperations.get(KEY, chatId);
    }

    @Override
    public void deleteByChatId(String chatId) {
        hashOperations.delete(KEY, chatId);
    }

    @Override
    public void saveByChatId(String chatId, UserData data) {
        hashOperations.put(KEY, chatId, data);
    }

    @Override
    public void setExpire(Duration timeout) {
        template.expire(KEY, timeout);
    }
}
