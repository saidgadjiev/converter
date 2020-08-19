package ru.gadjini.telegram.converter.utils;

import java.util.ArrayList;
import java.util.Collection;

public class ReflectionUtils {

    public static<I, A> Collection<A> findImplements(Collection<I> impls, Class<A> api) {
        Collection<A> result = new ArrayList<>();

        for (Object impl: impls) {
            if (api.isAssignableFrom(impl.getClass())) {
                result.add((A) impl);
            }
        }

        return result;
    }
}
