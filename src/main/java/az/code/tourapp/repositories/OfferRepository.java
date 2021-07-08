package az.code.tourapp.repositories;

import az.code.tourapp.models.entities.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findTop5ByChatIdOrderByTimeStampAsc(String chatId);

    Integer countAllByChatId(String chatId);
}
