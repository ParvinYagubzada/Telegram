package az.code.tourapp.models.entities;

import az.code.tourapp.enums.ActionType;
import az.code.tourapp.models.Translatable;
import lombok.*;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "actions")
public class Action implements Translatable, Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757691L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String text;
    private String textAz;
    private String textRu;
    private String fieldName;

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
}
