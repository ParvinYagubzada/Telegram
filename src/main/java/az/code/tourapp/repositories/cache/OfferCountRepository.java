package az.code.tourapp.repositories.cache;

public interface OfferCountRepository {

    Integer findOfferCount(String chatId);

    void incrementOfferCount(String chatId);

    void deleteOfferCount(String chatId);

    boolean containsKey(String chatId);
}
