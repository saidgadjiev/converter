package ru.gadjini.telegram.converter.exception.botapi;

import ru.gadjini.telegram.converter.model.ApiResponse;


public class TelegramApiRequestException extends TelegramApiException {
    private String chatId;
    private String apiResponse = null;
    private Integer errorCode = 0;

    public TelegramApiRequestException(String message) {
        super(message);
    }

    public TelegramApiRequestException(String chatId, String message, ApiResponse response) {
        super(buildMessage(chatId, message, response));
        this.chatId = chatId;
        apiResponse = response.getErrorDescription();
        errorCode = response.getErrorCode();
    }

    public TelegramApiRequestException(String chatId, String message, Throwable cause) {
        super(buildMessage(chatId, message), cause);
        this.chatId = chatId;
    }

    public String getApiResponse() {
        return apiResponse;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getChatId() {
        return chatId;
    }

    public long getLongChatId() {
        return Long.parseLong(chatId);
    }

    private static String buildMessage(String chatId, String message) {
        StringBuilder msg = new StringBuilder();
        msg.append("(").append(chatId).append(") ").append(message);

        return msg.toString();
    }

    private static String buildMessage(String chatId, String message, ApiResponse response) {
        StringBuilder msg = new StringBuilder();
        msg.append("(").append(chatId).append(") ").append(message).append("\n").append(response.getErrorCode()).append(" ")
                .append(response.getErrorDescription());

        return msg.toString();
    }

    @Override
    public String toString() {
        if (apiResponse == null) {
            return super.toString();
        } else if (errorCode == null) {
            return super.toString() + ": " + apiResponse;
        } else {
            return super.toString() + ": [" + errorCode + "] " + apiResponse;
        }
    }
}
