package az.code.tourapp.models;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.models.entities.Question;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisHash;

import java.util.HashMap;
import java.util.Map;

@RedisHash("data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserData {
    Locale userLang;
    Question currentQuestion;
    Map<Long, String> data = new HashMap<>();
}
