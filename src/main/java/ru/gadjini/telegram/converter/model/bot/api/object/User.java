package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

    private static final String ID_FIELD = "id";
    private static final String USERNAME_FIELD = "username";
    private static final String LANGUAGECODE_FIELD = "language_code";

    @JsonProperty(ID_FIELD)
    private Integer id;
    @JsonProperty(USERNAME_FIELD)
    private String userName;
    @JsonProperty(LANGUAGECODE_FIELD)
    private String languageCode;

    public User() {
        super();
    }

    public Integer getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", languageCode='" + languageCode + '\'' +
                '}';
    }
}
