package az.code.tourapp.exceptions;

import az.code.tourapp.enums.Locale;

@SuppressWarnings("ALL")
public class NoSuchSessionException extends RuntimeException implements CustomException {

    String text = "You don't have a open request. You can start a new request by writing \"start\" command.";
    String text_az = "Sizin aktiv sorğunuz yoxdur. Yeni sorğu yaratmaq üçün \"start\" əmrindən istifadə edə bilərsiniz.";
    String text_ru = "У вас нет открытого запроса. Вы можете начать новый запрос, написав \"start\" команду.";

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
