package az.code.tourapp.exceptions.user;

import az.code.tourapp.models.Translatable;
import lombok.Getter;

@Getter
@SuppressWarnings("ALL")
public class MultipleAcceptanceException extends RuntimeException implements Translatable {
    String text = "You already accepted one offer. You can only accept 1 offer.";
    String textAz = "Siz artıq bu sorğu üzrə təklif qəbul etmisiniz. Yalnız 1 təklif qəbul edilə bilər.";
    String textRu = "Вы уже приняли одно предложение. Вы можете принять только 1 предложение.";
}
