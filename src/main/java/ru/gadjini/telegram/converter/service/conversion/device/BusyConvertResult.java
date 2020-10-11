package ru.gadjini.telegram.converter.service.conversion.device;

import ru.gadjini.telegram.converter.service.conversion.api.result.ConvertResult;
import ru.gadjini.telegram.converter.service.conversion.api.result.ResultType;

public class BusyConvertResult implements ConvertResult {
    @Override
    public ResultType resultType() {
        return ResultType.BUSY;
    }

    @Override
    public void close() throws Exception {

    }
}
