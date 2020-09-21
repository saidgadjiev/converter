package ru.gadjini.telegram.converter.service.conversion.api.result;

public interface ConvertResult extends AutoCloseable {

    ResultType resultType();
}
