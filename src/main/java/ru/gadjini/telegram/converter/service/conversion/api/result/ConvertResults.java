package ru.gadjini.telegram.converter.service.conversion.api.result;

import java.util.ArrayList;
import java.util.List;

public class ConvertResults implements ConvertResult {

    private List<ConvertResult> convertResults = new ArrayList<>();

    public void addResult(ConvertResult convertResult) {
        convertResults.add(convertResult);
    }

    public List<ConvertResult> getConvertResults() {
        return convertResults;
    }

    @Override
    public ResultType resultType() {
        return ResultType.CONTAINER;
    }
}
