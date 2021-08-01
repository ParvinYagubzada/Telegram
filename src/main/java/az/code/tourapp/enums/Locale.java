package az.code.tourapp.enums;

import lombok.Getter;

@Getter
public enum Locale {
    AZ(java.util.Locale.forLanguageTag("az")),
    EN(java.util.Locale.forLanguageTag("en")),
    RU(java.util.Locale.forLanguageTag("ru"));

    public static Locale DEFAULT = EN;

    java.util.Locale javaLocale;

    Locale(java.util.Locale javaLocale) {
        this.javaLocale = javaLocale;
    }
}
