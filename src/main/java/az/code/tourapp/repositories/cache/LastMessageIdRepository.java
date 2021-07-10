package az.code.tourapp.repositories.cache;

import java.time.Duration;

public interface LastMessageIdRepository {

    Integer findLastMessageId(String chatId, String uuid);

    void saveLastMessageId(String chatId, String uuid, Integer messageId);

    void deleteLastMessageId(String chatId, String uuid);

    boolean containsKey(String chatId, String uuid);

    void setExpire(Duration timeout);
}
