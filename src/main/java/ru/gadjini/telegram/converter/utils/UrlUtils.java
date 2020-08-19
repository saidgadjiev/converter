package ru.gadjini.telegram.converter.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

    private static final Pattern URL_CHECK = Pattern.compile("^(http://www\\.|https://www\\.|http://|https://)?[a-z0-9]+([\\-.][a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(/.*)?$");

    private UrlUtils() {}

    public static String appendScheme(String url) {
        if (UrlUtils.hasScheme(url)) {
            return url;
        }
        if (url.contains(":443")) {
            return "https://" + url;
        }

        return "http://" + url;
    }

    public static boolean isUrl(String url) {
        Matcher matcher = URL_CHECK.matcher(url);

        return matcher.matches();
    }

    public static boolean hasScheme(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        return url.startsWith("http://") || url.startsWith("https://");
    }
}
