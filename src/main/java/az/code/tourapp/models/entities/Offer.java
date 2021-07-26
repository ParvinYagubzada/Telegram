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
    private Long id;
    private String chatId;
    private String baseMessageId;
    private String messageId;
    private String uuid;
    private String photoUrl;
    private String agencyName;
    @CreationTimestamp
    private LocalDateTime timeStamp;
}
