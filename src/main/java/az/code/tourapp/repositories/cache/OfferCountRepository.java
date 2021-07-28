package az.code.tourapp.repositories.cache;

public interface OfferCountRepository {

    Integer findOfferCount(String chatId, String uuid);

    Integer incrementOfferCount(String chatId, String uuid);

    void saveOfferCount(String chatId, String uuid, Integer offerCount);

    void deleteOfferCount(String chatId, String uuid);

    boolean containsKey(String chatId, String uuid);
}
