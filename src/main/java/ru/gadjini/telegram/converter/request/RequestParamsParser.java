package ru.gadjini.telegram.converter.request;

import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RequestParamsParser {

    public RequestParams parse(String argsPart) {
        RequestParams requestParams = new RequestParams();

        List<List<String>> partitions = Lists.partition(Arrays.asList(argsPart.split("=")), 2);

        for (List<String> partition: partitions) {
            requestParams.add(partition.get(0), partition.get(1));
        }

        return requestParams;
    }
}
