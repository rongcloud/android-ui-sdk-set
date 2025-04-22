package io.rong.imkit.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import java.lang.reflect.Method;

/** 导航栏工具类 */
public class NavigationBarUtil {
    // 核心方法：获取导航栏高度（无导航栏返回0）
    public static int getNavigationBarHeight(Context context) {
        if (context == null) {
            return 0;
        }
        if (!hasVirtualNavigationBar(context)) {
            return 0;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getModernHeight(context);
        } else {
            return getLegacyHeight(context);
        }
    }

    // 检测是否存在虚拟导航栏（综合判断）
    private static boolean hasVirtualNavigationBar(Context context) {
        // 1. 检测手势导航（小米/OPPO等全面屏手势）
        if (isGestureNavigationEnabled(context)) {
            return false;
        }

        // 2. 检测系统配置（通过系统属性判断）
        return checkSystemNavigationEnabled(context);
    }

    // Android 11+ 现代API获取
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.R)
    private static int getModernHeight(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            WindowMetrics metrics = wm.getCurrentWindowMetrics();
            WindowInsets insets = metrics.getWindowInsets();

            // 处理三星横屏导航栏右侧显示
            if (isSamsungLandscape(context)) {
                return Math.max(
                        insets.getInsets(WindowInsets.Type.navigationBars()).right,
                        insets.getInsets(WindowInsets.Type.navigationBars()).bottom);
            }
            return insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
        } catch (Exception e) {
            return 0;
        }
    }

    // 旧版本兼容方案
    private static int getLegacyHeight(Context context) {
        // 方案1：系统资源获取（华为/小米通用）
        int resHeight = getSystemResourceHeight(context);
        if (resHeight > 0) return resHeight;

        // 方案2：屏幕差值计算（三星等特殊机型）
        return calculateScreenDifference(context);
    }

    // 手势导航检测（小米/OPPO等）
    private static boolean isGestureNavigationEnabled(Context context) {
        try {
            // 通过系统配置检测（兼容大部分厂商）
            Class<?> cls = Class.forName("android.os.SystemProperties");
            Method getMethod = cls.getDeclaredMethod("get", String.class);
            String navMode = (String) getMethod.invoke(null, "ro.boot.navigation_mode");
            return "gesture".equals(navMode);
        } catch (Exception e) {
            return false;
        }
    }

    // 系统级导航栏启用检测
    private static boolean checkSystemNavigationEnabled(Context context) {
        try {
            Resources res = context.getResources();
            int resId = res.getIdentifier("config_showNavigationBar", "bool", "android");
            return resId > 0 && res.getBoolean(resId);
        } catch (Exception e) {
            return true; // 默认存在导航栏
        }
    }

    // 系统资源维度获取（华为/小米通用）
    private static int getSystemResourceHeight(Context context) {
        try {
            int resId =
                    context.getResources()
                            .getIdentifier("navigation_bar_height", "dimen", "android");
            return resId > 0 ? context.getResources().getDimensionPixelSize(resId) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // 屏幕差值计算（三星横屏特殊处理）
    private static int calculateScreenDifference(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();

            DisplayMetrics realMetrics = new DisplayMetrics();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealMetrics(realMetrics);
            }

            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);

            if (isLandscape(context)) {
                return realMetrics.widthPixels - displayMetrics.widthPixels;
            } else {
                return realMetrics.heightPixels - displayMetrics.heightPixels;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    // 三星横屏检测
    private static boolean isSamsungLandscape(Context context) {
        return Build.MANUFACTURER != null
                && Build.MANUFACTURER.toLowerCase().contains("samsung")
                && context.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE;
    }

    private static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }
}
