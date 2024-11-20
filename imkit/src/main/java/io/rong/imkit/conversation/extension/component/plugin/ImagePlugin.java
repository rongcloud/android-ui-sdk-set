package io.rong.imkit.conversation.extension.component.plugin;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.manager.SendImageManager;
import io.rong.imkit.manager.SendMediaManager;
import io.rong.imkit.picture.PictureSelector;
import io.rong.imkit.picture.config.PictureConfig;
import io.rong.imkit.picture.config.PictureMimeType;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.picture.permissions.PermissionChecker;
import io.rong.imkit.utils.AndroidConstant;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import java.util.List;

public class ImagePlugin implements IPluginModule, IPluginRequestPermissionResultCallback {
    private static final String TAG = "ImagePlugin";
    ConversationIdentifier conversationIdentifier;
    private int mRequestCode = -1;

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ext_plugin_image_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_ext_plugin_image);
    }

    @Override
    public void onClick(Fragment currentFragment, RongExtension extension, int index) {
        if (extension == null) {
            RLog.e(TAG, "onClick extension null");
            return;
        }
        conversationIdentifier = extension.getConversationIdentifier();
        mRequestCode = ((index + 1) << 8) + (PictureConfig.CHOOSE_REQUEST & 0xff);

        FragmentActivity activity = currentFragment.getActivity();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            RLog.e(TAG, "onClick activity null");
            return;
        }

        // KNOTE: 2021/8/25 CAMERA权限进入图库后点击拍照时申请
        // 如果是Android 14， 判断两种情况
        // 第一种：获取到所有权限：READ_MEDIA_IMAGES 和 READ_MEDIA_VIDEO
        // 第二种：获取到部分权限：READ_MEDIA_VISUAL_USER_SELECTED
        // 此两种都可以打开媒体库并发送媒体消息，不属于上述两种则弹出警示框
        if (Build.VERSION.SDK_INT >= AndroidConstant.ANDROID_UPSIDE_DOWN_CAKE) {
            String[] allPermissions =
                    new String[] {
                        Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO
                    };
            String[] subPermissions =
                    new String[] {Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED};

            if (PermissionChecker.checkSelfPermission(
                    currentFragment.getContext(), allPermissions)) {
                openPictureSelector(currentFragment);
            } else if (PermissionChecker.checkSelfPermission(
                    currentFragment.getContext(), subPermissions)) {
                openPictureSelector(currentFragment, false);
            } else {
                String[] permissions =
                        PermissionCheckUtil.getMediaStoragePermissions(
                                currentFragment.getContext());
                extension.requestPermissionForPluginResult(
                        permissions,
                        IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN,
                        this);
            }
            return;
        }

        if (PermissionCheckUtil.checkMediaStoragePermissions(currentFragment.getContext())) {
            openPictureSelector(currentFragment);
        } else {
            String[] permissions =
                    PermissionCheckUtil.getMediaStoragePermissions(currentFragment.getContext());
            extension.requestPermissionForPluginResult(
                    permissions,
                    IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN,
                    this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (conversationIdentifier == null) {
                RLog.e(
                        TAG,
                        "onActivityResult conversationIdentifier is null, requestCode="
                                + requestCode
                                + ",resultCode="
                                + resultCode);
                return;
            }
            // 图片、视频、音频选择结果回调
            List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
            if (selectList != null && selectList.size() > 0) {
                boolean sendOrigin = selectList.get(0).isOriginal();
                for (LocalMedia item : selectList) {
                    String mimeType = item.getMimeType();
                    if (mimeType.startsWith("image")) {
                        SendImageManager.getInstance()
                                .sendImage(conversationIdentifier, item, sendOrigin);
                        if (conversationIdentifier
                                .getType()
                                .equals(Conversation.ConversationType.PRIVATE)) {
                            RongIMClient.getInstance()
                                    .sendTypingStatus(
                                            conversationIdentifier.getType(),
                                            conversationIdentifier.getTargetId(),
                                            "RC:ImgMsg");
                        }
                    } else if (mimeType.startsWith("video")) {
                        Uri path = Uri.parse(item.getPath());
                        if (TextUtils.isEmpty(path.getScheme())) {
                            path = Uri.parse("file://" + item.getPath());
                        }
                        SendMediaManager.getInstance()
                                .sendMedia(
                                        IMCenter.getInstance().getContext(),
                                        conversationIdentifier,
                                        path,
                                        item.getDuration());
                        if (conversationIdentifier
                                .getType()
                                .equals(Conversation.ConversationType.PRIVATE)) {
                            RongIMClient.getInstance()
                                    .sendTypingStatus(
                                            conversationIdentifier.getType(),
                                            conversationIdentifier.getTargetId(),
                                            "RC:SightMsg");
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onRequestPermissionResult(
            Fragment fragment,
            RongExtension extension,
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
            if (requestCode != -1) {
                openPictureSelector(fragment);
            }
        } else {
            String[] subPermission =
                    new String[] {Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED};
            if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), subPermission)) {
                openPictureSelector(fragment, false);
            } else {
                if (fragment.getActivity() != null) {
                    PermissionCheckUtil.showRequestPermissionFailedAlter(
                            fragment.getContext(), permissions, grantResults);
                }
            }
        }
        return true;
    }

    private void openPictureSelector(Fragment currentFragment) {
        PictureSelector.create(currentFragment)
                .openGallery(
                        RongConfigCenter.conversationConfig().rc_media_selector_contain_video
                                ? PictureMimeType.ofAll()
                                : PictureMimeType.ofImage())
                .loadImageEngine(RongConfigCenter.featureConfig().getKitImageEngine())
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .videoDurationLimit(RongIMClient.getInstance().getVideoLimitTime())
                .gifSizeLimit(RongIMClient.getInstance().getGIFLimitSize() * 1024)
                .maxSelectNum(9)
                .imageSpanCount(3)
                .isGif(true)
                .forResult(mRequestCode);
    }

    private void openPictureSelector(Fragment currentFragment, boolean isAll) {
        PictureSelector.create(currentFragment)
                .openGallery(
                        RongConfigCenter.conversationConfig().rc_media_selector_contain_video
                                ? PictureMimeType.ofAll()
                                : PictureMimeType.ofImage())
                .loadImageEngine(RongConfigCenter.featureConfig().getKitImageEngine())
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .videoDurationLimit(RongIMClient.getInstance().getVideoLimitTime())
                .gifSizeLimit(RongIMClient.getInstance().getGIFLimitSize() * 1024)
                .maxSelectNum(9)
                .imageSpanCount(3)
                .isGif(true)
                .forResult(mRequestCode, isAll);
    }
}
