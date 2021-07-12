package az.code.tourapp.utils;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalendarUtil {

    public static final String IGNORE = "ignore";

    public static final String[] WD = {"M", "T", "W", "T", "F", "S", "S"};
    public static DateTimeFormatter format = DateTimeFormat.forPattern("dd.MM.yyyy");
    public static DateTimeFormatter headerFormat = DateTimeFormat.forPattern("MMMM yyyy");

    public static InlineKeyboardMarkup generateKeyboard(java.time.LocalDate input, Locale locale) {
        System.out.println("Inside generate keyboard");
        LocalDate date = toJoda(input);
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        // row - Month and Year
        List<InlineKeyboardButton> headerRow = new ArrayList<>();
        headerRow.add(createButton(IGNORE, headerFormat.withLocale(locale).print(date)));
        keyboard.add(headerRow);

        // row - Days of the week
        List<InlineKeyboardButton> daysOfWeekRow = new ArrayList<>();
        for (String day : WD) {
            daysOfWeekRow.add(createButton(IGNORE, day));
        }
        keyboard.add(daysOfWeekRow);

        LocalDate firstDay = date.dayOfMonth().withMinimumValue();

        int shift = firstDay.dayOfWeek().get() - 1;
        int daysInMonth = firstDay.dayOfMonth().getMaximumValue();
        int rows = ((daysInMonth + shift) % 7 > 0 ? 1 : 0) + (daysInMonth + shift) / 7;
        for (int i = 0; i < rows; i++) {
            keyboard.add(buildRow(firstDay, shift));
            firstDay = firstDay.plusDays(7 - shift);
            shift = 0;
        }
        List<InlineKeyboardButton> controlsRow = new ArrayList<>();
        controlsRow.add(createButton("<" + format.print(date), "<"));
        controlsRow.add(createButton(">" + format.print(date), ">"));
        keyboard.add(controlsRow);
        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }

    private static InlineKeyboardButton createButton(String callBack, String text) {
        return InlineKeyboardButton.builder().callbackData(callBack).text(text).build();
    }

    private static List<InlineKeyboardButton> buildRow(LocalDate date, int shift) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        int day = date.getDayOfMonth();
        LocalDate callbackDate = date;
        for (int j = 0; j < shift; j++) {
            row.add(createButton(IGNORE, " "));
        }
        for (int j = shift; j < 7; j++) {
            if (day <= (date.dayOfMonth().getMaximumValue())) {
                row.add(createButton(format.print(callbackDate), Integer.toString(day++)));
                callbackDate = callbackDate.plusDays(1);
            } else {
                row.add(createButton(IGNORE, " "));
            }
        }
        return row;
    }

    public static org.joda.time.LocalDate toJoda(java.time.LocalDate input) {
        return new org.joda.time.LocalDate(input.getYear(),
                input.getMonthValue(),
                input.getDayOfMonth());
    }
}
