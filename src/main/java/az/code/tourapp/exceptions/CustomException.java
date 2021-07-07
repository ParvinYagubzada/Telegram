package az.code.tourapp.exceptions;

import az.code.tourapp.enums.Locale;

public interface CustomException {
    String getText(Locale locale);
}
