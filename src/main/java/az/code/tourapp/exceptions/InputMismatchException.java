package az.code.tourapp.exceptions;

import az.code.tourapp.enums.Locale;

public class InputMismatchException extends RuntimeException implements CustomException{

    String text = "You can not select or type option other than provided.";
    String text_az = "Verilən seçimlərdən kənar seçim seçmək və ya yazmaq olmaz.";
    String text_ru = "nüll"; //TODO: Russian

    public InputMismatchException() {
        super("You can not select or type option other than provided.");
    }

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
