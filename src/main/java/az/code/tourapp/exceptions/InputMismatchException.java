package az.code.tourapp.exceptions;

import az.code.tourapp.enums.Locale;

@SuppressWarnings("ALL")
public class InputMismatchException extends RuntimeException implements CustomException{

    //TODO: Add translations.
    String text = "You answer does not meet typing requirements.";
    String text_az = "Sizin cavabınızın yazılış forması düzgün deyil.";
    String text_ru = "nüll";

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
