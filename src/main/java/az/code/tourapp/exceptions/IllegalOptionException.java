package az.code.tourapp.exceptions;

import az.code.tourapp.enums.Locale;

@SuppressWarnings("ALL")
public class IllegalOptionException extends RuntimeException implements CustomException {

    //TODO: Add translations.
    String text = "You can not select or type option other than provided.";
    String text_az = "Verilən seçimlərdən kənar seçim seçmək olmaz.";
    String text_ru = "Вы не можете выбрать или ввести другой вариант, кроме предоставленного.";

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
