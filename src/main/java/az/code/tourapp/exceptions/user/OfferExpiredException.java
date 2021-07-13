package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class OfferExpiredException extends RuntimeException implements Translatable {
    String text = "This offer expired already. Offers is valid only for 2 weeks.";
    String textAz = "Bu təklifin vaxtı bitmişdir. Təkliflər yalnız 2 həftə aktiv olur.";
    String textRu = "Срок действия этого предложения уже истек. Предложение действительно только 2 недели.";
}
