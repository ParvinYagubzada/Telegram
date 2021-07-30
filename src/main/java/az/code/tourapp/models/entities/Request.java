package az.code.tourapp.models.entities;

import az.code.tourapp.enums.Locale;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "requests")
public class Request implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757692L;

    @Id
    private String uuid;
    private String chatId;
    private String clientId;
    @Enumerated(EnumType.STRING)
    private Locale lang;
    private String data;
    private boolean active;
    private boolean accepted;
    @CreationTimestamp
    private LocalDateTime creationTime;
    private LocalDateTime expirationTime;

    @Override
    public String toString() {
        return uuid;
    }
}
