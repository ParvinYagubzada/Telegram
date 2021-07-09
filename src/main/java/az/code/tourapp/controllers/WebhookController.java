package az.code.tourapp.controllers;

import az.code.tourapp.exceptions.NoSuchRequestException;
import az.code.tourapp.services.WebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.ws.rs.core.Response;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController {

    private final WebhookService service;

    public WebhookController(WebhookService service) {
        this.service = service;
    }

    @ExceptionHandler(NoSuchRequestException.class)
    private ResponseEntity<String> handleRequestException(NoSuchRequestException exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @PostMapping
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        return service.onWebhookUpdateReceived(update);
    }

//    @PostMapping("/sendResponse")
//    public ResponseEntity<Boolean> sendResponse(@RequestParam String uuid, @RequestBody MultipartFile file) throws TelegramApiException, IOException {
//        return service.sendResponse(uuid, file);
//    }
}
