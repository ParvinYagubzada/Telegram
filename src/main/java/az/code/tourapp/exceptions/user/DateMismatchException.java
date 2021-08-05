package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class DateMismatchException extends RuntimeException implements Translatable {
    String text = "You must select a date in range of %s to %s.";
    String textAz = "%s ilə %s aralığında bir gün seçməlisiniz.";
    String textRu = "Вы должны выбрать дату в диапазоне от %s до %s.";

    public DateMismatchException(String startDate, String endDate) {
        text = String.format(text, startDate, endDate);
        textAz = String.format(textAz, startDate, endDate);
        textRu = String.format(textRu, startDate, endDate);
    }
}
