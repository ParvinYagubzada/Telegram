package az.code.tourapp.repositories;

import az.code.tourapp.models.entities.Action;
import az.code.tourapp.models.entities.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionRepository extends JpaRepository<Action, Long> {
    List<Action> findAllByBaseQuestionOrderById(Question question);
}
