package io.rong.imkit.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.core.app.AppOpsManagerCompat;
import androidx.fragment.app.Fragment;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Created by jiangecho on 2016/10/25. */
public class PermissionCheckUtil {
    private static final String TAG = PermissionCheckUtil.class.getSimpleName();
    public static final int REQUEST_CODE_ASK_PERMISSIONS = 100;
    public static final int REQUEST_CODE_LOCATION_SHARE = 101;
    private static IRequestPermissionListListener listener;
    private static final String PROMPT = "prompt";
    private static final String IS_PROMPT = "isPrompt";

    public static boolean requestPermissions(Fragment fragment, String[] permissions) {
        return requestPermissions(fragment, permissions, 0);
    }

    public static boolean requestPermissions(
            final Fragment fragment, String[] permissions, final int requestCode) {
        if (permissions.length == 0) {
            return true;
        }

        final List<String> permissionsNotGranted = new ArrayList<>();
        boolean result = false;

        for (String permission : permissions) {
            //            if ((isFlyme() || Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            //                    && permission.equals(Manifest.permission.RECORD_AUDIO)) {
            //                final SharedPreferences sharedPreferences =
            //                        fragment.getContext().getSharedPreferences(PROMPT,
            // Context.MODE_PRIVATE);
            //                boolean isPrompt = sharedPreferences.getBoolean(IS_PROMPT, true);
            //                if (isPrompt) {
            //                    showPermissionAlert(
            //                            fragment.getContext(),
            //                            fragment.getString(R.string.rc_permission_grant_needed)
            //                                    +
            // fragment.getString(R.string.rc_permission_microphone),
            //                            new DialogInterface.OnClickListener() {
            //                                @Override
            //                                public void onClick(DialogInterface dialog, int which)
            // {
            //                                    if (DialogInterface.BUTTON_POSITIVE == which) {
            //                                        fragment.startActivity(
            //                                                new Intent(
            //                                                        Settings
            //
            // .ACTION_MANAGE_APPLICATIONS_SETTINGS));
            //                                    } else if (DialogInterface.BUTTON_NEUTRAL ==
            // which) {
            //                                        SharedPreferences.Editor editor =
            //                                                sharedPreferences
            //                                                        .edit()
            //                                                        .putBoolean(IS_PROMPT, false);
            //                                        editor.commit();
            //                                    }
            //                                }
            //                            });
            //                }
            //                return false;
            //            }
            if (!hasPermission(fragment.getActivity(), permission)) {
                permissionsNotGranted.add(permission);
            }
        }

        if (permissionsNotGranted.size() > 0) {
            final int size = permissionsNotGranted.size();
            if (listener != null) {
                listener.onRequestPermissionList(
                        fragment.getActivity(),
                        permissionsNotGranted,
                        new IPermissionEventCallback() {
                            @Override
                            public void confirmed() {
                                fragment.requestPermissions(
                                        permissionsNotGranted.toArray(new String[size]),
                                        requestCode);
                            }

                            @Override
                            public void cancelled() {
                                // do nothing
                            }
                        });
            } else {
                fragment.requestPermissions(
                        permissionsNotGranted.toArray(new String[size]), requestCode);
            }
        } else {
            result = true;
        }
        return result;
    }

    public static boolean requestPermissions(
            final Activity activity, @NonNull String[] permissions) {
        return requestPermissions(activity, permissions, 0);
    }

    @TargetApi(23)
    public static boolean requestPermissions(
            final Activity activity, @NonNull final String[] permissions, final int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (permissions.length == 0) {
            return true;
        }

        final List<String> permissionsNotGranted = new ArrayList<>();
        boolean result = false;

        for (String permission : permissions) {
            if (!hasPermission(activity, permission)) {
                permissionsNotGranted.add(permission);
            }
        }

        if (permissionsNotGranted.size() > 0) {
            final int size = permissionsNotGranted.size();
            if (listener != null) {
                listener.onRequestPermissionList(
                        activity,
                        permissionsNotGranted,
                        new IPermissionEventCallback() {
                            @Override
                            public void confirmed() {
                                if (activity != null) {
                                    activity.requestPermissions(
                                            permissionsNotGranted.toArray(new String[size]),
                                            requestCode);
                                }
                            }

                            @Override
                            public void cancelled() {
                                // do nothing
                            }
                        });
            } else {
                if (activity != null) {
                    activity.requestPermissions(
                            permissionsNotGranted.toArray(new String[size]), requestCode);
                }
            }
        } else {
            result = true;
        }
        return result;
    }

