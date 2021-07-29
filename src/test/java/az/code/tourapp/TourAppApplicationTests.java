package az.code.tourapp;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.repositories.cache.ContactRepository;
import az.code.tourapp.repositories.cache.LastMessageIdRepository;
import az.code.tourapp.repositories.cache.OfferCountRepository;
import az.code.tourapp.repositories.cache.UserDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@MockBeans({
        @MockBean(RabbitTemplate.class),
        @MockBean(UserDataRepository.class),
        @MockBean(LastMessageIdRepository.class),
        @MockBean(OfferCountRepository.class),
        @MockBean(ContactRepository.class),
})
public class TourAppApplicationTests {

    public static final String CHAT_ID = "12345";
    public static final Integer MESSAGE_ID = 12345;
    public static final Locale LOCALE = Locale.EN;

    public static final String TEST_NAME = "John";
    public static final String TEST_STRING = "test";
    public static final String TEST_SURNAME = "Doe";
    public static final String AGENCY_NAME = "Global Travel";
    public static final String PHONE_NUMBER = "994703685666";
    public static final String UUID = "cefadd13-426a-452b-a8a7-96622bf94206";

    @Test
    @DisplayName("Application stats")
    void contextLoads() {}
}
