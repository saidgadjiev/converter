package ru.gadjini.telegram.converter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResponse<T> {
    private static final String OK_FIELD = "ok";
    private static final String ERROR_CODE_FIELD = "error_code";
    private static final String DESCRIPTION_CODE_FIELD = "description";
    private static final String RESULT_FIELD = "result";

    @JsonProperty(OK_FIELD)
    public Boolean ok;
    @JsonProperty(ERROR_CODE_FIELD)
    public Integer errorCode;
    @JsonProperty(DESCRIPTION_CODE_FIELD)
    public String errorDescription;
    @JsonProperty(RESULT_FIELD)
    public T result;

    public Boolean getOk() {
        return ok;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public T getResult() {
        return result;
    }

    @Override
    public String toString() {
        if (ok) {
            return "ApiResponse{" +
                    "ok=" + ok +
                    ", result=" + result +
                    '}';
        } else {
            return "ApiResponse{" +
                    "ok=" + ok +
                    ", errorCode=" + errorCode +
                    ", errorDescription='" + errorDescription + '\'' +
                    '}';
        }
    }
}
