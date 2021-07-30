package az.code.tourapp.models.entities;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "offers")
public class Offer {

    @EmbeddedId
    RequestId id;

    private String chatId;
    private String baseMessageId;
    private String messageId;
    private String photoUrl;

    @CreationTimestamp
    private LocalDateTime timeStamp;
}
