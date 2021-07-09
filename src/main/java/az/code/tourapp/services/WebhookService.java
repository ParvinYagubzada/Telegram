package az.code.tourapp.services;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface WebhookService {
    BotApiMethod<?> onWebhookUpdateReceived(Update update);
}
