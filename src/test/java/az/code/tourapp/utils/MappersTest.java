package az.code.tourapp.utils;

import az.code.tourapp.bots.TourBot;
import az.code.tourapp.configs.BotConfig;
import az.code.tourapp.models.CustomMessage;
import az.code.tourapp.models.dto.AcceptedOffer;
import az.code.tourapp.models.entities.BotUser;
import az.code.tourapp.repositories.*;
import az.code.tourapp.repositories.cache.ContactRepository;
import az.code.tourapp.repositories.cache.LastMessageIdRepository;
import az.code.tourapp.repositories.cache.OfferCountRepository;
import az.code.tourapp.repositories.cache.UserDataRepository;
import az.code.tourapp.services.FilesStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;

import static az.code.tourapp.TourAppApplicationTests.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestMethodOrder(MethodOrderer.DisplayName.class)
class MappersTest {

    @Autowired
    Mappers mappers;
    @Autowired
    BotConfig config;

    @MockBean
    private FilesStorageService store;
    @MockBean
    private RabbitTemplate rabbit;
    @MockBean
    private QuestionRepository questionRepo;
    @MockBean
    private ActionRepository actionRepo;
    @MockBean
    private RequestRepository requestRepo;
    @MockBean
    private OfferRepository offerRepo;
    @MockBean
    private UserRepository userRepo;
    @MockBean
    private UserDataRepository userDataRepo;
    @MockBean
    private LastMessageIdRepository lastMessageRepo;
    @MockBean
    private OfferCountRepository offerCountRepo;
    @MockBean
    private ContactRepository contactRepo;

    @Test
    @DisplayName("Mappers - AuthConfig to SecurityServiceImpl")
    void configToBot(
            @Value("#{botConfig.token}") String token,
            @Value("#{botConfig.username}") String username,
            @Value("#{botConfig.domain}") String domain,
            @Value("#{botConfig.api}") String api,
            @Value("#{botConfig.firstQuestionId}") Long firstQuestionId,
            @Value("#{botConfig.expirationDays}") Integer expirationDays,
            @Value("#{botConfig.messages}") Map<String, CustomMessage> messages
    ) throws TelegramApiException, InterruptedException {
        TourBot expected = TourBot.builder()
                .store(store).rabbit(rabbit).questionRepo(questionRepo).mappers(mappers)
                .actionRepo(actionRepo).requestRepo(requestRepo).offerRepo(offerRepo)
                .userRepo(userRepo).userDataRepo(userDataRepo).lastMessageRepo(lastMessageRepo)
                .offerCountRepo(offerCountRepo).contactRepo(contactRepo)
                .token(token).username(username).domain(domain)
                .api(api).firstQuestionId(firstQuestionId).expirationDays(expirationDays).messages(messages)
                .build();

        Thread.sleep(1000);
        assertEquals(expected, mappers.configToBot(config));
    }

    @Test
    void contactToBotUser() {
        Contact contact = new Contact();
        contact.setUserId(TEST_LONG);
        contact.setFirstName(TEST_NAME);
        contact.setLastName(TEST_SURNAME);
        contact.setPhoneNumber(PHONE_NUMBER);
        BotUser expected = BotUser.builder()
                .username(TEST_STRING).userId(TEST_LONG)
                .firstName(TEST_NAME).lastName(TEST_SURNAME)
                .phoneNumber(PHONE_NUMBER).build();

        assertEquals(expected, mappers.contactToBotUser(TEST_STRING, contact));
    }

    @Test
    void botUserToAcceptedOffer() {
        BotUser botUser = BotUser.builder()
                .username(TEST_STRING).userId(TEST_LONG)
                .firstName(TEST_NAME).lastName(TEST_SURNAME)
                .phoneNumber(PHONE_NUMBER).build();
        AcceptedOffer expected = AcceptedOffer.builder()
                .uuid(UUID).agencyName(AGENCY_NAME).username(TEST_STRING)
                .firstName(TEST_NAME).lastName(TEST_SURNAME)
                .userId("1").phoneNumber(PHONE_NUMBER)
                .build();
        assertEquals(expected, mappers.botUserToAcceptedOffer(UUID, AGENCY_NAME, botUser));
    }

    @Test
    void contactToAcceptedOffer() {
        Contact contact = new Contact();
        contact.setUserId(TEST_LONG);
        contact.setFirstName(TEST_NAME);
        contact.setLastName(TEST_SURNAME);
        AcceptedOffer expected = AcceptedOffer.builder()
                .uuid(UUID).agencyName(AGENCY_NAME).username(TEST_STRING)
                .firstName(TEST_NAME).lastName(TEST_SURNAME).userId("1")
                .build();

        assertEquals(expected, mappers.contactToAcceptedOffer(UUID, AGENCY_NAME, TEST_STRING, contact));

        contact.setPhoneNumber(null);
        expected.setPhoneNumber(null);
        assertEquals(expected, mappers.contactToAcceptedOffer(UUID, AGENCY_NAME, TEST_STRING, contact));
    }
}