package io.rong.imkit.conversation.extension.component.plugin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.List;

import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.ConversationConfig;
import io.rong.imkit.config.FeatureConfig;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.manager.SendImageManager;
import io.rong.imkit.manager.SendMediaManager;
import io.rong.imkit.picture.PictureSelector;
import io.rong.imkit.picture.config.PictureConfig;
import io.rong.imkit.picture.config.PictureMimeType;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.GlideKitImageEngine;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

import static android.app.Activity.RESULT_OK;

public class ImagePlugin implements IPluginModule, IPluginRequestPermissionResultCallback {
    Conversation.ConversationType conversationType;
    String targetId;
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
        conversationType = extension.getConversationType();
        targetId = extension.getTargetId();
        mRequestCode = ((index + 1) << 8) + (PictureConfig.CHOOSE_REQUEST & 0xff);
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};

        if (PermissionCheckUtil.checkPermissions(currentFragment.getContext(), permissions)) {
            openPictureSelector(currentFragment);
        } else {
            extension.requestPermissionForPluginResult(permissions, IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN, this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            // 图片、视频、音频选择结果回调
            List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
            if (selectList != null && selectList.size() > 0) {
                boolean sendOrigin = selectList.get(0).isOriginal();
                for (LocalMedia item : selectList) {
                    String mimeType = item.getMimeType();
                    if (mimeType.startsWith("image")) {
                        SendImageManager.getInstance().sendImage(conversationType, targetId, item, sendOrigin);
                        if (conversationType.equals(Conversation.ConversationType.PRIVATE)) {
                            RongIMClient.getInstance().sendTypingStatus(conversationType, targetId, "RC:ImgMsg");
                        }
                    } else if (mimeType.startsWith("video")) {
                        Uri path = Uri.parse(item.getPath());
                        if (TextUtils.isEmpty(path.getScheme())) {
                            path = Uri.parse("file://" + item.getPath());
                        }
                        SendMediaManager.getInstance().sendMedia(IMCenter.getInstance().getContext(), conversationType, targetId, path, item.getDuration());
                        if (conversationType.equals(Conversation.ConversationType.PRIVATE)) {
                            RongIMClient.getInstance().sendTypingStatus(conversationType, targetId, "RC:SightMsg");
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean onRequestPermissionResult(Fragment fragment, RongExtension extension, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
            if (requestCode != -1) {
                openPictureSelector(fragment);
            }
        } else {
            if (fragment.getActivity() != null) {
                PermissionCheckUtil.showRequestPermissionFailedAlter(fragment.getContext(), permissions, grantResults);
            }
        }
        return true;
    }

    private void openPictureSelector(Fragment currentFragment) {
        PictureSelector.create(currentFragment)
                .openGallery(RongConfigCenter.conversationConfig().rc_media_selector_contain_video ? PictureMimeType.ofAll() : PictureMimeType.ofImage())
                .loadImageEngine(RongConfigCenter.featureConfig().getKitImageEngine())
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .videoDurationLimit(RongIMClient.getInstance().getVideoLimitTime())
                .maxSelectNum(9)
                .imageSpanCount(3)
                .isGif(true)
                .forResult(mRequestCode);
    }
}
