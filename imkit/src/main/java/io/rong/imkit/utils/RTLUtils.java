package io.rong.imkit.utils;

import android.text.TextUtils;
import android.view.View;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 在某些语言下，处理RTL模式工具类 */
public class RTLUtils {

    private static final String AIT = "@";
    private static final Pattern pLetter = Pattern.compile("[a-zA-Z0-9]");
    private static final Pattern pChinese = Pattern.compile("[\u4e00-\u9fa5]");

    public static String adapterAitInRTL(String str) {
        return getRTLCode(str, AIT) + str;
    }

    public static String getRTLCode(String str, String first) {
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                != View.LAYOUT_DIRECTION_RTL) {
            return "";
        }
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        // @字符前需添加"\u200e"(LRM)或"\u200f"(RLM)表明方向，否则RLT下多种文字混排有问题
        if (str.length() < 2) {
            return "";
        }
        String[] splitStr = str.split("");
        if (splitStr.length < 2) {
            return "";
        }
        // 判断首个字符，空则跳过
        if (!TextUtils.isEmpty(first) && !splitStr[0].equals(first)) {
            return "";
        }
        // 判断第二个字符
        if (isChineseOrLetter(splitStr[1])) {
            return "\u200e";
        } else {
            return "\u200f";
        }
    }

    public static boolean isChineseOrLetter(String txt) {
        // 英文字符和数字
        Matcher mLetter = pLetter.matcher(txt);
        if (mLetter.matches()) {
            return true;
        }
        // 中文字符
        Matcher mChinese = pChinese.matcher(txt);
        return mChinese.matches();
    }
}
