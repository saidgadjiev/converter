package ru.gadjini.telegram.converter.service.conversion.api.result;

public interface ConversionResult {

    ResultType resultType();

    default boolean deleteSrcFiles() {
        return true;
    }
}
