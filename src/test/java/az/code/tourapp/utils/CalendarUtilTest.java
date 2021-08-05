package az.code.tourapp.utils;

import org.joda.time.LocalDate;
import org.junit.jupiter.api.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static az.code.tourapp.TourAppApplicationTests.CHAT_ID;
import static az.code.tourapp.TourAppApplicationTests.MESSAGE_ID;
import static az.code.tourapp.utils.CalendarUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class CalendarUtilTest {

    @Test
    @DisplayName("CalendarUtil - createCalendar()")
    void createCalendar() {
        Locale locale = az.code.tourapp.enums.Locale.EN.getJavaLocale();
        org.joda.time.LocalDate date = format.parseLocalDate("28.07.2021");
        InlineKeyboardMarkup expected = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        createMonthRow(locale, date, keyboard);
        createWeekNamesRow(locale, keyboard);
        createDaysSection(keyboard);
        createControlsRow(date, keyboard);
        expected.setKeyboard(keyboard);
        assertEquals(expected, CalendarUtil.createCalendar(date, date.minusMonths(12), date.plusMonths(12), locale));
    }

    private void createMonthRow(Locale locale, org.joda.time.LocalDate date, List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> headerRow = new ArrayList<>();
        headerRow.add(InlineKeyboardButton.builder()
                .callbackData(IGNORE)
                .text(headerFormat.withLocale(locale).print(date))
                .build());
        keyboard.add(headerRow);
    }

    private void createWeekNamesRow(Locale locale, List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> daysOfWeekRow = new ArrayList<>();
        String[] weekNames = new DateFormatSymbols(locale).getShortWeekdays();
        for (int i = 2; i < weekNames.length; i++) {
            daysOfWeekRow.add(InlineKeyboardButton.builder()
                    .callbackData(IGNORE)
                    .text(weekNames[i])
                    .build());
        }
        daysOfWeekRow.add(InlineKeyboardButton.builder()
                .callbackData(IGNORE)
                .text(weekNames[1])
                .build());
        keyboard.add(daysOfWeekRow);
    }

    private void createDaysSection(List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        int day = 1;
        for (int i = 0; i < 5; i++) {
            if (i == 0) {
                for (int j = 0; j < 7; j++) {
                    if (j < 3) {
                        addIgnoreButton(row);
                    } else {
                        day = addNormalButton(row, day);
                    }
                }
            } else {
                for (int j = 0; j < 7; j++) {
                    if (i == 4 && j == 6) {
                        addIgnoreButton(row);
                    } else {
                        day = addNormalButton(row, day);
                    }

                }
            }
            keyboard.add(row);
            row = new ArrayList<>();
        }
    }

    private int addNormalButton(List<InlineKeyboardButton> row, int day) {
        row.add(InlineKeyboardButton.builder()
                .callbackData(format.print(new org.joda.time.LocalDate(2021, 7, day)))
                .text(Integer.toString(day++))
                .build());
        return day;
    }

    private void addIgnoreButton(List<InlineKeyboardButton> row) {
        row.add(InlineKeyboardButton.builder()
                .callbackData(IGNORE)
                .text(IGNORE_TEXT)
                .build());
    }

    private void createControlsRow(org.joda.time.LocalDate date, List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> controlsRow = new ArrayList<>();
        controlsRow.add(InlineKeyboardButton.builder()
                .callbackData("<" + format.print(date))
                .text("<")
                .build());
        controlsRow.add(InlineKeyboardButton.builder()
                .callbackData(">" + format.print(date))
                .text(">")
                .build());
        keyboard.add(controlsRow);
    }

    @Test
    @DisplayName("CalendarUtil - toJodaLocalDate()")
    void toJodaLocalDate() {
        LocalDate expected = LocalDate.now();
        java.time.LocalDate date = java.time.LocalDate.now();
        assertEquals(expected, CalendarUtil.toJodaLocalDate(date));
    }

    @Test
    @Disabled
    @DisplayName("CalendarUtil - handleCalendarControls()")
    void handleCalendarControls() {
        String start = "01.01.2000";
        String end = "31.12.2050";
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_STRING);
//        String choice = "<" + LocalDate.now().format(formatter);
//        LocalDate newDate = LocalDate.parse(choice.substring(1), formatter).minusMonths(1);
        EditMessageReplyMarkup expected = EditMessageReplyMarkup.builder()
                .chatId(CHAT_ID)
                .messageId(MESSAGE_ID)
//                .replyMarkup(CalendarUtil.createCalendar(newDate, LOCALE.getJavaLocale()))
                .build();
//        assertEquals(expected, CalendarUtil.handleCalendarControls(CHAT_ID, LOCALE, MESSAGE_ID, choice, start, end));
    }

}