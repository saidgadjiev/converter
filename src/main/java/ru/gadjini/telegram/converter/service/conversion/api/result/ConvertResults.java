package ru.gadjini.telegram.converter.service.conversion.api.result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ConvertResults implements ConvertResult {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertResults.class);

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
