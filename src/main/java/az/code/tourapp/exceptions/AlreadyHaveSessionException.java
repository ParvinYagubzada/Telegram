package az.code.tourapp.exceptions;

import az.code.tourapp.enums.Locale;

@SuppressWarnings("ALL")
public class AlreadyHaveSessionException extends RuntimeException implements CustomException{

    //TODO: Add translations.
    String text = "You already have a open request. You can continue previous request or you can end it by writing \"stop\" command.";
    String text_az = "You already have a open request. You can continue previous request or you can end it by writing \"stop\" command.";
    String text_ru = "You already have a open request. You can continue previous request or you can end it by writing \"stop\" command.";

    @Override
    public String getText(Locale locale) {
        if (locale == null) return this.text;
        return switch (locale) {
            case EN -> this.text;
            case AZ -> this.text_az;
            case RU -> this.text_ru;
        };
    }
}
