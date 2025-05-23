package io.rong.imkit.utils.language;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.view.ContextThemeWrapper;
import io.rong.common.SystemUtils;
import io.rong.common.rlog.RLog;
import io.rong.imlib.RongIMClient;
import java.util.Locale;

/** Created by CaoHaiyang on 2017/9/25. */
public class RongConfigurationManager {
    private static final String TAG = RongConfigurationManager.class.getSimpleName();
    private static String RONG_CONFIG = "RongKitConfiguration";
    private static String FILE_MAX_SIZE = "FileMaxSize";
    private static boolean isInit = false;

    private RongConfigurationManager() {
        // default implementation ignored
    }

    private static class SingletonHolder {
        static RongConfigurationManager sInstance = new RongConfigurationManager();
    }

    /** 监听系统语言的切换，避免应用语言根随系统语言的切换而切换 */
    private static class SystemConfigurationChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
                LangUtils.setSystemLocale(Locale.getDefault());
                LangUtils.RCLocale appLocale = LangUtils.getAppLocale(context);
                Locale systemLocale = LangUtils.getSystemLocale();
                if (!appLocale.toLocale().equals(systemLocale)) {
                    RongConfigurationManager.getInstance().switchLocale(appLocale, context);
                }
            }
        }
    }

    public static RongConfigurationManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public static void init(Context context) {
        if (!isInit) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            SystemUtils.registerReceiverCompat(
                    context, new SystemConfigurationChangedReceiver(), filter);

            // 初始化时将应用语言重新设置为之前设置的语言
            LangUtils.RCLocale locale =
                    RongConfigurationManager.getInstance().getAppLocale(context);
            RongConfigurationManager.getInstance().switchLocale(locale, context);
            isInit = true;
        }
    }

    /**
     * 设置发送文件时,支持发送的文件最大值。
     *
     * @param context 上下文
     * @param size 支持发送的文件最大值,单位兆。
     */
    public void setFileMaxSize(Context context, int size) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(RONG_CONFIG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(FILE_MAX_SIZE, size).apply();
    }

    /**
     * 获取发送文件时支持的文件最大值。
     *
     * @param context 上下文
     * @return 支持发送的文件最大值, 单位兆。
     */
    public int getFileMaxSize(Context context) {
        if (context == null) {
            return 100;
        }
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(RONG_CONFIG, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(FILE_MAX_SIZE, 100);
    }

    /**
     * 用于切换语言
     *
     * @param locale 可传入的值为RCLocale.LOCALE_CHINA、RCLocale.LOCALE_US 和 RCLocale.AUTO
     */
    public void switchLocale(LangUtils.RCLocale locale, Context context) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale.toLocale();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            context.getResources().updateConfiguration(config, resources.getDisplayMetrics());
        }
        LangUtils.saveLocale(context, locale);
    }

    /**
     * 生成 ConfigurationContext，在 attachBaseContext 方法中替换
     *
     * @param newBase 上下文
     * @return ConfigurationContext
     */
    public Context getConfigurationContext(Context newBase) {
        Context context = LangUtils.getConfigurationContext(newBase);
        final Configuration configuration = context.getResources().getConfiguration();
        try {
            return new ContextThemeWrapper(
                    context, androidx.appcompat.R.style.Theme_AppCompat_Empty) {
                @Override
                public void applyOverrideConfiguration(Configuration overrideConfiguration) {
                    if (overrideConfiguration != null) {
                        overrideConfiguration.setTo(configuration);
                    }
                    super.applyOverrideConfiguration(overrideConfiguration);
                }
            };
        } catch (Exception e) {
            RLog.e(TAG, "getConfigurationContext e : ", e);
            return context;
        }
    }

    /**
     * 获取应用内设置的语言
     *
     * @return 应用内设置的语言
     */
    public LangUtils.RCLocale getAppLocale(Context context) {
        return LangUtils.getAppLocale(context);
    }

    /**
     * 获取系统语言
     *
     * @return 系统语言
     */
    public Locale getSystemLocale() {
        return LangUtils.getSystemLocale();
    }

    /**
     * 获取推送语言
     *
     * @return 推送语言
     */
    public RongIMClient.PushLanguage getPushLanguage(Context context) {
        return LangUtils.getPushLanguage(context);
    }

    /**
     * 设置推送语言
     *
     * @param pushLanguage 推送语言
     */
    public void setPushLanguage(Context context, RongIMClient.PushLanguage pushLanguage) {
        LangUtils.setPushLanguage(context, pushLanguage);
    }

    /**
     * 获取当前app 的语言设置
     *
     * @return
     */
    public LangUtils.RCLocale getLanguageLocal(Context context) {
        LangUtils.RCLocale appLocale = RongConfigurationManager.getInstance().getAppLocale(context);
        if (appLocale == LangUtils.RCLocale.LOCALE_AUTO) {
            Locale systemLocale = RongConfigurationManager.getInstance().getSystemLocale();
            if (systemLocale.getLanguage().equals(Locale.CHINESE.getLanguage())) {
                appLocale = LangUtils.RCLocale.LOCALE_CHINA;
            } else if (systemLocale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                appLocale = LangUtils.RCLocale.LOCALE_US;
            } else if (systemLocale.getLanguage().equals(new Locale("ar").getLanguage())) {
                appLocale = LangUtils.RCLocale.LOCALE_ARAB;
            } else {
                appLocale = LangUtils.RCLocale.LOCALE_CHINA;
            }
        }
        return appLocale;
    }
}
