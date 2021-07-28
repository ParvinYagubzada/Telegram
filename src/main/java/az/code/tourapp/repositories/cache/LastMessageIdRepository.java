package az.code.tourapp.repositories.cache;

public interface LastMessageIdRepository {

    Integer findLastMessageId(String chatId);

    void saveLastMessageId(String chatId, Integer messageId);

    void deleteLastMessageId(String chatId);

    boolean containsKey(String chatId);
}
