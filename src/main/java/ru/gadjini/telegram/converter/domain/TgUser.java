package ru.gadjini.telegram.converter.domain;

import java.util.Locale;

public class TgUser {

    public static final String TYPE = "tg_user";

    public static final String USER_ID = "user_id";

    public static final String USERNAME ="username";

    public static final String LOCALE = "locale";

    public static final String ORIGINAL_LOCALE = "original_locale";

    private int userId;

    private String username;

    private String locale;

    private String originalLocale;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getLanguageCode() {
        return locale;
    }

    public Locale getLocale() {
        return new Locale(locale);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOriginalLocale() {
        return originalLocale;
    }

    public void setOriginalLocale(String originalLocale) {
        this.originalLocale = originalLocale;
    }
}
