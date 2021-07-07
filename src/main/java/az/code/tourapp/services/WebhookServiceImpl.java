package az.code.tourapp.services;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.WebhookBot;

@Service
public class WebhookServiceImpl implements WebhookService{

    private final WebhookBot bot;

    public WebhookServiceImpl(WebhookBot bot) {
        this.bot = bot;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return bot.onWebhookUpdateReceived(update);
    }
}
