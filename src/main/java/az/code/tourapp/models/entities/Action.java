package az.code.tourapp.models.entities;

import az.code.tourapp.enums.ActionType;
import az.code.tourapp.enums.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "actions")
public class Action {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String text;
    private String text_az;
    private String text_ru;
    @Enumerated(EnumType.STRING)
    private ActionType type;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "base_id", referencedColumnName = "id")
    private Question baseQuestion;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "next_id", referencedColumnName = "id")
    private Question nextQuestion;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Action action = (Action) o;
        return Objects.equals(text, action.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    public static Action of(String text) {
        Action action = new Action();
        action.setText(text);
        return action;
    }

    public String getText(Locale locale) {
        if (locale == null) return this.text_az;
        return switch (locale) {
            case EN -> this.text;
            case AZ -> this.text_az;
            case RU -> this.text_ru;
        };
    }
}
