package az.code.tourapp.configs;

import az.code.tourapp.bots.TourBot;
import az.code.tourapp.models.Command;
import az.code.tourapp.repositories.*;
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
    private final QuestionRepository questionRepo;
    private final ActionRepository actionRepo;
    private final RequestRepository requestRepo;
    private final OfferRepository offerRepo;
    private final RedisRepository redisRepo;

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
        TourBot bot = new TourBot(store, template, questionRepo, actionRepo, requestRepo, offerRepo, redisRepo, token, username, baseUrl, apiUrl);
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
