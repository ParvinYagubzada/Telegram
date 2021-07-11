package az.code.tourapp.models;

import az.code.tourapp.enums.Locale;
import lombok.*;

@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomMessage {

    private String context;
    private String context_az;
    private String context_ru;

    public String getText(Locale locale) {
        if (locale == null) return this.context_az;
        return switch (locale) {
            case EN -> this.context;
            case AZ -> this.context_az;
            case RU -> this.context_ru;
        };
    }
}
