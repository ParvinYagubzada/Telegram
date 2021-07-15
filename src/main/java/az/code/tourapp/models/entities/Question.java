package az.code.tourapp.models.entities;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.exceptions.user.IllegalOptionException;
import az.code.tourapp.exceptions.user.InputMismatchException;
import az.code.tourapp.helpers.BotHelper;
import az.code.tourapp.models.Translatable;
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

    public Action findNext(String actionText, Locale locale) {
        if (this.actions.size() != 1) {
            Optional<Action> find = this.actions.stream()
                    .filter(action -> {
                        System.out.println(BotHelper.getText(action, locale));
                        System.out.println(actionText);
                        return BotHelper.getText(action, locale).equals(actionText);
                    })
                    .findFirst();
            if (find.isPresent())
                return find.get();
            throw new IllegalOptionException();
        } else {
            String regex = this.actions.get(0).getText();
            Pattern pattern = Pattern.compile(regex);
            if (pattern.matcher(actionText).matches())
                return this.actions.get(0);
            throw new InputMismatchException();
        }
    }

    @Override
    public String toString() {
        return String.join(":", fieldName, id.toString());
    }
}
