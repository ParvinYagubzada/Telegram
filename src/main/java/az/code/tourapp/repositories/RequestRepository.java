package az.code.tourapp.repositories;

import az.code.tourapp.models.entities.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, String> {

    Boolean existsAllByChatIdAndStatusIsTrue(String chatId);

    Optional<Request> findByUuidAndStatusIsTrue(String uuid);

    Request findByUuid(String uuid);

    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    @Query("SELECT request.uuid FROM Request request " +
            "WHERE request.chatId = :chatId " +
            "AND request.status = true ")
    String findUuidByChatId(String chatId);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value =
            "UPDATE requests SET status = FALSE " +
            "WHERE chat_id = :chatId")
    void deactivate(String chatId);
}
