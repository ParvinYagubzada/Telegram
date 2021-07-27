package az.code.tourapp.models.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AcceptedOffer {

    String uuid;
    String agencyName;
    String username;
    String phoneNumber;
    String firstName;
    String lastName;
    String userId;
}
