package az.code.tourapp.models.entities;

import az.code.tourapp.enums.Locale;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "requests")
public class Request {
    @Id
    private String uuid;
    private String chatId;
    private String clientId;
    @Enumerated(EnumType.STRING)
    private Locale lang;
    private String data;
    private Boolean status;
    private LocalDateTime creationTime;
}
