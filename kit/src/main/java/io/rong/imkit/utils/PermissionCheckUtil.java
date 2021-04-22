package io.rong.imkit.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.rong.common.RLog;
import io.rong.imkit.R;

/**
 * Created by jiangecho on 2016/10/25.
 */

public class PermissionCheckUtil {
    private static final String TAG = PermissionCheckUtil.class.getSimpleName();
    public static final int REQUEST_CODE_ASK_PERMISSIONS = 100;
    private static final String PROMPT = "prompt";
    private static final String IS_PROMPT = "isPrompt";

    public static boolean requestPermissions(Fragment fragment, String[] permissions) {
        return requestPermissions(fragment, permissions, 0);
    }

    public static boolean requestPermissions(final Fragment fragment, String[] permissions, int requestCode) {
        if (permissions.length == 0) {
            return true;
        }

        final List<String> permissionsNotGranted = new ArrayList<>();
        boolean result = false;

        for (String permission : permissions) {
            if ((isFlyme() || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) && permission.equals(Manifest.permission.RECORD_AUDIO)) {
                final SharedPreferences sharedPreferences = fragment.getContext().getSharedPreferences(PROMPT, Context.MODE_PRIVATE);
                boolean isPrompt = sharedPreferences.getBoolean(IS_PROMPT, true);
                if (isPrompt) {
                    showPermissionAlert(fragment.getContext(), fragment.getString(R.string.rc_permission_grant_needed) + fragment.getString(R.string.rc_permission_microphone),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (DialogInterface.BUTTON_POSITIVE == which) {
                                        fragment.startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
                                    } else if (DialogInterface.BUTTON_NEUTRAL == which) {
                                        SharedPreferences.Editor editor = sharedPreferences.edit().putBoolean(IS_PROMPT, false);
                                        editor.commit();
                                    }
                                }
                            });
                }
                return false;
            }
            if (!hasPermission(fragment.getActivity(), permission)) {
                permissionsNotGranted.add(permission);
            }
        }

        if (permissionsNotGranted.size() > 0) {
            int size = permissionsNotGranted.size();
            fragment.requestPermissions(permissionsNotGranted.toArray(new String[size]), requestCode);
        } else {
            result = true;
        }
        return result;
    }

    public static boolean requestPermissions(final Activity activity, @NonNull String[] permissions) {
        return requestPermissions(activity, permissions, 0);
    }

    @TargetApi(23)
    public static boolean requestPermissions(final Activity activity, @NonNull final String[] permissions, final int requestCode) {
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
            int size = permissionsNotGranted.size();
            activity.requestPermissions(permissionsNotGranted.toArray(new String[size]), requestCode);
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
            if ((isFlyme() || (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)) && permission.equals(Manifest.permission.RECORD_AUDIO)) {
                RLog.i(TAG, "Build.MODEL = " + Build.MODEL);
                if ((Build.BRAND.toLowerCase().equals("meizu") && Build.MODEL.equals("M1852"))) {
                    return hasPermission(context, permission);
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
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSizeInBytes < 0) {
            RLog.e(TAG, "bufferSizeInBytes = " + bufferSizeInBytes);
            return false;
        }
        AudioRecord audioRecord;
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
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

    private static String getNotGrantedPermissionMsg(Context context, String[] permissions, int[] grantResults) {
        if (permissions == null || permissions.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String permissionName;
        sb.append(context.getResources().getString(R.string.rc_permission_grant_needed));
        sb.append("(");
        try {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionName = context.getString(context.getResources().getIdentifier("rc_" + permissions[i], "string", context.getPackageName()), 0);
                    sb.append(permissionName);
                    if (i != permissions.length - 1) {
                        sb.append(" ");
                    }
                }
            }
        } catch (Resources.NotFoundException e) {
            RLog.e(TAG, "One of the permissions is not recognized by SDK." + Arrays.toString(permissions));
            return "";
        }
        sb.append(")");
        return sb.toString();
    }

    private static String getNotGrantedPermissionMsg(Context context, List<String> permissions) {
        if (permissions == null || permissions.size() == 0) {
            return "";
        }
        Set<String> permissionsValue = new HashSet<>();
        String permissionValue;
        try {
            for (String permission : permissions) {
                permissionValue = context.getString(context.getResources().getIdentifier("rc_" + permission, "string", context.getPackageName()), 0);
                permissionsValue.add(permissionValue);
            }
        } catch (Resources.NotFoundException e) {
            RLog.e(TAG, "one of the permissions is not recognized by SDK." + permissions.toString());
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
    private static void showPermissionAlert(Context context, String content, DialogInterface.OnClickListener listener) {
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
    public static boolean canDrawOverlays(final Context context, boolean needOpenPermissionSetting) {
        boolean result = true;
        boolean booleanValue;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                booleanValue = (Boolean) Settings.class.getDeclaredMethod("canDrawOverlays", Context.class).invoke(null, new Object[]{context});
                if (!booleanValue && needOpenPermissionSetting) {
                    ArrayList<String> permissionList = new ArrayList<>();
                    permissionList.add(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    showPermissionAlert(context, context.getString(R.string.rc_permission_grant_needed) + getNotGrantedPermissionMsg(context, permissionList),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (DialogInterface.BUTTON_POSITIVE == which) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:" + context.getPackageName()));
                                        context.startActivity(intent);
                                    }
                                }
                            });
                }
                RLog.i(TAG, "isFloatWindowOpAllowed allowed: " + booleanValue);
                return booleanValue;
            } catch (Exception e) {
                RLog.e(TAG, String.format("getDeclaredMethod:canDrawOverlays! Error:%s, etype:%s", e.getMessage(), e.getClass().getCanonicalName()));
                return true;
            }
        } else if (Build.VERSION.SDK_INT < 19) {
            return true;
        } else {
            Method method;
            Object systemService = context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                method = Class.forName("android.app.AppOpsManager").getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
            } catch (NoSuchMethodException e) {
                RLog.e(TAG, String.format("NoSuchMethodException method:checkOp! Error:%s", e.getMessage()));
                method = null;
            } catch (ClassNotFoundException e) {
                RLog.e(TAG, "canDrawOverlays", e);
                method = null;
            }
            if (method != null) {
                try {
                    Integer tmp = (Integer) method.invoke(systemService, new Object[]{24, context.getApplicationInfo().uid, context.getPackageName()});
                    result = tmp != null && tmp == 0;
                } catch (Exception e) {
                    RLog.e(TAG, String.format("call checkOp failed: %s etype:%s", e.getMessage(), e.getClass().getCanonicalName()));
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
        return context != null && context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void showRequestPermissionFailedAlter(final Context context, @NonNull String[] permissions, @NonNull int[] grantResults) {
        String content = getNotGrantedPermissionMsg(context, permissions, grantResults);
        if (TextUtils.isEmpty(content)) {
            return;
        }
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
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
}
