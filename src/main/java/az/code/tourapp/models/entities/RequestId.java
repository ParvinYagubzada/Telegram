package az.code.tourapp.models.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Embeddable
public class RequestId implements Serializable {
    protected String agencyName;
    protected String uuid;
}