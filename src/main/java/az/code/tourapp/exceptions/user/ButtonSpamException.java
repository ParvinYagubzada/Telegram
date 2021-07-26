package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class ButtonSpamException extends RuntimeException implements Translatable {
    String text = "Please, don't spam bloody buttons. If you want to go fast just type it with XX.XX.XXXX format.";
    String textAz = "Xahiş olunur düymələri zorlamayasınız. " +
            "Əgər istəyirsinizsə kalendarı istifadə etməyib özünüz də XX.XX.XXXX formatında vaxtı yaza bilərsiniz.";
    String textRu = "Пожалуйста, не спамите кровавыми кнопками. Если вы хотите работать быстро, просто введите его в формате XX.XX.XXXX.";
}
