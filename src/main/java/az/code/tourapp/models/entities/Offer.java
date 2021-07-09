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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String chatId;
    String baseMessageId;
    String messageId;
    String uuid;
    String photoUrl;
    String agencyName;
    @CreationTimestamp
    LocalDateTime timeStamp;
}
