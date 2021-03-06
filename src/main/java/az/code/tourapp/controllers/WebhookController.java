package az.code.tourapp.controllers;

import az.code.tourapp.services.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhook")
public class WebhookController {

    private final WebhookService service;

    @PostMapping
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        return service.onWebhookUpdateReceived(update);
    }
}
