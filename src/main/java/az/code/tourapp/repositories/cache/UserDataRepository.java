package az.code.tourapp.repositories.cache;

import az.code.tourapp.models.UserData;

import java.time.Duration;

public interface UserDataRepository {

    UserData findByChatId(String chatId);

    void deleteByChatId(String chatId);

    void saveByChatId(String chatId, UserData data);

    void setExpire(Duration timeout);
}
