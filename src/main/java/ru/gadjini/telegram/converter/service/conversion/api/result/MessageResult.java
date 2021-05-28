package ru.gadjini.telegram.converter.service.conversion.api.result;

public class MessageResult implements ConversionResult {

    private String text;

    private String parseMode;

    public MessageResult(String text, String parseMode) {
        this.text = text;
        this.parseMode = parseMode;
    }

    public String getText() {
        return text;
    }

    public String getParseMode() {
        return parseMode;
    }

    @Override
    public ResultType resultType() {
        return ResultType.MESSAGE;
    }
}
