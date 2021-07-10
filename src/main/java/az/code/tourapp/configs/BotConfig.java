package az.code.tourapp.configs;

import az.code.tourapp.bots.TourBot;
import az.code.tourapp.models.Command;
import az.code.tourapp.repositories.ActionRepository;
import az.code.tourapp.repositories.OfferRepository;
import az.code.tourapp.repositories.QuestionRepository;
import az.code.tourapp.repositories.RequestRepository;
import az.code.tourapp.repositories.cache.LastMessageIdRepository;
import az.code.tourapp.repositories.cache.OfferCountRepository;
import az.code.tourapp.repositories.cache.UserDataRepository;
import az.code.tourapp.services.FilesStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;

@Configuration
@RequiredArgsConstructor
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

    @Value("${telegram.bot.token}")
    String token;
    @Value("${telegram.bot.username}")
    String username;
    @Value("${app.domain.url}")
    String baseUrl;
    @Value("${app.api.url}")
    String apiUrl;

    @Bean
    TourBot getBot() {
        TourBot bot = TourBot.builder()
                .store(store).rabbit(template)
                .questionRepo(questionRepo).actionRepo(actionRepo).requestRepo(requestRepo).offerCountRepo(offerCountRepo)
                .cache(userDataRepo).lastMessageRepo(lastMessageRepo).offerRepo(offerRepo)
                .token(token).username(username).baseUrl(baseUrl).apiUrl(apiUrl).build();
        try {
            bot.getCommands().put(new Command("start", "Starts bot interrogation!"), bot::interrogate);
            bot.getCommands().put(new Command("stop", "Stops bot current interrogation."), bot::stop);
            bot.execute(SetWebhook.builder().url(baseUrl + apiUrl).dropPendingUpdates(true).build());
            bot.execute(SetMyCommands.builder()
                    .commands(new ArrayList<>(bot.getCommands().keySet()))
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return bot;
    }
}
