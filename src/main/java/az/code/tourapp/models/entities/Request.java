package az.code.tourapp.models.entities;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
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
    private Boolean status;
    private String data;
    @Column(name = "creation_time")
    private LocalDateTime creationTime;
}
