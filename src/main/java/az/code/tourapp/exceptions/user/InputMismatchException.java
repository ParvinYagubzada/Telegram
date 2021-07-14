package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class InputMismatchException extends RuntimeException implements Translatable {
    String text = "You answer does not meet typing requirements.";
    String textAz = "Sizin cavabınızın yazılış forması düzgün deyil.";
    String textRu = "Ваш ответ не соответствует требованиям к вводу текста.";
}
