package az.code.tourapp.models.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class BotUser {

    @Id
    private Long userId;
    private String username;
    private String phoneNumber;
    private String firstName;
    private String lastName;
}
