package az.code.tourapp.repositories;

import az.code.tourapp.models.entities.Request;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

@SuppressWarnings("NullableProblems")
public interface RequestRepository extends JpaRepository<Request, String> {

    boolean existsByChatIdAndActiveIsTrue(String chatId);

    Request findByUuidAndActiveIsTrue(String uuid);

    Request findByChatIdAndActiveIsTrue(String chatId);

    @Cacheable(value = "request")
    Request findByUuid(String uuid);

    @CacheEvict("request")
    @Override
    <S extends Request> S save(@NonNull S s);
}
