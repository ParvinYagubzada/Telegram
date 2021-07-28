package az.code.tourapp.repositories.cache;

import az.code.tourapp.models.UserData;

public interface UserDataRepository {

    UserData findByChatId(String chatId);

    void deleteByChatId(String chatId);

    void saveByChatId(String chatId, UserData data);
}
