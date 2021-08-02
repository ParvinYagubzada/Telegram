package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class MissingUsernameException extends RuntimeException implements Translatable {
    String text = "You need to set a Telegram username in settings for us to contact you via Telegram.";
    String textAz = "Telegram vasitəsi ilə sizinə əlaqə saxlaya bilməyimiz üçün Telegram istifadəçi adınızı " +
            "tənzimləmələrdən təyin etməyiniz xahiş olunur.";
    String textRu = "Вам необходимо указать имя пользователя Telegram в настройках, чтобы мы могли связываться " +
            "с вами через Telegram.";
}
