package az.code.tourapp.helpers;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.models.CustomMessage;
import az.code.tourapp.models.Translatable;
import az.code.tourapp.models.entities.Action;
import az.code.tourapp.utils.CalendarUtil;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotHelperTest {

    @Test
    void handleCalendarControls() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String chatId = "12345", choice = "<" + LocalDate.now().format(formatter);
        Locale locale = Locale.EN;
        Integer messageId = 123456;
        LocalDate newDate = LocalDate.parse(choice.substring(1), formatter).minusMonths(1);
        EditMessageReplyMarkup expected = EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(messageId)
                .replyMarkup(CalendarUtil.createCalendar(newDate, locale.getJavaLocale()))
                .build();
        assertEquals(expected, BotHelper.handleCalendarControls(chatId, locale, messageId, choice));
    }

    @Test
    void createKeyboard() {
        Locale locale = Locale.EN;
        List<Action> actions = IntStream.range(1, 10)
                .mapToObj(value -> Action.builder().text("This is test action no:" + value).build())
                .collect(Collectors.toList());
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (int i = 0; i < actions.size(); i++) {
            String buttonText = BotHelper.getText(actions.get(i), locale);
            row.add(buttonText);
            if ((i + 1) % 2 == 0 || i + 1 == actions.size()) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
        }
        ReplyKeyboardMarkup expected = ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .build();
        assertEquals(expected, BotHelper.createKeyboard(actions, locale));
    }

    @Test
    void createSingleButtonKeyboard() {
        String uuid = "12345", text = "123456";
        InlineKeyboardMarkup expected = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> row = new ArrayList<>();
        List<InlineKeyboardButton> sub = new ArrayList<>();
        sub.add(InlineKeyboardButton.builder()
                .callbackData("loadMore&" + uuid)
                .text(text)
                .build());
        row.add(sub);
        expected.setKeyboard(row);
        assertEquals(expected, BotHelper.createSingleButtonKeyboard(uuid, text));
    }

    @Test
    void createRequestContactKeyboard() {
        Locale locale = Locale.EN;
        CustomMessage message = CustomMessage.builder()
                .text(">_< 0_0 >_< 0_0 >_< 0_0 >_< 0_0")
                .build();
        KeyboardRow row = new KeyboardRow();
        row.add(KeyboardButton.builder()
                .requestContact(true)
                .text(BotHelper.getText(message, locale))
                .build());
        ReplyKeyboardMarkup expected = ReplyKeyboardMarkup.builder()
                .keyboardRow(row)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
        assertEquals(expected, BotHelper.createRequestContactKeyboard(locale, message));
    }

    @Test
    void createEditMessage() {
        String chatId = "12345", text = "123456";
        Integer messageId = 123456;
        EditMessageText expected = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .build();
        assertEquals(expected, BotHelper.createEditMessage(chatId, messageId, text));
    }

    @Test
    void createDeleteMessage() {
        String chatId = "12345";
        Integer messageId = 12345;
        DeleteMessage expected = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        assertEquals(expected, BotHelper.createDeleteMessage(chatId, messageId));
    }

    @Test
    void createCustomMessage() {
        String chatId = "12345", myMessage = "test";
        SendMessage expected = SendMessage.builder()
                .chatId(chatId)
                .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                .text(myMessage)
                .build();
        assertEquals(expected, BotHelper.createCustomMessage(chatId, myMessage));
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void extractLocale() {
        String data = "{\"travelEndDate\":\"20.07.2021\",\"tourType\":\"Istirahət-gəzinti\",\"travelStartDate\":\"21.07.2021\",\"language\":\"AZ\",\"travellerCount\":\"1234\",\"addressFrom\":\"Baki\",\"addressTo\":\"TurAl təklif etsin\",\"budget\":\"1234\"}";
        assertEquals(Locale.AZ, BotHelper.extractLocale(data));
    }

    @Test
    void getText() {
        String expected = "test";
        Translatable entry = CustomMessage.builder().text("test").build();
        Locale locale = Locale.EN;
        assertEquals(expected, BotHelper.getText(entry, locale));
    }
}