package io.rong.imkit.picture.tools;

import android.content.Context;
import android.widget.Toast;

public final class ToastUtils {
    public static void s(Context mContext, String s) {
        if (mContext == null) {
            return;
        }
        Toast.makeText(mContext.getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }
}
