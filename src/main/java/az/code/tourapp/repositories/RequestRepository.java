package az.code.tourapp.repositories;

import az.code.tourapp.models.entities.Request;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, String> {

    Boolean existsAllByChatIdAndStatusIsTrue(String chatId);

    Optional<Request> findByUuidAndStatusIsTrue(String uuid);

    @Cacheable("request")
    Request findByUuid(String uuid);

    @Cacheable("request")
    @Query("SELECT request FROM Request request " +
            "WHERE request.chatId = :chatId " +
            "AND request.status = true ")
    Request findUuidByChatId(String chatId);

    @CacheEvict("request")
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value =
            "UPDATE requests SET status = FALSE " +
                    "WHERE chat_id = :chatId")
    void deactivate(String chatId);
}
