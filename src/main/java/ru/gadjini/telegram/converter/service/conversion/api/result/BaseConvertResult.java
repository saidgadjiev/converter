package ru.gadjini.telegram.converter.service.conversion.api.result;

public abstract class BaseConvertResult implements ConvertResult {

    private long time;

    BaseConvertResult(long time) {
        this.time = time;
    }

    @Override
    public final long time() {
        return time;
    }
}
