package io.rong.imkit.picture;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import io.rong.common.CursorUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.picture.config.PictureConfig;
import io.rong.imkit.picture.config.PictureSelectionConfig;
import io.rong.imkit.picture.dialog.PictureLoadingDialog;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.picture.entity.LocalMediaFolder;
import io.rong.imkit.picture.tools.AttrsUtils;
import io.rong.imkit.picture.tools.MediaUtils;
import io.rong.imkit.picture.tools.PictureFileUtils;
import io.rong.imkit.picture.tools.SdkVersionUtils;
import io.rong.imkit.picture.tools.ToastUtils;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.utils.language.RongConfigurationManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class PictureBaseActivity extends AppCompatActivity {
    private static final String TAG = PictureBaseActivity.class.getCanonicalName();
    protected PictureSelectionConfig config;
    protected boolean openWhiteStatusBar, numComplete;
    protected int colorPrimary, colorPrimaryDark;
    protected String cameraPath;
    protected String originalPath;
    protected PictureLoadingDialog dialog;
    protected List<LocalMedia> selectionMedias;
    protected Handler mHandler;
    protected View container;

    /**
     * 获取布局文件
     *
     * @return
     */
    public abstract int getResourceId();

    protected void initWidgets() {
        // default implementation ignored
    }

    protected void initPictureSelectorStyle() {
        // default implementation ignored
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = RongConfigurationManager.getInstance().getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RongUtils.fixAndroid8ActivityCrash(this);
        if (savedInstanceState != null) {
            config = savedInstanceState.getParcelable(PictureConfig.EXTRA_CONFIG);
            cameraPath = savedInstanceState.getString(PictureConfig.BUNDLE_CAMERA_PATH);
            originalPath = savedInstanceState.getString(PictureConfig.BUNDLE_ORIGINAL_PATH);
        } else {
            config = PictureSelectionConfig.getInstance();
        }
        setTheme(config.themeStyleId);
        super.onCreate(savedInstanceState);
        initConfig();
        int layoutResID = getResourceId();
        if (layoutResID != 0) {
            setContentView(layoutResID);
        }
        initWidgets();
        initPictureSelectorStyle();
    }

    /**
     * 获取Context上下文
     *
     * @return
     */
    protected Context getContext() {
        return this;
    }

    /** 获取配置参数 */
    private void initConfig() {
        // 已选图片列表
        selectionMedias =
                config.selectionMedias == null
                        ? new ArrayList<LocalMedia>()
                        : config.selectionMedias;
        openWhiteStatusBar = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_statusFontColor);

        numComplete = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_style_numComplete);

        config.checkNumMode =
                AttrsUtils.getTypeValueBoolean(this, R.attr.picture_style_checkNumMode);

        // 标题栏背景色
        colorPrimary = AttrsUtils.getTypeValueColor(this, R.attr.colorPrimary);

        // 状态栏色值
        colorPrimaryDark = AttrsUtils.getTypeValueColor(this, R.attr.colorPrimaryDark);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PictureConfig.BUNDLE_CAMERA_PATH, cameraPath);
        outState.putString(PictureConfig.BUNDLE_ORIGINAL_PATH, originalPath);
        outState.putParcelable(PictureConfig.EXTRA_CONFIG, config);
    }

    /** loading dialog */
    protected void showPleaseDialog() {
        if (!isFinishing()) {
            dismissDialog();
            dialog = new PictureLoadingDialog(getContext());
            dialog.show();
        }
    }

    /** dismiss dialog */
    protected void dismissDialog() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                dialog = null;
            }
        } catch (Exception e) {
            dialog = null;
            RLog.e(TAG, e.getMessage());
        }
    }

    /**
     * compress or callback
     *
     * @param result
     */
    protected void handlerResult(List<LocalMedia> result) {
        onResult(result);
    }

    /**
     * 如果没有任何相册，先创建一个最近相册出来
     *
     * @param folders
     */
    protected void createNewFolder(List<LocalMediaFolder> folders) {
        if (folders.size() == 0) {
            // 没有相册 先创建一个最近相册出来
            LocalMediaFolder newFolder = new LocalMediaFolder();
            String folderName = getString(R.string.rc_picture_camera_roll);
            newFolder.setName(folderName);
            newFolder.setFirstImagePath("");
            folders.add(newFolder);
        }
    }

    /**
     * 将图片插入到相机文件夹中
     *
     * @param path
     * @param imageFolders
     * @return
     */
    protected LocalMediaFolder getImageFolder(String path, List<LocalMediaFolder> imageFolders) {
        File imageFile = new File(path);
        File folderFile = imageFile.getParentFile();

        for (LocalMediaFolder folder : imageFolders) {
            if (folder.getName().equals(folderFile.getName())) {
                return folder;
            }
        }
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setName(folderFile.getName());
        newFolder.setFirstImagePath(path);
        imageFolders.add(newFolder);
        return newFolder;
    }

    /**
     * return image result
     *
     * @param images
     */
    protected void onResult(List<LocalMedia> images) {
        if (images == null) {
            return;
        }
        if (config.camera
                && config.selectionMode == PictureConfig.MULTIPLE
                && selectionMedias != null) {
            images.addAll(images.size() > 0 ? images.size() - 1 : 0, selectionMedias);
        }
        if (config.isCheckOriginalImage) {
            int size = images.size();
            for (int i = 0; i < size; i++) {
                LocalMedia media = images.get(i);
                media.setOriginal(true);
            }
        }
        Intent intent = PictureSelector.putIntentResult(images);
        setResult(RESULT_OK, intent);
        closeActivity();
    }

    /** Close Activity */
    protected void closeActivity() {
        finish();
        if (config.camera) {
            overridePendingTransition(0, R.anim.rc_picture_anim_fade_out);
        } else {
            overridePendingTransition(0, R.anim.rc_picture_anim_exit);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissDialog();
    }

    /**
     * 删除部分手机 拍照在DCIM也生成一张的问题
     *
     * @param id
     * @param eqVideo
     */
    @Deprecated
    protected void removeImage(int id, boolean eqVideo) {
        try {
            Uri uri =
                    eqVideo
                            ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String selection =
                    eqVideo
                            ? MediaStore.Video.Media._ID + "=?"
                            : MediaStore.Images.Media._ID + "=?";
            CursorUtils.delete(this, uri, selection, new String[] {Long.toString(id)});
        } catch (Exception e) {
            RLog.e(TAG, e.getMessage());
        }
    }

    /** start to camera、preview、crop */
    protected void startOpenCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Uri imageUri;
                if (SdkVersionUtils.checkedAndroid_Q()) {
                    imageUri = MediaUtils.createImageUri(getApplicationContext());
                    if (imageUri != null) {
                        cameraPath = imageUri.toString();
                    }
                } else {
                    int chooseMode =
                            config.chooseMode == PictureConfig.TYPE_ALL
                                    ? PictureConfig.TYPE_IMAGE
                                    : config.chooseMode;
                    File cameraFile =
                            PictureFileUtils.createCameraFile(
                                    getApplicationContext(),
                                    chooseMode,
                                    config.cameraFileName,
                                    config.suffixType);
                    cameraPath = cameraFile.getAbsolutePath();
                    imageUri = PictureFileUtils.parUri(this, cameraFile);
                }
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
            }
        } catch (Exception e) {
            RLog.i(TAG, e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (PermissionCheckUtil.checkPermissionResultIncompatible(permissions, grantResults)) {
            if (getContext() != null) {
                ToastUtils.s(getContext(), getString(R.string.rc_permission_request_failed));
            }
            return;
        }

        switch (requestCode) {
            case PictureConfig.APPLY_AUDIO_PERMISSIONS_CODE:
                // 录音权限
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
                    }
                } else {
                    ToastUtils.s(getContext(), getString(R.string.rc_picture_audio));
                }
                break;
            default:
                break;
        }
    }
}
