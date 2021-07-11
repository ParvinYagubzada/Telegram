package az.code.tourapp.configs;

import az.code.tourapp.bots.TourBot;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Setter
@Configuration
public class BotConfig {

    final
    TelegramConfiguration properties;

    public BotConfig(TelegramConfiguration properties) {
        this.properties = properties;
    }

    @Bean
    TourBot getBot() {
        return new TourBot(properties);
    }
}
