package io.rong.imkit.utils;

import android.text.TextUtils;

/**
 * Created by weiqinxiao on 15/12/28.
 */
public class StringUtils {
    private final static String SEPARATOR = "#@6RONG_CLOUD9@#";

    public static String getKey(String arg1, String arg2) {
        return arg1 + SEPARATOR + arg2;
    }

    public static String getArg1(String key) {
        String arg = null;
        if (key.contains(SEPARATOR)) {
            int index = key.indexOf(SEPARATOR);
            arg = key.substring(0, index);
        }
        return arg;
    }

    public static String getArg2(String key) {
        String arg = null;
        if (key.contains(SEPARATOR)) {
            int index = key.indexOf(SEPARATOR) + SEPARATOR.length();
            arg = key.substring(index);
        }
        return arg;
    }
    public static String getStringNoBlank(String str) {
        if (!TextUtils.isEmpty(str)) {
            return str.replaceAll("\\s", " ");
        } else {
            return str;
        }
    }
}
