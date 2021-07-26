package az.code.tourapp.models.dto;

import az.code.tourapp.models.entities.BotUser;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.meta.api.objects.Contact;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AcceptedOffer {

    String uuid;
    String agencyName;
    String username;
    String phoneNumber;
    String firstName;
    String lastName;
    String userId;

    public AcceptedOffer(String uuid, String agencyName, String username, Contact contact) {
        this.uuid = uuid;
        this.agencyName = agencyName;
        this.username = username;
        this.firstName = contact.getFirstName();
        this.lastName = contact.getLastName();
        this.userId = contact.getUserId().toString();
        this.phoneNumber = contact.getPhoneNumber() != null ? contact.getPhoneNumber() : null;
    }

    public AcceptedOffer(String uuid, String agencyName, BotUser user) {
        this.uuid = uuid;
        this.agencyName = agencyName;
        this.username = user.getUserName();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.userId = user.getUserId().toString();
        this.phoneNumber = user.getPhoneNumber();
    }
}
