package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class IllegalOptionException extends RuntimeException implements Translatable {
    String text = "You can not select or type option other than provided.";
    String textAz = "Verilən seçimlərdən kənar seçim seçmək olmaz.";
    String textRu = "Вы не можете выбрать или ввести другой вариант, кроме предоставленного.";
}
