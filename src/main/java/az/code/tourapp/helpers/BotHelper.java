package az.code.tourapp.helpers;

import az.code.tourapp.enums.Locale;
import az.code.tourapp.models.CustomMessage;
import az.code.tourapp.models.Translatable;
import az.code.tourapp.models.entities.Action;
import az.code.tourapp.utils.CalendarUtil;
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

import static az.code.tourapp.utils.CalendarUtil.createButton;

public class BotHelper {

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static EditMessageReplyMarkup handleCalendarControls(String chatId, Locale locale, Integer messageId, String choice) {
        LocalDate newDate = choice.startsWith("<") ?
                LocalDate.parse(choice.substring(1), formatter).minusMonths(1) :
                LocalDate.parse(choice.substring(1), formatter).plusMonths(1);
        return EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(messageId)
                .replyMarkup(CalendarUtil.createCalendar(newDate, locale.getJavaLocale()))
                .build();
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
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .build();
    }

    public static InlineKeyboardMarkup createSingleButtonKeyboard(String uuid, String text) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> row = new ArrayList<>();
        List<InlineKeyboardButton> sub = new ArrayList<>();
        sub.add(createButton("loadMore&" + uuid, text));
        row.add(sub);
        keyboard.setKeyboard(row);
        return keyboard;
    }

    public static ReplyKeyboardMarkup createRequestContactKeyboard(Locale locale, CustomMessage message) {
        KeyboardRow row = new KeyboardRow();
        row.add(KeyboardButton.builder()
                .requestContact(true)
                .text(getText(message, locale))
                .build());
        return ReplyKeyboardMarkup.builder()
                .keyboardRow(row)
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
        if (locale == null) return entry.getText();
        return switch (locale) {
            case EN -> entry.getText();
            case AZ -> entry.getTextAz();
            case RU -> entry.getTextRu();
        };
    }
}