    public static boolean checkPermissions(Context context, @NonNull String[] permissions) {
        if (permissions.length == 0) {
            return true;
        }
        for (String permission : permissions) {
            if ((isFlyme() || (Build.VERSION.SDK_INT < Build.VERSION_CODES.M))
                    && permission.equals(Manifest.permission.RECORD_AUDIO)) {
                RLog.i(TAG, "Build.MODEL = " + Build.MODEL);
                if ((Build.BRAND.toLowerCase().equals("meizu"))) {
                    // 针对魅族手机，采取双重判断的方式
                    if (hasPermission(context, permission) || hasRecordPermision(context)) {
                        continue;
                    } else {
                        return false;
                    }
                }

                if (!hasRecordPermision(context)) {
                    return false;
                } else {
                    continue;
                }
            }
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static String[] getMediaStoragePermissions(Context context) {
        String[] permissions;
        if (RongUtils.checkSDKVersionAndTargetIsUDC(context)) {
            permissions =
                    new String[] {
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    };
        } else if (RongUtils.checkSDKVersionAndTargetIsTIRAMISU(context)) {
            permissions =
                    new String[] {
                        Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO
                    };
        } else {
            permissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        return permissions;
    }

    public static boolean checkMediaStoragePermissions(Context context) {
        String[] permissions = getMediaStoragePermissions(context);
        return checkPermissions(context, permissions);
    }

    private static boolean isFlyme() {
        String osString = "";
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method get = clz.getMethod("get", String.class, String.class);
            osString = (String) get.invoke(clz, "ro.build.display.id", "");
        } catch (Exception e) {
            RLog.e(TAG, "isFlyme", e);
        }
        return osString != null && osString.toLowerCase().contains("flyme");
    }

    private static boolean hasRecordPermision(Context context) {
        boolean hasPermission = false;
        int bufferSizeInBytes =
                AudioRecord.getMinBufferSize(
                        44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSizeInBytes < 0) {
            RLog.e(TAG, "bufferSizeInBytes = " + bufferSizeInBytes);
            return false;
        }
        AudioRecord audioRecord;
        try {
            audioRecord =
                    new AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            44100,
                            AudioFormat.CHANNEL_IN_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSizeInBytes);
            audioRecord.startRecording();
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                hasPermission = true;
                audioRecord.stop();
            }
            audioRecord.release();
        } catch (Exception e) {
            RLog.e(TAG, "Audio record exception.");
        }
        return hasPermission;
    }

    // KNOTE: 2021/8/25 修复权限提示窗权限提示name重复问题
    private static String getNotGrantedPermissionMsg(
            Context context, String[] permissions, int[] grantResults) {
        if (checkPermissionResultIncompatible(permissions, grantResults)) {
            return "";
        }

        try {
            List<String> permissionNameList = new ArrayList<>(permissions.length);
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    String permissionName =
                            context.getString(
                                    context.getResources()
                                            .getIdentifier(
                                                    "rc_" + permissions[i],
                                                    "string",
                                                    context.getPackageName()),
                                    0);
                    if (!permissionNameList.contains(permissionName)) {
                        permissionNameList.add(permissionName);
                    }
                }
            }

