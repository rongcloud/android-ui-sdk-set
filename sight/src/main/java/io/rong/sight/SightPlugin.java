package io.rong.sight;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import io.rong.common.FileUtils;
import io.rong.common.LibStorageUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.conversation.extension.component.plugin.IPluginRequestPermissionResultCallback;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RongOperationPermissionUtils;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.message.SightMessage;
import io.rong.sight.record.SightRecordActivity;
import java.io.File;

public class SightPlugin implements IPluginModule, IPluginRequestPermissionResultCallback {
    private static final String TAG = "SightPlugin";
    protected Conversation.ConversationType conversationType;
    protected String targetId;
    protected Context context;
    private static final int REQUEST_SIGHT = 104;

    @Override
    public Drawable obtainDrawable(Context context) {
        this.context = context;
        return ContextCompat.getDrawable(context, R.drawable.rc_ext_plugin_sight_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_plugin_sight);
    }

    @Override
    public void onClick(Fragment currentFragment, RongExtension extension, int index) {
        if (extension == null) {
            RLog.e(TAG, "onClick extension null");
            return;
        }
        if (!RongOperationPermissionUtils.isMediaOperationPermit(currentFragment.getActivity())) {
            return;
        }

        // KNOTE: 2021/8/24 小视频录像保存至私有目录  不需要存储权限
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        conversationType = extension.getConversationType();
        targetId = extension.getTargetId();
        if (PermissionCheckUtil.checkPermissions(currentFragment.getActivity(), permissions)) {
            startSightRecord(currentFragment, extension);
        } else {
            extension.requestPermissionForPluginResult(
                    permissions,
                    IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN,
                    this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK
                && requestCode == REQUEST_SIGHT
                && data != null
                && context != null) {
            String fileUrl = data.getStringExtra("recordSightUrl");
            File file = new File(fileUrl);
            if (file.exists()) {
                int recordTime = data.getIntExtra("recordSightTime", 0);
                SightMessage sightMessage = SightMessage.obtain(Uri.fromFile(file), recordTime);
                if (sightMessage == null) {
                    RLog.e(TAG, "onActivityResult SightMessage null");
                    return;
                }
                if (DestructManager.isActive()) {
                    sightMessage.setDestructTime(DestructManager.SIGHT_DESTRUCT_TIME);
                }
                io.rong.imlib.model.Message message =
                        io.rong.imlib.model.Message.obtain(
                                targetId, conversationType, sightMessage);
                IMCenter.getInstance()
                        .sendMediaMessage(
                                message,
                                DestructManager.isActive()
                                        ? context.getResources()
                                                .getString(
                                                        io.rong
                                                                .imkit
                                                                .R
                                                                .string
                                                                .rc_conversation_summary_content_burn)
                                        : null,
                                null,
                                (IRongCallback.ISendMediaMessageCallback) null);
            }
        }
    }

    private void startSightRecord(Fragment currentFragment, RongExtension extension) {
        FragmentActivity activity = currentFragment.getActivity();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            RLog.e(TAG, "startSightRecord activity null");
            return;
        }
        File saveDir = null;
        saveDir =
                new File(
                        FileUtils.getMediaDownloadDir(
                                currentFragment.getContext(), LibStorageUtils.VIDEO));
        boolean successMkdir = saveDir.mkdirs();
        if (!successMkdir) {
            RLog.e(TAG, "Created folders UnSuccessfully");
        }

        Intent intent = new Intent(currentFragment.getActivity(), SightRecordActivity.class);
        if (saveDir != null) {
            intent.putExtra("recordSightDir", saveDir.getAbsolutePath());
        }
        int maxRecordDuration = 10;
        try {
            maxRecordDuration =
                    currentFragment
                            .getActivity()
                            .getResources()
                            .getInteger(io.rong.sight.R.integer.rc_sight_max_record_duration);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
        // 获取允许视频最长时间，小视频设置的最长录制时间不能超过此时间
        int videoLimitTime = RongIMClient.getInstance().getVideoLimitTime();
        if (maxRecordDuration > videoLimitTime) {
            maxRecordDuration = videoLimitTime;
        }
        intent.putExtra("maxRecordDuration", maxRecordDuration); // seconds
        extension.startActivityForPluginResult(intent, REQUEST_SIGHT, this);
    }

    @Override
    public boolean onRequestPermissionResult(
            Fragment fragment,
            RongExtension extension,
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
            startSightRecord(fragment, extension);
        } else {
            PermissionCheckUtil.showRequestPermissionFailedAlter(
                    fragment.getContext(), permissions, grantResults);
        }
        return true;
    }
}
