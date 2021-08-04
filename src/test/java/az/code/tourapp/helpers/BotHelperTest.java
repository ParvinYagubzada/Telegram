package az.code.tourapp.helpers;

import az.code.tourapp.enums.ButtonType;
import az.code.tourapp.enums.Locale;
import az.code.tourapp.models.CustomMessage;
import az.code.tourapp.models.Translatable;
import az.code.tourapp.models.entities.Action;
import az.code.tourapp.utils.CalendarUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.data.util.Pair;
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

import static az.code.tourapp.TourAppApplication.DATE_FORMAT_STRING;
import static az.code.tourapp.TourAppApplicationTests.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class BotHelperTest {

    @Test
    @DisplayName("BotHelper - handleCalendarControls()")
    void handleCalendarControls() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_STRING);
        String choice = "<" + LocalDate.now().format(formatter);
        LocalDate newDate = LocalDate.parse(choice.substring(1), formatter).minusMonths(1);
        EditMessageReplyMarkup expected = EditMessageReplyMarkup.builder()
                .chatId(CHAT_ID)
                .messageId(MESSAGE_ID)
                .replyMarkup(CalendarUtil.createCalendar(newDate, LOCALE.getJavaLocale()))
                .build();
        assertEquals(expected, BotHelper.handleCalendarControls(CHAT_ID, LOCALE, MESSAGE_ID, choice));
    }

    @Test
    @DisplayName("BotHelper - createKeyboard()")
    void createKeyboard() {
        List<Action> actions = IntStream.range(1, 10)
                .mapToObj(value -> Action.builder().text("This is test action no:" + value).build())
                .collect(Collectors.toList());
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (int i = 0; i < actions.size(); i++) {
            String buttonText = BotHelper.getText(actions.get(i), LOCALE);
            row.add(buttonText);
            if ((i + 1) % 2 == 0 || i + 1 == actions.size()) {
                keyboard.add(row);
                row = new KeyboardRow();
            }
        }
        ReplyKeyboardMarkup expected = ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .build();
        assertEquals(expected, BotHelper.createKeyboard(actions, LOCALE));
    }

    @Test
    @DisplayName("BotHelper - createSingleButtonKeyboard()")
    void createSingleButtonKeyboard() {
        InlineKeyboardMarkup expected = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> row = new ArrayList<>();
        List<InlineKeyboardButton> sub = new ArrayList<>();
        sub.add(InlineKeyboardButton.builder()
                .callbackData("loadMore&" + UUID)
                .text(TEST_STRING)
                .build());
        row.add(sub);
        expected.setKeyboard(row);
        assertEquals(expected, BotHelper.createSingleButtonKeyboard(UUID, TEST_STRING));
    }

    @Test
    @DisplayName("BotHelper - createInlineButton()")
    void createInlineButton() {
        String callBack = "testCallback";
        InlineKeyboardButton expected = InlineKeyboardButton.builder()
                .callbackData(callBack)
                .text(TEST_STRING)
                .build();
        assertEquals(expected, BotHelper.createInlineButton(callBack, TEST_STRING));
    }

    @Test
    @DisplayName("BotHelper - createRequestContactKeyboard()")
    void createRequestContactKeyboard() {
        CustomMessage message = CustomMessage.builder()
                .text(">_< 0_0 >_< 0_0 >_< 0_0 >_< 0_0")
                .build();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(KeyboardButton.builder()
                .requestContact(true)
                .text(BotHelper.getText(message, LOCALE))
                .build());
        keyboard.add(row);
        row = new KeyboardRow();
        row.add(KeyboardButton.builder()
                .text(BotHelper.getText(message, LOCALE))
                .build());
        keyboard.add(row);
        ReplyKeyboardMarkup expected = ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
        Pair<String, ButtonType> first = Pair.of(BotHelper.getText(message, LOCALE), ButtonType.CONTACT);
        Pair<String, ButtonType> second = Pair.of(BotHelper.getText(message, LOCALE), ButtonType.DEFAULT);
        assertEquals(expected, BotHelper.createRequestContactKeyboard(first, second));
    }

    @Test
    @DisplayName("BotHelper - createEditMessage()")
    void createEditMessage() {
        EditMessageText expected = EditMessageText.builder()
                .chatId(CHAT_ID)
                .messageId(MESSAGE_ID)
                .text(TEST_STRING)
                .build();
        assertEquals(expected, BotHelper.createEditMessage(CHAT_ID, MESSAGE_ID, TEST_STRING));
    }

    @Test
    @DisplayName("BotHelper - createDeleteMessage()")
    void createDeleteMessage() {
        DeleteMessage expected = DeleteMessage.builder()
                .chatId(CHAT_ID)
                .messageId(MESSAGE_ID)
                .build();
        assertEquals(expected, BotHelper.createDeleteMessage(CHAT_ID, MESSAGE_ID));
    }

    @Test
    @DisplayName("BotHelper - createCustomMessage()")
    void createCustomMessage() {
        SendMessage expected = SendMessage.builder()
                .chatId(CHAT_ID)
                .replyMarkup(ReplyKeyboardRemove.builder().removeKeyboard(true).build())
                .text(TEST_STRING)
                .build();
        assertEquals(expected, BotHelper.createCustomMessage(CHAT_ID, TEST_STRING));
    }

    @Test
    @DisplayName("BotHelper - extractLocale()")
    void extractLocale() {
        assertEquals(Locale.AZ, BotHelper.extractLocale(JSON_DATA));
    }

    @Test
    @DisplayName("BotHelper - getText()")
    void getText() {
        Translatable entry = CustomMessage.builder().text(TEST_STRING).build();
        assertEquals(TEST_STRING, BotHelper.getText(entry, LOCALE));
    }
}