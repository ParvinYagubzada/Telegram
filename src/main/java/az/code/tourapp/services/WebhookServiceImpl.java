package az.code.tourapp.services;

import az.code.tourapp.bots.TourBot;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.WebhookBot;

import java.io.IOException;

@Service
public class WebhookServiceImpl implements WebhookService{

    private final TourBot bot;

    public WebhookServiceImpl(TourBot bot) {
        this.bot = bot;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return bot.onWebhookUpdateReceived(update);
    }

//    @Override
//    public ResponseEntity<Boolean> sendResponse(String uuid, MultipartFile file) throws TelegramApiException, IOException {
//        return new ResponseEntity<>(bot.sendResponse(uuid, file), HttpStatus.OK);
//    }
}
