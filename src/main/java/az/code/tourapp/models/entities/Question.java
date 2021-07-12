package az.code.tourapp.models.entities;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.exceptions.IllegalOptionException;
import az.code.tourapp.exceptions.InputMismatchException;
import lombok.*;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "questions")
public class Question implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757690L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String context;
    private String context_az;
    private String context_ru;
    private String fieldName;
    @OneToMany(mappedBy = "baseQuestion", fetch = FetchType.EAGER)
    private List<Action> actions;

    public Question findNext(String actionText, Locale locale) {
        if (this.actions.size() != 1) {
            Optional<Action> find = this.actions.stream()
                    .filter(action -> action.getText(locale).equals(actionText))
                    .findFirst();
            if (find.isPresent())
                return find.get().getNextQuestion();
            throw new IllegalOptionException();
        } else {
            String regex = this.actions.get(0).getText();
            Pattern pattern = Pattern.compile(regex);
            if (pattern.matcher(actionText).matches())
                return this.actions.get(0).getNextQuestion();
            throw new InputMismatchException();
        }
    }

    public String getText(Locale locale) {
        if (locale == null) return this.context_az;
        return switch (locale) {
            case EN -> this.context;
            case AZ -> this.context_az;
            case RU -> this.context_ru;
        };
    }

    @Override
    public String toString() {
        return fieldName;
    }
}
