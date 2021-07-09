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
    @Column(name = "chat_id")
    private String chatId;
    @Column(name = "client_id")
    private String clientId;
    @Enumerated(EnumType.STRING)
    private Locale lang;
    private String data;
    private Boolean status;
    @Column(name = "creation_time")
    private LocalDateTime creationTime;
}
