package az.code.tourapp.exceptions;

import az.code.tourapp.enums.Locale;

@SuppressWarnings("ALL")
public class NoSuchSessionException extends RuntimeException implements CustomException{

    //TODO: Add translations.
    String text = "You don't have a open request. You can start a new request by writing \"start\" command.";
    String text_az = "You don't have a open request. You can start a new request by writing \"start\" command.";
    String text_ru = "You don't have a open request. You can start a new request by writing \"start\" command.";

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
