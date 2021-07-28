package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class RequestExpiredException extends RuntimeException implements Translatable {
    String text = "Your request expired. You cannot accept any offers. You can start a new request by writing /start command.";
    String textAz = "Sizin sorğunuzun vaxtı bitmişdir. Heç bir təklifi qəbul edə bilərsiniz. Yeni sorğu yaratmaq üçün /start əmrindən istifadə edə bilərsiniz.";
    String textRu = "Срок действия вашего запроса истек. Вы не можете принимать какие-либо предложения. Вы можете начать новый запрос, написав /start команду.";
}
