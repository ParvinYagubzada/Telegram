package az.code.tourapp.configs;

import az.code.tourapp.bots.TourBot;
import az.code.tourapp.models.Command;
import az.code.tourapp.repositories.ActionRepository;
import az.code.tourapp.repositories.QuestionRepository;
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

    @Value("${telegram.bot.token}")
    String token;
    @Value("${telegram.bot.username}")
    String username;
    @Value("${app.domain.url}")
    String baseUrl;
    @Value("${app.api.url}")
    String apiUrl;

    public BotConfig(QuestionRepository questionRepo, ActionRepository actionRepo, RequestRepository requestRepo) {
        this.questionRepo = questionRepo;
        this.actionRepo = actionRepo;
        this.requestRepo = requestRepo;
    }

    @Bean
    WebhookBot getBot() {
        TourBot bot = new TourBot(questionRepo, actionRepo, requestRepo, token, username, baseUrl, apiUrl);
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
