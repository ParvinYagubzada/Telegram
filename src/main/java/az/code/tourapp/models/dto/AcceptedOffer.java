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
    String phoneNumber;
    String fistName;
    String lastName;
    String userId;

    public AcceptedOffer(String uuid, String agencyName, Contact contact) {
        this.uuid = uuid;
        this.agencyName = agencyName;
        this.phoneNumber = contact.getPhoneNumber();
        this.fistName = contact.getFirstName();
        this.lastName = contact.getLastName();
        this.userId = contact.getUserId().toString();
    }
}
