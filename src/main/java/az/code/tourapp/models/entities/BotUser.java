package az.code.tourapp.models.entities;

import lombok.*;
import org.telegram.telegrambots.meta.api.objects.Contact;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class BotUser {

    @Id
    private Long userId;
    private String userName;
    private String phoneNumber;
    private String firstName;
    private String lastName;

    public BotUser(String userName, Contact contact) {
        this.userName = userName;
        this.firstName = contact.getFirstName();
        this.lastName = contact.getLastName();
        this.userId = contact.getUserId();
        this.phoneNumber = contact.getPhoneNumber();
    }
}
