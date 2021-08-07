package az.code.tourapp.models.entities;

import az.code.tourapp.enums.ActionType;
import az.code.tourapp.exceptions.user.IllegalOptionException;
import az.code.tourapp.exceptions.user.InputMismatchException;
import az.code.tourapp.helpers.BotHelper;
import az.code.tourapp.models.Translatable;
import az.code.tourapp.models.UserData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static az.code.tourapp.helpers.BotHelper.handleDateType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "questions")
public class Question implements Translatable, Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757690L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String text;
    private String textAz;
    private String textRu;
    private String fieldName;
    @OneToMany(mappedBy = "baseQuestion", fetch = FetchType.EAGER)
    private List<Action> actions;

    public Action findNext(String actionText, UserData data) {
        if (this.actions.size() != 1) {
            Optional<Action> find = this.actions.stream()
                    .filter(action -> BotHelper.getText(action, data.userLang()).equals(actionText))
                    .findFirst();
            return find.orElseThrow(IllegalOptionException::new);
        } else {
            Action action = this.actions.get(0);
            if (action.getType() == ActionType.FREETEXT) {
                Pattern pattern = Pattern.compile(action.getText());
                if (pattern.matcher(actionText).matches())
                    return action;
                throw new InputMismatchException();
            } else {
                return handleDateType(actionText, data.data(), action);
            }
        }
    }

    @Override
    public String toString() {
        return String.join(":", fieldName, id.toString());
    }
}
