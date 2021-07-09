package az.code.tourapp.exceptions;

import az.code.tourapp.enums.Locale;

@SuppressWarnings("ALL")
public class AlreadyHaveSessionException extends RuntimeException implements CustomException {

    String text = "You already have a open request. You can continue previous request or you can end it by writing \"stop\" command.";
    String text_az = "Sizin aktiv sorğunuz var. Aktiv sorğuya davam edə və ya \"stop\" əmrindən istifadə edib dayandıra bilərsiniz.";
    String text_ru = "У вас уже есть открытый запрос. Вы можете продолжить предыдущий запрос или вы можете закончить это, написав команду \"stop\".";

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
