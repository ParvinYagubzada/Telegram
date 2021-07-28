package az.code.tourapp.services;

import az.code.tourapp.bots.TourBot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final TourBot bot;

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return bot.onWebhookUpdateReceived(update);
    }
}
