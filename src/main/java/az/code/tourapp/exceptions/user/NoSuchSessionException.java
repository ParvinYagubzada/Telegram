package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class NoSuchSessionException extends RuntimeException implements Translatable {
    String text = "You don't have a open request. You can start a new request by writing \"start\" command.";
    String textAz = "Sizin aktiv sorğunuz yoxdur. Yeni sorğu yaratmaq üçün \"start\" əmrindən istifadə edə bilərsiniz.";
    String textRu = "У вас нет открытого запроса. Вы можете начать новый запрос, написав \"start\" команду.";
}
