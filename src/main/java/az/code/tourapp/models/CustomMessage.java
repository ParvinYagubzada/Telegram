package az.code.tourapp.models;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomMessage implements Translatable {
    private String text;
    private String textAz;
    private String textRu;
}
