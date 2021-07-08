package az.code.tourapp.repositories;

import az.code.tourapp.models.entities.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface RequestRepository extends JpaRepository<Request, String> {

    Boolean existsAllByChatIdAndStatusIsTrue(String chatId);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value =
            "UPDATE requests SET status = FALSE " +
            "WHERE chat_id = :chatId")
    void deactivate(String chatId);
}
