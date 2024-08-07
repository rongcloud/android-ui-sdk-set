package io.rong.imkit.picture.tools;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import io.rong.common.rlog.RLog;
import java.lang.reflect.Field;

public class ScreenUtils {
    private static final String TAG = ScreenUtils.class.getSimpleName();

    /** dp2px */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int getScreenWidth(Context context) {
        if (!(context instanceof Activity)) {
            return 0;
        }
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(localDisplayMetrics);
        return localDisplayMetrics.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        if (!(context instanceof Activity)) {
            return 0;
        }
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(localDisplayMetrics);
        return localDisplayMetrics.heightPixels - getStatusBarHeight(context);
    }

    public static int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        if (context == null) {
            return 0;
        }
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object o = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = (Integer) field.get(o);
            statusBarHeight =
                    context.getApplicationContext().getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
        return statusBarHeight == 0 ? dip2px(context, 25) : statusBarHeight;
    }
}
