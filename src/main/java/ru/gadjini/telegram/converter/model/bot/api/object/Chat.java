package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Chat {

    private static final String ID_FIELD = "id";
    @JsonProperty(ID_FIELD)
    private Long id;

    public Chat() {
        super();
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "id=" + id +
                '}';
    }
}
