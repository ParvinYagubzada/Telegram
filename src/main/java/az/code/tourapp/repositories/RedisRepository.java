package az.code.tourapp.repositories;

import az.code.tourapp.models.UserData;

import java.time.Duration;

public interface RedisRepository {

    UserData findByChatId(String chatId);

    void deleteByChatId(String chatId);

    void updateByChatId(String chatId, UserData data);

    void saveByChatId(String chatId, UserData data);

    void setExpire(Duration timeout);
}
