package az.code.tourapp.configs;

import az.code.tourapp.models.CustomMessage;
import az.code.tourapp.repositories.ActionRepository;
import az.code.tourapp.repositories.OfferRepository;
import az.code.tourapp.repositories.QuestionRepository;
import az.code.tourapp.repositories.RequestRepository;
import az.code.tourapp.repositories.cache.LastMessageIdRepository;
import az.code.tourapp.repositories.cache.OfferCountRepository;
import az.code.tourapp.repositories.cache.UserDataRepository;
import az.code.tourapp.services.FilesStorageService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Setter
@Getter
@Configuration
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "telegram.bot")
public class BotConfig {

    private final FilesStorageService store;
    private final RabbitTemplate template;

    //SQL Repos
    private final QuestionRepository questionRepo;
    private final ActionRepository actionRepo;
    private final RequestRepository requestRepo;
    private final OfferRepository offerRepo;

    //Redis Repos
    private final UserDataRepository userDataRepo;
    private final LastMessageIdRepository lastMessageRepo;
    private final OfferCountRepository offerCountRepo;

    private String token;
    private String username;
    private String domain;
    private String api;

    private Map<String, CustomMessage> messages;
}
