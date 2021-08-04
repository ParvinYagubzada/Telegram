package az.code.tourapp.utils;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static az.code.tourapp.TourAppApplication.DATE_FORMAT_STRING;
import static az.code.tourapp.helpers.BotHelper.createInlineButton;

public class CalendarUtil {

    public static final String IGNORE = "ignore";
    public static final String IGNORE_TEXT = " ";
    public static final DateTimeFormatter format = DateTimeFormat.forPattern(DATE_FORMAT_STRING);
    public static final DateTimeFormatter headerFormat = DateTimeFormat.forPattern("MMMM yyyy");

    public static InlineKeyboardMarkup createCalendar(java.time.LocalDate input, Locale locale) {
        LocalDate date = toJodaLocalDate(input);
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        createMonthRow(locale, date, keyboard);
        createWeekNamesRow(keyboard, locale);
        createDaysSection(date, keyboard);
        createControlsRow(date, keyboard);
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private static void createMonthRow(Locale locale, LocalDate date, List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> headerRow = new ArrayList<>();
        headerRow.add(createInlineButton(IGNORE, headerFormat.withLocale(locale).print(date)));
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

    private static void createDaysSection(LocalDate date, List<List<InlineKeyboardButton>> keyboard) {
        LocalDate firstDay = date.dayOfMonth().withMinimumValue();

        int shift = firstDay.dayOfWeek().get() - 1;
        int daysInMonth = firstDay.dayOfMonth().getMaximumValue();
        int rows = ((daysInMonth + shift) % 7 > 0 ? 1 : 0) + (daysInMonth + shift) / 7;
        for (int i = 0; i < rows; i++) {
            keyboard.add(createWeekDaysRow(firstDay, shift));
            firstDay = firstDay.plusDays(7 - shift);
            shift = 0;
        }
    }

    private static void createControlsRow(LocalDate date, List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> controlsRow = new ArrayList<>();
        controlsRow.add(createInlineButton("<" + format.print(date), "<"));
        controlsRow.add(createInlineButton(">" + format.print(date), ">"));
        keyboard.add(controlsRow);
    }

    private static List<InlineKeyboardButton> createWeekDaysRow(LocalDate date, int shift) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        int day = date.getDayOfMonth();
        LocalDate callbackDate = date;
        for (int j = 0; j < shift; j++) {
            row.add(createInlineButton(IGNORE, IGNORE_TEXT));
        }
        for (int j = shift; j < 7; j++) {
            if (day <= (date.dayOfMonth().getMaximumValue())) {
                row.add(createInlineButton(format.print(callbackDate), Integer.toString(day++)));
                callbackDate = callbackDate.plusDays(1);
            } else {
                row.add(createInlineButton(IGNORE, " "));
            }
        }
        return row;
    }

    public static org.joda.time.LocalDate toJodaLocalDate(java.time.LocalDate input) {
        return new org.joda.time.LocalDate(input.getYear(),
                input.getMonthValue(),
                input.getDayOfMonth());
    }
}
