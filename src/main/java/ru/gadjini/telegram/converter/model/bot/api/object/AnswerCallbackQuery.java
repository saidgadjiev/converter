package ru.gadjini.telegram.converter.model.bot.api.object;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnswerCallbackQuery {

    public static final String METHOD = "answercallbackquery";

    private static final String CALLBACKQUERYID_FIELD = "callback_query_id";

    private static final String TEXT_FIELD = "text";

    private static final String SHOW_ALERT = "show_alert";

    @JsonProperty(CALLBACKQUERYID_FIELD)
    private String callbackQueryId;

    @JsonProperty(TEXT_FIELD)
    private String text;

    @JsonProperty(SHOW_ALERT)
    private Boolean showAlert;

    public AnswerCallbackQuery() {}

    public AnswerCallbackQuery(String callbackQueryId, String text) {
        this.callbackQueryId = callbackQueryId;
        this.text = text;
    }

    public AnswerCallbackQuery(String callbackQueryId, String text, boolean showAlert) {
        this.callbackQueryId = callbackQueryId;
        this.text = text;
        this.showAlert = showAlert;
    }

    public String getCallbackQueryId() {
        return this.callbackQueryId;
    }

    public String getText() {
        return this.text;
    }

    public void setCallbackQueryId(String callbackQueryId) {
        this.callbackQueryId = callbackQueryId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getShowAlert() {
        return showAlert;
    }

    public void setShowAlert(Boolean showAlert) {
        this.showAlert = showAlert;
    }

    @Override
    public String toString() {
        return "AnswerCallbackQuery{" +
                "callbackQueryId='" + callbackQueryId + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
