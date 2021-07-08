package az.code.tourapp.configs;

import az.code.tourapp.bots.TourBot;
import az.code.tourapp.models.Command;
import az.code.tourapp.repositories.ActionRepository;
import az.code.tourapp.repositories.QuestionRepository;
import az.code.tourapp.repositories.RedisRepository;
import az.code.tourapp.repositories.RequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.WebhookBot;

import java.util.ArrayList;

@Configuration
public class BotConfig {
    private final QuestionRepository questionRepo;
    private final ActionRepository actionRepo;
    private final RequestRepository requestRepo;
    private final RedisRepository redisRepo;

    @Value("${telegram.bot.token}")
    String token;
    @Value("${telegram.bot.username}")
    String username;
    @Value("${app.domain.url}")
    String baseUrl;
    @Value("${app.api.url}")
    String apiUrl;

    public BotConfig(QuestionRepository questionRepo, ActionRepository actionRepo, RequestRepository requestRepo, RedisRepository redisRepo) {
        this.questionRepo = questionRepo;
        this.actionRepo = actionRepo;
        this.requestRepo = requestRepo;
        this.redisRepo = redisRepo;
    }

    @Bean
    WebhookBot getBot() {
        TourBot bot = new TourBot(questionRepo, actionRepo, requestRepo, redisRepo, username, baseUrl, apiUrl, token);
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
