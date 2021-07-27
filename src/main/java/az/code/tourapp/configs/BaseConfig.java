package az.code.tourapp.configs;

import az.code.tourapp.bots.TourBot;
import az.code.tourapp.utils.Mappers;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Configuration
public class BaseConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public TourBot createBot(BotConfig config, Mappers mappers) throws TelegramApiException {
        return mappers.configToBot(config);
    }
}