            StringBuilder builder =
                    new StringBuilder(
                            context.getResources().getString(R.string.rc_permission_grant_needed));
            return builder.append("(")
                    .append(TextUtils.join(" ", permissionNameList))
                    .append(")")
                    .toString();

        } catch (Resources.NotFoundException e) {
            RLog.e(
                    TAG,
                    "One of the permissions is not recognized by SDK."
                            + Arrays.toString(permissions));
        }

        return "";
    }

    private static String getNotGrantedPermissionMsg(Context context, List<String> permissions) {
        if (permissions == null || permissions.size() == 0) {
            return "";
        }
        Set<String> permissionsValue = new HashSet<>();
        String permissionValue;
        try {
            for (String permission : permissions) {
                permissionValue =
                        context.getString(
                                context.getResources()
                                        .getIdentifier(
                                                "rc_" + permission,
                                                "string",
                                                context.getPackageName()),
                                0);
                permissionsValue.add(permissionValue);
            }
        } catch (Resources.NotFoundException e) {
            RLog.e(
                    TAG,
                    "one of the permissions is not recognized by SDK." + permissions.toString());
            return "";
        }

        StringBuilder result = new StringBuilder("(");
        for (String value : permissionsValue) {
            result.append(value).append(" ");
        }
        result = new StringBuilder(result.toString().trim() + ")");
        return result.toString();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void showPermissionAlert(
            Context context, String content, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setMessage(content)
                .setPositiveButton(R.string.rc_confirm, listener)
                .setNegativeButton(R.string.rc_cancel, listener)
                .setNeutralButton(R.string.rc_not_prompt, listener)
                .setCancelable(false)
                .create()
                .show();
    }

    @TargetApi(19)
    public static boolean canDrawOverlays(Context context) {
        return canDrawOverlays(context, true);
    }

    /**
     * 检查是否有悬浮窗权限
     *
     * @param context 上下文
     * @return boolean whether have the permission
     */
    @TargetApi(19)
    public static boolean canDrawOverlays(
            final Context context, boolean needOpenPermissionSetting) {
        boolean result = true;
        boolean booleanValue;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                booleanValue =
                        (Boolean)
                                Settings.class
                                        .getDeclaredMethod("canDrawOverlays", Context.class)
                                        .invoke(null, new Object[] {context});
                if (!booleanValue && needOpenPermissionSetting) {
                    ArrayList<String> permissionList = new ArrayList<>();
                    permissionList.add(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    showPermissionAlert(
                            context,
                            context.getString(R.string.rc_permission_grant_needed)
                                    + getNotGrantedPermissionMsg(context, permissionList),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (DialogInterface.BUTTON_POSITIVE == which) {
                                        Intent intent =
                                                new Intent(
                                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                        Uri.parse(
                                                                "package:"
                                                                        + context
                                                                                .getPackageName()));
                                        context.startActivity(intent);
                                    }
                                }
                            });
                }
                RLog.i(TAG, "isFloatWindowOpAllowed allowed: " + booleanValue);
                return booleanValue;
            } catch (Exception e) {
                RLog.e(
                        TAG,
                        String.format(
                                "getDeclaredMethod:canDrawOverlays! Error:%s, etype:%s",
                                e.getMessage(), e.getClass().getCanonicalName()));
                return true;
            }
        } else if (Build.VERSION.SDK_INT < 19) {
            return true;
        } else {
            Method method;
            Object systemService = context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                method =
                        Class.forName("android.app.AppOpsManager")
                                .getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
            } catch (NoSuchMethodException e) {
                RLog.e(
                        TAG,
                        String.format(
                                "NoSuchMethodException method:checkOp! Error:%s", e.getMessage()));
                method = null;
            } catch (ClassNotFoundException e) {
                RLog.e(TAG, "canDrawOverlays", e);
                method = null;
            }
            if (method != null) {
                try {
                    Integer tmp =
                            (Integer)
                                    method.invoke(
                                            systemService,
                                            new Object[] {
                                                24,
                                                context.getApplicationInfo().uid,
                                                context.getPackageName()
                                            });
                    result = tmp != null && tmp == 0;
                } catch (Exception e) {
                    RLog.e(
                            TAG,
                            String.format(
                                    "call checkOp failed: %s etype:%s",
                                    e.getMessage(), e.getClass().getCanonicalName()));
                }
            }
            RLog.i(TAG, "isFloatWindowOpAllowed allowed: " + result);
            return result;
        }
    }

    private static boolean hasPermission(Context context, String permission) {
        String opStr = AppOpsManagerCompat.permissionToOp(permission);
        if (opStr == null && Build.VERSION.SDK_INT < 23) {
            return true;
        }
        return context != null
                && context.checkCallingOrSelfPermission(permission)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public static void showRequestPermissionFailedAlter(
            final Context context, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (context == null) {
            return;
        }
        String content = getNotGrantedPermissionMsg(context, permissions, grantResults);
        if (TextUtils.isEmpty(content)) {
            return;
        }
        DialogInterface.OnClickListener listener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                Intent intent =
                                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                                intent.setData(uri);
                                context.startActivity(intent);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                            default:
                                break;
                        }
                    }
                };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert)
                    .setMessage(content)
                    .setPositiveButton(R.string.rc_confirm, listener)
                    .setNegativeButton(R.string.rc_cancel, listener)
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            new AlertDialog.Builder(context)
                    .setMessage(content)
                    .setPositiveButton(R.string.rc_confirm, listener)
                    .setNegativeButton(R.string.rc_cancel, listener)
                    .setCancelable(false)
                    .create()
                    .show();
        }
    }

    /**
     * 权限授权回调参数匹配性检测.
     *
     * @param grantResults 系统返回的授权结果。
     * @return {@code true} 参数不匹配;{@code false} 参数检查匹配
     */
    public static boolean checkPermissionResultIncompatible(
            String[] permissions, int[] grantResults) {
        return grantResults == null
                || grantResults.length == 0
                || permissions == null
                || permissions.length != grantResults.length;
    }

    /**
     * 设置申请权限前拦截监听
     *
     * @param listener
     */
    public static void setRequestPermissionListListener(IRequestPermissionListListener listener) {
        if (listener == null) {
            return;
        }
        PermissionCheckUtil.listener = listener;
    }

    /** SDK申请权限前,用户可以设置此监听，在{@code onRequestPermissionList}方法实现中创建Dialog弹窗,用于向用户解释权限申请的原因. */
    public interface IRequestPermissionListListener {
        /**
         * @param activity
         * @param permissionsNotGranted
         * @param callback 用于回调Dialog弹窗的确定和取消事件
         */
        void onRequestPermissionList(
                Context activity,
                List<String> permissionsNotGranted,
                IPermissionEventCallback callback);
    }

    /** 权限申请原因解释Dialog的确定和取消的按钮事件通知. */
    public interface IPermissionEventCallback {
        void confirmed();

        void cancelled();
    }
}
