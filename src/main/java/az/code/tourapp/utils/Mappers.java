package az.code.tourapp.utils;

import az.code.tourapp.bots.TourBot;
import az.code.tourapp.configs.BotConfig;
import az.code.tourapp.models.dto.AcceptedOffer;
import az.code.tourapp.models.entities.BotUser;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class Mappers {

    private final ModelMapper mapper;

    public TourBot configToBot(BotConfig config) throws TelegramApiException {
        return mapper.map(config, TourBot.class).init();
    }

    public BotUser contactToBotUser(String username, Contact contact) {
        BotUser user = mapper.map(contact, BotUser.class);
        user.setUserName(username);
        return user;
    }

    public AcceptedOffer botUserToAcceptedOffer(String uuid, String agencyName, BotUser botUser) {
        AcceptedOffer offer = mapper.map(botUser, AcceptedOffer.class);
        offer.setUuid(uuid);
        offer.setAgencyName(agencyName);
        return offer;
    }

    public AcceptedOffer contactToAcceptedOffer(String uuid, String agencyName, String username, Contact contact) {
        AcceptedOffer offer = mapper.map(contact, AcceptedOffer.class);
        offer.setUuid(uuid);
        offer.setAgencyName(agencyName);
        offer.setUsername(username);
        return offer;
    }
}
