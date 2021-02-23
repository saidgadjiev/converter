package ru.gadjini.telegram.converter.service.conversion.api.result;

import java.util.ArrayList;
import java.util.List;

public class ConvertResults implements ConversionResult {

    private List<ConversionResult> convertResults = new ArrayList<>();

    public void addResult(ConversionResult convertResult) {
        convertResults.add(convertResult);
    }

    public List<ConversionResult> getConvertResults() {
        return convertResults;
    }

    @Override
    public ResultType resultType() {
        return ResultType.CONTAINER;
    }
}
