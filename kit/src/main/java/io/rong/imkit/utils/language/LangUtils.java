package io.rong.imkit.utils.language;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

import io.rong.imlib.RongIMClient;

/**
 * Created by weiqinxiao on 2018/2/27.
 */

public class LangUtils {
    private static final String LOCALE_CONF_FILE_NAME = "locale.config";
    private static final String APP_LOCALE = "app_locale";
    private static final String APP_PUSH_LANGUAGE = "app_push_language";
    private static Locale systemLocale = Locale.getDefault();

    public static Context getConfigurationContext(Context context) {
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        Context configurationContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = new LocaleList(getAppLocale(context).toLocale());
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
            configurationContext = context.createConfigurationContext(config);
        }
        return configurationContext;
    }

    public static RCLocale getAppLocale(Context context) {
        SharedPreferences sp
                = context.getSharedPreferences(LOCALE_CONF_FILE_NAME, Context.MODE_PRIVATE);
        String locale = sp.getString(APP_LOCALE, "auto");
        return RCLocale.valueOf(locale);
    }

    public static void saveLocale(Context context, RCLocale locale) {
        SharedPreferences sp
                = context.getSharedPreferences(LOCALE_CONF_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(APP_LOCALE, locale.value()).commit();
    }

    public static RongIMClient.PushLanguage getPushLanguage(Context context) {
        SharedPreferences sp
                = context.getSharedPreferences(LOCALE_CONF_FILE_NAME, Context.MODE_PRIVATE);
        //默认值为空串  证明没有设置过推送语言
        String language = sp.getString(APP_PUSH_LANGUAGE, "");
        if ((RongIMClient.PushLanguage.ZH_CN.getMsg().equals(language))) {
            return RongIMClient.PushLanguage.ZH_CN;
        } else if ((RongIMClient.PushLanguage.EN_US.getMsg().equals(language))) {
            return RongIMClient.PushLanguage.EN_US;
        } else {
            return null;
        }
    }

    public static void setPushLanguage(Context context, RongIMClient.PushLanguage pushLanguage) {
        SharedPreferences sp
                = context.getSharedPreferences(LOCALE_CONF_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(APP_PUSH_LANGUAGE, pushLanguage.getMsg()).commit();
    }


    /**
     * 可选择的语言的包装类
     */
    public static class RCLocale {
        /**
         * 中文
         */
        public static final RCLocale LOCALE_CHINA = new RCLocale("zh");
        /**
         * 英文
         */
        public static final RCLocale LOCALE_US = new RCLocale("en");
        /**
         * 阿拉伯
         */
        public static final RCLocale LOCALE_ARAB = new RCLocale("ar");
        /**
         * 跟随系统
         */
        public static final RCLocale LOCALE_AUTO = new RCLocale("auto");

        private String rcLocale;

        private RCLocale(String rcLocale) {
            this.rcLocale = rcLocale;
        }

        public String value() {
            return rcLocale;
        }

        public Locale toLocale() {
            Locale locale;
            if (rcLocale.equals(LOCALE_CHINA.value())) {
                locale = Locale.CHINESE;
            } else if (rcLocale.equals(LOCALE_US.value())) {
                locale = Locale.ENGLISH;
            } else if (rcLocale.equals(LOCALE_ARAB.value())) {
                locale = new Locale("ar");
            } else {
                locale = getSystemLocale();
            }
            return locale;
        }

        public static RCLocale valueOf(String rcLocale) {
            RCLocale locale;
            if (LOCALE_CHINA.value().equals(rcLocale)) {
                locale = LOCALE_CHINA;
            } else if (LOCALE_US.value().equals(rcLocale)) {
                locale = LOCALE_US;
            } else if (LOCALE_ARAB.value().equals(rcLocale)) {
                locale = LOCALE_ARAB;
            } else {
                locale = LOCALE_AUTO;
            }
            return locale;
        }
    }


    /**
     * 获取系统语言
     *
     * @return 系统语言
     */
    public static Locale getSystemLocale() {
        return systemLocale;
    }

    /**
     * 设置系统语言
     *
     * @param locale 设置的系统语音
     */
    public static void setSystemLocale(Locale locale) {
        systemLocale = locale;
    }

    /**
     * 获取当前语言,不管有没有设置或跟随系统
     *
     * @param context 上下文
     * @return 当前语言
     */
    public static RCLocale getCurrentLanguage(Context context) {
        SharedPreferences sp
                = context.getSharedPreferences(LOCALE_CONF_FILE_NAME, Context.MODE_PRIVATE);
        String locale = sp.getString(APP_LOCALE, "auto");
        if (("auto").equals(locale)) {
            return getSystemLocale().toString().equals("zh_CN") ? RCLocale.LOCALE_CHINA : RCLocale.LOCALE_US;
        }
        return RCLocale.valueOf(locale);
    }
}
