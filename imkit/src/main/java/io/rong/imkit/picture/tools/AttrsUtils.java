package io.rong.imkit.picture.tools;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import io.rong.common.RLog;

public class AttrsUtils {

    private static final String TAG = AttrsUtils.class.getSimpleName();

    /**
     * get attrs color
     *
     * @param context
     * @param attr
     * @return
     */
    public static int getTypeValueColor(Context context, int attr) {
        try {
            TypedValue typedValue = new TypedValue();
            int[] attribute = new int[] {attr};
            TypedArray array = context.obtainStyledAttributes(typedValue.resourceId, attribute);
            int color = array.getColor(0, 0);
            array.recycle();
            return color;
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
        return 0;
    }

    /**
     * attrs status color or black
     *
     * @param context
     * @param attr
     * @return
     */
    public static boolean getTypeValueBoolean(Context context, int attr) {
        try {
            TypedValue typedValue = new TypedValue();
            int[] attribute = new int[] {attr};
            TypedArray array = context.obtainStyledAttributes(typedValue.resourceId, attribute);
            boolean statusFont = array.getBoolean(0, false);
            array.recycle();
            return statusFont;
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
        return false;
    }

    /**
     * attrs drawable
     *
     * @param context
     * @param attr
     * @return
     */
    public static Drawable getTypeValueDrawable(Context context, int attr) {
        try {
            TypedValue typedValue = new TypedValue();
            int[] attribute = new int[] {attr};
            TypedArray array = context.obtainStyledAttributes(typedValue.resourceId, attribute);
            Drawable drawable = array.getDrawable(0);
            array.recycle();
            return drawable;
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
        return null;
    }
}
