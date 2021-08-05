package az.code.tourapp.models;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.models.entities.Question;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@RedisHash("userdata")
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserData implements Serializable {
    Locale userLang;
    Question currentQuestion;
    Map<String, String> data;
}
