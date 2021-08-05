package az.code.tourapp.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static az.code.tourapp.helpers.BotHelper.createInlineButton;
import static az.code.tourapp.helpers.BotHelper.formatter;

public class CalendarUtil {

    public static final String IGNORE = "ignore";
    public static final String IGNORE_TEXT = " ";
    public static final DateTimeFormatter headerFormat = DateTimeFormatter.ofPattern("MMMM yyyy");

    public static InlineKeyboardMarkup createCalendar(LocalDate date, LocalDate start, LocalDate end, Locale locale) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        createMonthRow(locale, date, keyboard);
        createWeekNamesRow(keyboard, locale);
        createDaysSection(date, start, end, keyboard);
        createControlsRow(date, start, end, keyboard);
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private static void createMonthRow(Locale locale, LocalDate date, List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> headerRow = new ArrayList<>();
        headerRow.add(createInlineButton(IGNORE, headerFormat.withLocale(locale).format(date)));
        keyboard.add(headerRow);
    }

    private static void createWeekNamesRow(List<List<InlineKeyboardButton>> keyboard, Locale locale) {
        List<InlineKeyboardButton> daysOfWeekRow = new ArrayList<>();
        String[] weekNames = new DateFormatSymbols(locale).getShortWeekdays();
        for (int i = 2; i < weekNames.length; i++) {
            daysOfWeekRow.add(createInlineButton(IGNORE, weekNames[i]));
        }
        daysOfWeekRow.add(createInlineButton(IGNORE, weekNames[1]));
        keyboard.add(daysOfWeekRow);
    }

    private static void createDaysSection(LocalDate date, LocalDate start, LocalDate end, List<List<InlineKeyboardButton>> keyboard) {
        LocalDate firstDay = date.withDayOfMonth(1);

        int shift = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = firstDay.lengthOfMonth();
        int rows = ((daysInMonth + shift) % 7 > 0 ? 1 : 0) + (daysInMonth + shift) / 7;
        for (int i = 0; i < rows; i++) {
            keyboard.add(createWeekDaysRow(firstDay, start, end, shift));
            firstDay = firstDay.plusDays(7 - shift);
            shift = 0;
        }
    }

    private static void createControlsRow(LocalDate date, LocalDate start, LocalDate end, List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> controlsRow = new ArrayList<>();
        if (start.withDayOfMonth(1).compareTo(date.withDayOfMonth(1)) < 0)
            controlsRow.add(createInlineButton("<" + formatter.format(date), "<"));
        if (end.withDayOfMonth(1).compareTo(date.withDayOfMonth(1)) > 0)
            controlsRow.add(createInlineButton(">" + formatter.format(date), ">"));
        keyboard.add(controlsRow);
    }

    private static List<InlineKeyboardButton> createWeekDaysRow(LocalDate date, LocalDate start, LocalDate end, int shift) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        int day = date.getDayOfMonth();
        LocalDate callbackDate = date;
        for (int j = 0; j < shift; j++) {
            row.add(createInlineButton(IGNORE, IGNORE_TEXT));
        }
        for (int j = shift; j < 7; j++) {
            if (day <= (date.lengthOfMonth())) {
                boolean inRange = start.compareTo(callbackDate) < 1 && end.compareTo(callbackDate) > -1;
                String callback = inRange ? formatter.format(callbackDate) : IGNORE;
                String text = inRange ? Integer.toString(day) : "❌";
                row.add(createInlineButton(callback, text));
                callbackDate = callbackDate.plusDays(1);
                day++;
            } else {
                row.add(createInlineButton(IGNORE, " "));
            }
        }
        return row;
    }
}
