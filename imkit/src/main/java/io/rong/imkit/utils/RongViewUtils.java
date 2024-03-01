package io.rong.imkit.utils;

import android.view.View;
import android.view.ViewGroup;
import io.rong.common.RLog;

public class RongViewUtils {

    /** 安全的addView */
    public static void addView(ViewGroup viewGroup, View addedView) {
        addView(viewGroup, addedView, -1);
    }

    /** 安全的addView */
    public static void addView(ViewGroup viewGroup, View addedView, int index) {
        if (addedView == null || viewGroup == null) {
            return;
        }
        try {
            if (addedView.getParent() != null) {
                ((ViewGroup) addedView.getParent()).removeView(addedView);
            }
            viewGroup.addView(addedView, index);
        } catch (Exception e) {
            RLog.d("RongViewUtils", "addView e:" + e);
        }
    }
}
