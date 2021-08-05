package az.code.tourapp.helpers;

import az.code.tourapp.enums.ButtonType;
import az.code.tourapp.enums.Locale;
import az.code.tourapp.exceptions.user.DateMismatchException;
import az.code.tourapp.exceptions.user.InputMismatchException;
import az.code.tourapp.models.CustomMessage;
import az.code.tourapp.models.Translatable;
import az.code.tourapp.models.dto.AcceptedOffer;
import az.code.tourapp.models.entities.Action;
import az.code.tourapp.models.entities.RequestId;
import az.code.tourapp.utils.CalendarUtil;
import az.code.tourapp.utils.Mappers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static az.code.tourapp.TourAppApplicationTests.*;
import static az.code.tourapp.configs.RabbitConfig.ACCEPTED_EXCHANGE;
import static az.code.tourapp.configs.RabbitConfig.ACCEPTED_KEY;
import static az.code.tourapp.helpers.BotHelper.formatter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class BotHelperTest {

    @Mock
    Mappers mappers;
    @Mock
    RabbitTemplate template;

    @Test
    @DisplayName("BotHelper - handleCalendarControls() - Valid")
    void handleDateType() {
        String actionText = "15.06.2020";
        Action action = Action.builder().text("13.06.2020").textAz("13.07.2020").build();
        assertEquals(action, BotHelper.handleDateType(actionText, action));
        actionText = "13.06.2020";
        assertEquals(action, BotHelper.handleDateType(actionText, action));
        actionText = "13.07.2020";
        assertEquals(action, BotHelper.handleDateType(actionText, action));
    }

    @Test
    @DisplayName("BotHelper - handleCalendarControls() - DateMismatchException")
    void handleDateType_DateMismatchException() {
        String actionText = "12.06.2020";
        Action action = Action.builder().text("13.06.2020").textAz("13.07.2020").build();
        assertThrows(DateMismatchException.class, () ->
                BotHelper.handleDateType(actionText, action));
    }

    @Test
    @DisplayName("BotHelper - handleCalendarControls() - InputMismatchException")
    void handleDateType_InputMismatchException() {
        assertThrows(InputMismatchException.class, () ->
                BotHelper.handleDateType(TEST_STRING, new Action()));
    }

    @Test
    @DisplayName("BotHelper - convertRepresentation() - Valid")
    void convertRepresentation() {
        String dateString = "13.06.2020";
        LocalDate date = LocalDate.parse(dateString, formatter);
        assertEquals(dateString, BotHelper.convertRepresentation(dateString, String.class));
        assertEquals(date, BotHelper.convertRepresentation(dateString, LocalDate.class));
    }

    @Test
    @DisplayName("BotHelper - convertRepresentation() - Valid")
    void convertRepresentation_Special() {
        String dateString = "plusDays 2";
        LocalDate expectedDate = LocalDate.now().plusDays(2);
        String expected = formatter.format(expectedDate);
        assertEquals(expected, BotHelper.convertRepresentation(dateString, String.class));
        assertEquals(expectedDate, BotHelper.convertRepresentation(dateString, LocalDate.class));
    }

    @Test
    @DisplayName("BotHelper - convertRepresentation() - NoSuchMethodException")
    void convertRepresentation_NoSuchMethodException() {
        String dateString = "random 2";
        LocalDate expectedDate = LocalDate.now();
        String expected = formatter.format(expectedDate);
        assertEquals(expected, BotHelper.convertRepresentation(dateString, String.class));
        assertEquals(expectedDate, BotHelper.convertRepresentation(dateString, LocalDate.class));
    }

    @Test
    @DisplayName("BotHelper - convertRepresentation() - DateTimeParseException")
    void convertRepresentation_Invalid_Format() {
        String dateString = "1234";
        assertEquals(dateString, BotHelper.convertRepresentation(dateString, String.class));
        assertThrows(DateTimeParseException.class, () -> BotHelper.convertRepresentation(dateString, LocalDate.class));
    }

    @Test
    @DisplayName("BotHelper - handleCalendarControls()")
    void handleCalendarControls() {
        LocalDate start = LocalDate.parse("01.01.2000", formatter);
        LocalDate end = LocalDate.parse("31.12.2050", formatter);
        String choice = "<" + LocalDate.now().format(formatter);
        LocalDate newDate = LocalDate.parse(choice.substring(1), formatter).minusMonths(1);
        EditMessageReplyMarkup expected = EditMessageReplyMarkup.builder()
                .chatId(CHAT_ID)
                .messageId(MESSAGE_ID)
                .replyMarkup(CalendarUtil.createCalendar(newDate, start, end, LOCALE.getJavaLocale()))
                .build();
        assertEquals(expected, BotHelper.handleCalendarControls(CHAT_ID, LOCALE, MESSAGE_ID, choice, start, end));
    }

    @Test
    void sendPreUserInfo() {
        User user = createUser();
        Contact contact = createContact();
        RequestId id = new RequestId(AGENCY_NAME, UUID);
        AcceptedOffer acceptedOffer = new AcceptedOffer();

        Mockito.when(mappers.contactToAcceptedOffer(UUID, AGENCY_NAME, TEST_NAME, contact))
                .thenReturn(acceptedOffer);
        BotHelper.sendPreUserInfo(user, id, template, mappers);
        Mockito.verify(template, Mockito.times(1))
                .convertAndSend(ACCEPTED_EXCHANGE, ACCEPTED_KEY, acceptedOffer);
    }

    private Contact createContact() {
        Contact contact = new Contact();
        contact.setFirstName(TEST_NAME);
        contact.setLastName(TEST_SURNAME);
        contact.setUserId(TEST_LONG);
        return contact;
    }

    private User createUser() {
        User user = new User();
        user.setUserName(TEST_NAME);
        user.setFirstName(TEST_NAME);
        user.setLastName(TEST_SURNAME);
        user.setId(TEST_LONG);
        return user;
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