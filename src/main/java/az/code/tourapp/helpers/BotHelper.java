package az.code.tourapp.helpers;

import az.code.tourapp.enums.ButtonType;
import az.code.tourapp.enums.Locale;
import az.code.tourapp.exceptions.user.DateMismatchException;
import az.code.tourapp.exceptions.user.InputMismatchException;
import az.code.tourapp.models.Translatable;
import az.code.tourapp.models.entities.Action;
import az.code.tourapp.models.entities.RequestId;
import az.code.tourapp.utils.Mappers;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.util.Pair;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static az.code.tourapp.TourAppApplication.DATE_FORMAT_STRING;
import static az.code.tourapp.configs.RabbitConfig.ACCEPTED_EXCHANGE;
import static az.code.tourapp.configs.RabbitConfig.ACCEPTED_KEY;
import static az.code.tourapp.utils.CalendarUtil.createCalendar;

@Log4j2
public class BotHelper {

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_STRING);
    public static String DATE_REGEX = "\\d{2}\\.\\d{2}\\.\\d{4}";

    public static Action handleDateType(String actionText, Action action) {
        try {
            LocalDate selectedDate = LocalDate.parse(actionText, formatter);
            String start = convertRepresentation(action.getText(), String.class);
            String end = convertRepresentation(action.getTextAz(), String.class);
            LocalDate startDate = LocalDate.parse(start, formatter);
            LocalDate endDate = LocalDate.parse(end, formatter);
            if (startDate.compareTo(selectedDate) < 1 && endDate.compareTo(selectedDate) > -1)
                return action;
            else
                throw new DateMismatchException(start, end);
        } catch (DateTimeParseException e) {
            throw new InputMismatchException();
        }
    }

    public static <T> T convertRepresentation(String dateRepresentation, Class<T> tClass) {
        if (dateRepresentation.matches("^\\p{Alpha}.+")) {
            String[] data = dateRepresentation.split(" ");
            LocalDate date = LocalDate.now();
            try {
                Method method = LocalDate.class.getMethod(data[0], long.class);
                date = (LocalDate) method.invoke(date, Integer.valueOf(data[1]));
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                log.error(e.getMessage(), e);
            }
            return (T) (tClass.getSimpleName().equals("LocalDate") ? date : date.format(formatter));
        }
        return (T) (tClass.getSimpleName().equals("LocalDate") ? LocalDate.parse(dateRepresentation, formatter) : dateRepresentation);
    }

    public static EditMessageReplyMarkup handleCalendarControls(
            String chatId, Locale locale, Integer messageId, String choice, LocalDate start, LocalDate end
    ) {
        LocalDate newDate = choice.startsWith("<") ?
                LocalDate.parse(choice.substring(1), formatter).minusMonths(1) :
                LocalDate.parse(choice.substring(1), formatter).plusMonths(1);
        return EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(messageId)
                .replyMarkup(createCalendar(newDate, start, end, locale.getJavaLocale()))
                .build();
    }

    public static void configureCalendarMessage(Locale locale, SendMessage message, Action action) {
        LocalDate now = LocalDate.now();
        String startString = convertRepresentation(action.getText(), String.class);
        String endString = convertRepresentation(action.getTextAz(), String.class);
        LocalDate start = LocalDate.parse(startString, formatter);
        LocalDate end = LocalDate.parse(endString, formatter);
        message.setText(String.format(message.getText(), startString, endString));
        message.setReplyMarkup(createCalendar(now, start, end, locale.getJavaLocale()));
    }

    public static void sendPreUserInfo(User user, RequestId id, RabbitTemplate rabbit, Mappers mappers) {
        Contact contact = new Contact();
        contact.setFirstName(user.getFirstName());
        contact.setLastName(user.getLastName());
        contact.setUserId(user.getId());
        rabbit.convertAndSend(ACCEPTED_EXCHANGE, ACCEPTED_KEY, mappers.contactToAcceptedOffer(
                id.getUuid(),
                id.getAgencyName(),
                user.getUserName(),
                contact
        ));
    }

    public static ReplyKeyboardMarkup createKeyboard(List<Action> actions, Locale locale) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (int i = 0; i < actions.size(); i++) {
            String buttonText = getText(actions.get(i), locale);
            row.add(buttonText);
            if ((i + 1) % 2 == 0 || i + 1 == actions.size()) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
        }
        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
    }

    public static InlineKeyboardMarkup createSingleButtonKeyboard(String uuid, String text) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> row = new ArrayList<>();
        List<InlineKeyboardButton> sub = new ArrayList<>();
        sub.add(createInlineButton("loadMore&" + uuid, text));
        row.add(sub);
        keyboard.setKeyboard(row);
        return keyboard;
    }

    public static InlineKeyboardButton createInlineButton(String callBack, String text) {
        return InlineKeyboardButton.builder().callbackData(callBack).text(text).build();
    }

    @SafeVarargs
    public static ReplyKeyboardMarkup createRequestContactKeyboard(Pair<String, ButtonType>... messages) {
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (Pair<String, ButtonType> message : messages) {
            KeyboardButton button = KeyboardButton.builder()
                    .text(message.getFirst()).build();
            switch (message.getSecond()) {
                case CONTACT -> button.setRequestContact(true);
                case LOCATION -> button.setRequestLocation(true);
            }
            row.add(button);
            keyboard.add(row);
            row = new KeyboardRow();
        }
        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }

    public static EditMessageText createEditMessage(String chatId, Integer messageId, String text) {
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .build();
    }

    public static DeleteMessage createDeleteMessage(String chatId, Integer messageId) {
        return DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
    }

    public static SendMessage createCustomMessage(String chatId, String myMessage) {
        return SendMessage.builder()
                .chatId(chatId)
                .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                .text(myMessage)
                .build();
    }

    public static Locale extractLocale(String data) {
        String fieldName = "language\":\"";
        int start = data.indexOf(fieldName) + fieldName.length();
        return Locale.valueOf(data.substring(start, start + 2));
    }

    public static String getText(Translatable entry, Locale locale) {
        if (locale == null) locale = Locale.DEFAULT;
        return switch (locale) {
            case EN -> entry.getText();
            case AZ -> entry.getTextAz();
            case RU -> entry.getTextRu();
        };
    }
}
