package ru.gadjini.telegram.converter.service.conversion.api.result;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class MessageResult implements ConversionResult {

    private SendMessage sendMessage;

    private boolean deleteSrcFiles;

    public MessageResult(SendMessage sendMessage, boolean deleteSrcFiles) {
        this.sendMessage = sendMessage;
        this.deleteSrcFiles = deleteSrcFiles;
    }

    public SendMessage getSendMessage() {
        return sendMessage;
    }

    @Override
    public boolean deleteSrcFiles() {
        return deleteSrcFiles;
    }

    @Override
    public ResultType resultType() {
        return ResultType.MESSAGE;
    }
}
