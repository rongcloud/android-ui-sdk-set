package io.rong.imkit.picture.tools;

import android.content.Context;
import android.widget.Toast;

/**
 * @deprecated 请使用 {@link io.rong.imkit.utils.ToastUtils}
 * @author rongcloud
 */
public final class ToastUtils {
    public static void s(Context mContext, String s) {
        io.rong.imkit.utils.ToastUtils.show(mContext, s, Toast.LENGTH_SHORT);
    }
}
