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

import static az.code.tourapp.TourAppApplicationTests.LOCALE;
import static az.code.tourapp.helpers.BotHelper.formatter;
import static az.code.tourapp.utils.CalendarUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class CalendarUtilTest {

    @Test
    @DisplayName("CalendarUtil - createCalendar()")
    void createCalendar() {
        Locale locale = LOCALE.getJavaLocale();
        LocalDate date = LocalDate.parse("28.07.2021", formatter);
        InlineKeyboardMarkup expected = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        createMonthRow(locale, date, keyboard);
        createWeekNamesRow(locale, keyboard);
        createDaysSection(keyboard);
        createControlsRow(date, keyboard);
        expected.setKeyboard(keyboard);
        assertEquals(expected, CalendarUtil.createCalendar(date, date.minusMonths(12), date.plusMonths(12), locale));
    }

    private void createMonthRow(Locale locale, LocalDate date, List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> headerRow = new ArrayList<>();
        headerRow.add(InlineKeyboardButton.builder()
                .callbackData(IGNORE)
                .text(headerFormat.withLocale(locale).format(date))
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
                .callbackData(formatter.format(LocalDate.of(2021, 7, day)))
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

    private void createControlsRow(LocalDate date, List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> controlsRow = new ArrayList<>();
        controlsRow.add(InlineKeyboardButton.builder()
                .callbackData("<" + formatter.format(date))
                .text("<")
                .build());
        controlsRow.add(InlineKeyboardButton.builder()
                .callbackData(">" + formatter.format(date))
                .text(">")
                .build());
        keyboard.add(controlsRow);
    }
}