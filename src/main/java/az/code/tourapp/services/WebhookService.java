package az.code.tourapp.services;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public interface WebhookService {
    BotApiMethod<?> onWebhookUpdateReceived(Update update);

//    ResponseEntity<Boolean> sendResponse(String uuid, MultipartFile file) throws TelegramApiException, IOException;
}
