package io.rong.imkit.utils;


import android.content.Context;

import java.lang.reflect.Method;

import io.rong.common.RLog;


/**
 * 判断融云SDK IPluginModule点击或者消息点击是否允许操作
 */
public class RongOperationPermissionUtils {
    private static final String TAG = "RongOperationPermissionUtils";

    /**
     * 是否允许录制语音消息、播放语音消息等的操作。
     * 如果正在VOIP通话过程返回false
     *
     * @param context 上下文
     * @return 是否允许操作
     */
    public static boolean isMediaOperationPermit(Context context) {
        try {
            String clazzName = "io.rong.callkit.RongCallKit";
            Class<?> voipclazz = Class.forName(clazzName);
            Method method = voipclazz.getMethod("isInVoipCall", Context.class);
            boolean isInVoipCall = (boolean) method.invoke(null, context);
            if (isInVoipCall) return false;
        } catch (Exception e) {
            RLog.e(TAG, "isMediaOperationPermit", e);
        }
        return true;
    }
}
