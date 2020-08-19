package ru.gadjini.telegram.converter.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RequestParams {

    public static final RequestParams EMPTY = new RequestParams();

    private Map<String, String> params = new HashMap<>();

    public String getString(String key) {
        return params.get(key);
    }

    public Integer getInt(String key) {
        return params.containsKey(key) ? Integer.parseInt(params.get(key)) : null;
    }

    public boolean getBoolean(String key) {
        return params.containsKey(key) && Boolean.parseBoolean(params.get(key));
    }

    public RequestParams add(String key, String value) {
        params.put(key, value);

        return this;
    }

    public RequestParams add(String key, Integer value) {
        params.put(key, String.valueOf(value));

        return this;
    }

    public RequestParams add(String key, Boolean value) {
        params.put(key, String.valueOf(value));

        return this;
    }

    public RequestParams merge(RequestParams requestParams) {
        requestParams.params.forEach((s, s2) -> params.putIfAbsent(s, s2));

        return this;
    }

    public boolean contains(String key) {
        return params.containsKey(key);
    }

    public String serialize(String delimiter) {
        StringBuilder serialize = new StringBuilder();

        params.forEach((s, s2) -> {
            if (serialize.length() > 0) {
                serialize.append(delimiter);
            }
            serialize.append(s).append(delimiter).append(s2);
        });

        return serialize.toString();
    }
}
