package ru.gadjini.telegram.converter.service.conversion.device;

import ru.gadjini.telegram.converter.service.conversion.api.result.ConversionResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ResultType;

public class BusyConvertResult implements ConversionResult {
    @Override
    public ResultType resultType() {
        return ResultType.BUSY;
    }
}
