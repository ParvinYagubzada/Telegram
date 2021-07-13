package az.code.tourapp.models.dto;

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
    String userName;
    String phoneNumber;
    String firstName;
    String lastName;
    String userId;

    public AcceptedOffer(String uuid, String agencyName, String userName, Contact contact) {
        this.uuid = uuid;
        this.agencyName = agencyName;
        this.userName = userName;
        this.firstName = contact.getFirstName();
        this.lastName = contact.getLastName();
        this.userId = contact.getUserId().toString();
        this.phoneNumber = contact.getPhoneNumber() != null ? contact.getPhoneNumber() : null;
    }
}
