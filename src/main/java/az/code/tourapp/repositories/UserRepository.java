package az.code.tourapp.repositories;

import az.code.tourapp.models.entities.BotUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<BotUser, Long> {
}
