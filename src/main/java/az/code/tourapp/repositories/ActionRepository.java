package az.code.tourapp.repositories;

import az.code.tourapp.models.entities.Action;
import az.code.tourapp.models.entities.Question;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionRepository extends JpaRepository<Action, Long> {
    @Cacheable("questionActions")
    List<Action> findAllByBaseQuestionOrderById(Question question);
}
