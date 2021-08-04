package az.code.tourapp.repositories;

import az.code.tourapp.models.entities.Offer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Integer countAllByChatIdAndId_UuidAndBaseMessageIdIsNull(String chatId, String uuid);

    List<Offer> findAllByChatIdAndId_UuidAndBaseMessageIdIsNull(String chatId, String uuid);

    @Query("SELECT offer FROM Offer offer " +
            "WHERE offer.chatId = :chatId " +
            "AND offer.id.uuid = :uuid " +
            "AND offer.baseMessageId IS null " +
            "ORDER BY offer.timeStamp ASC ")
    List<Offer> findTop5(String chatId, String uuid, Pageable pageable);

    @Query("SELECT offer FROM Offer offer " +
            "WHERE offer.chatId = :chatId " +
            "AND (offer.baseMessageId = :messageId OR offer.messageId = :messageId)")
    Offer getByMessageId(String chatId, String messageId);

    boolean existsById_Uuid(String uuid);
}
