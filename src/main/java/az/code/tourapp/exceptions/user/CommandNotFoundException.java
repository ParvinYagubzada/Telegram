package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class CommandNotFoundException extends RuntimeException implements Translatable {
    String text = "This command does not exists.";
    String textAz = "Bu əmr mövcud deyil.";
    String textRu = "Эта команда не существует.";
}
