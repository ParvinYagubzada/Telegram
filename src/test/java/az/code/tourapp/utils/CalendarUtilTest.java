package az.code.tourapp.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static az.code.tourapp.utils.CalendarUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class CalendarUtilTest {

    @Test
    @DisplayName("CalendarUtil - createCalendar()")
    void createCalendar() {
        Locale locale = az.code.tourapp.enums.Locale.EN.getJavaLocale();
        LocalDate javaDate = LocalDate.of(2021, 7, 28);
        org.joda.time.LocalDate date = CalendarUtil.toJodaLocalDate(javaDate);
        InlineKeyboardMarkup expected = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        createMonthRow(locale, date, keyboard);
        createWeekNamesRow(locale, keyboard);
        createDaysSection(keyboard);
        createControlsRow(date, keyboard);
        expected.setKeyboard(keyboard);
        assertEquals(expected, CalendarUtil.createCalendar(javaDate, locale));
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
        org.joda.time.LocalDate expected = org.joda.time.LocalDate.now();
        LocalDate date = LocalDate.now();
        assertEquals(expected, CalendarUtil.toJodaLocalDate(date));
    }
}