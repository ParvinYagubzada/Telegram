package az.code.tourapp.configs;

import az.code.tourapp.utils.Mappers;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.WebhookBot;

@Configuration
public class BaseConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public WebhookBot createBot(BotConfig config, Mappers mappers) throws TelegramApiException {
        return mappers.configToBot(config);
    }
}
