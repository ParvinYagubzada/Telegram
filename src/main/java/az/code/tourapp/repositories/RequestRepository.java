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

    Boolean existsAllByChatIdAndActiveIsTrue(String chatId);

    Optional<Request> findByUuidAndActiveIsTrue(String uuid);

    @Cacheable("request")
    Request findByUuid(String uuid);

    @Query("SELECT request FROM Request request " +
           "WHERE request.chatId = :chatId " +
           "AND request.active = true ")
    Request findUuidByChatId(String chatId);

    @CacheEvict("request")
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value =
            "UPDATE requests " +
            "SET active = FALSE " +
            "WHERE chat_id = :chatId")
    void deactivate(String chatId);

    @CacheEvict("request")
    @Override
    <S extends Request> S save(S s);
}
