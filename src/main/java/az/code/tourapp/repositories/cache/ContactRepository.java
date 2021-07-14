package az.code.tourapp.repositories.cache;

import java.time.Duration;

public interface ContactRepository {

    Integer findMessageId(String chatId);

    void saveMessageId(String chatId, Integer messageId);

    void deleteMessageId(String chatId);

    boolean containsKey(String chatId);

    void setExpire(Duration timeout);
}
