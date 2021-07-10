package az.code.tourapp.exceptions;

import az.code.tourapp.enums.Locale;

@SuppressWarnings("ALL")
public class OfferExpiredException extends RuntimeException implements CustomException {

    String text = "This offer expired already. Offers is valid only for 2 weeks.";
    String text_az = "Bu təklifin vaxtı bitmişdir. Təkliflər yalnız 2 həftə aktiv olur.";
    String text_ru = "Срок действия этого предложения уже истек. Предложение действительно только 2 недели.";

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
