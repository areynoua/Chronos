package com.reynouard.alexis.chronos.utils;

import java.util.List;

public class StringUtils {
    private StringUtils() {
    }

    public static int countOccurrencesOf(CharSequence of, String in) {
        return in.length() - in.replace(of, "").length();
    }

    public static String join(String glue, String[] strings) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strings.length - 1; ++i) {
            builder.append(strings[i]);
            builder.append(glue);
        }
        builder.append(strings[strings.length - 1]);
        return builder.toString();
    }

    public static String join(String glue, List<String> strings) {
        return join(glue, strings.toArray(new String[strings.size()]));
    }
}
