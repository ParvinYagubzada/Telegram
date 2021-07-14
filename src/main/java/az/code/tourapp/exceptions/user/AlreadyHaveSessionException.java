package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class AlreadyHaveSessionException extends RuntimeException implements Translatable {
    String text = "You already have a open request. You can continue previous request or you can end it by writing \"stop\" command.";
    String textAz = "Sizin aktiv sorğunuz var. Aktiv sorğuya davam edə və ya \"stop\" əmrindən istifadə edib dayandıra bilərsiniz.";
    String textRu = "У вас уже есть открытый запрос. Вы можете продолжить предыдущий запрос или вы можете закончить это, написав команду \"stop\".";
}
