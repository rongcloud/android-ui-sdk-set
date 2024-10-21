package io.rong.imkit.utils;

import android.text.TextUtils;

/** Created by weiqinxiao on 15/12/28. */
public class StringUtils {
    private static final String SEPARATOR = "#@6RONG_CLOUD9@#";

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

    /**
     * 获取字符的拼音首字母，如果是汉字则返回拼音首字母，大写形式；否则直接返回字符本身的大写形式。
     *
     * @param c 字符
     * @return 拼音首字母或字符本身的大写形式
     */
    public static char getFirstChar(char c) {
        if (c >= 'a' && c <= 'z') {
            return (char) (c - 'a' + 'A');
        }
        if (c >= 'A' && c <= 'Z') {
            return c;
        }

        c = CharacterParser.getInstance().convert(String.valueOf(c)).charAt(0);

        if (c >= 'a' && c <= 'z') {
            return (char) (c - 'a' + 'A');
        }
        if (c >= 'A' && c <= 'Z') {
            return c;
        }

        // 非汉字字符，直接返回大写形式
        return Character.toUpperCase('#');
    }
}
