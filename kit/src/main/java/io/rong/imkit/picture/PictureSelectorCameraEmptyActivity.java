package io.rong.imkit.picture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.picture.config.PictureConfig;
import io.rong.imkit.picture.config.PictureMimeType;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.picture.permissions.PermissionChecker;
import io.rong.imkit.picture.tools.MediaUtils;
import io.rong.imkit.picture.tools.PictureFileUtils;
import io.rong.imkit.picture.tools.SdkVersionUtils;
import io.rong.imkit.picture.tools.ToastUtils;

/**
 * @author：luck
 * @date：2019-11-15 21:41
 * @describe：单独拍照承载空Activity
 */
public class PictureSelectorCameraEmptyActivity extends PictureBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PermissionChecker
                .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                PermissionChecker
                        .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            onTakePhoto();
        } else {
            ToastUtils.s(getContext(), getString(R.string.rc_picture_camera));
            closeActivity();
            return;
        }
        setTheme(R.style.Picture_Theme_Translucent);
    }


    @Override
    public int getResourceId() {
        return R.layout.rc_picture_empty;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
    }

    /**
     * 启动相机
     */
    private void onTakePhoto() {
        // 启动相机拍照,先判断手机是否有拍照权限
        if (PermissionChecker
                .checkSelfPermission(this, Manifest.permission.CAMERA)) {
            startCamera();
        } else {
            PermissionChecker.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE);
        }
    }

    /**
     * 根据类型启动相应相机
     */
    private void startCamera() {
        switch (config.chooseMode) {
            case PictureConfig.TYPE_ALL:
            case PictureConfig.TYPE_IMAGE:
                // 拍照
                startOpenCamera();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.REQUEST_CAMERA:
                    requestCamera(data);
                    break;
                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            closeActivity();
        }
    }


    /**
     * 拍照后处理结果
     *
     * @param data
     */

    private void requestCamera(Intent data) {
        // on take photo success
        String mimeType = null;
        long duration = 0;
        boolean isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        if (TextUtils.isEmpty(cameraPath) || new File(cameraPath) == null) {
            return;
        }
        long size = 0;
        int[] newSize = new int[2];
        final File file = new File(cameraPath);
        if (!isAndroidQ) {
            new PictureMediaScannerConnection(getApplicationContext(), cameraPath, new PictureMediaScannerConnection.ScanListener() {
                @Override
                public void onScanFinish() {

                }
            });
        }
        LocalMedia media = new LocalMedia();
        // 图片视频处理规则
        if (isAndroidQ) {
            String path = PictureFileUtils.getPath(getApplicationContext(), Uri.parse(cameraPath));
            File f = new File(path);
            size = f.length();
            mimeType = PictureMimeType.fileToType(f);
            if (PictureMimeType.eqImage(mimeType)) {
                int degree = PictureFileUtils.readPictureDegree(this, cameraPath);
                String rotateImagePath = PictureFileUtils.rotateImageToAndroidQ(this,
                        degree, cameraPath, config.cameraFileName);
                media.setAndroidQToPath(rotateImagePath);
                newSize = MediaUtils.getLocalImageSizeToAndroidQ(this, cameraPath);
            } else {
                newSize = MediaUtils.getLocalVideoSize(this, Uri.parse(cameraPath));
                duration = MediaUtils.extractDuration(getContext(), true, cameraPath);
            }
        } else {
            mimeType = PictureMimeType.fileToType(file);
            size = new File(cameraPath).length();
            if (PictureMimeType.eqImage(mimeType)) {
                int degree = PictureFileUtils.readPictureDegree(this, cameraPath);
                PictureFileUtils.rotateImage(degree, cameraPath);
                newSize = MediaUtils.getLocalImageWidthOrHeight(cameraPath);
            } else {
                newSize = MediaUtils.getLocalVideoSize(cameraPath);
                duration = MediaUtils.extractDuration(getContext(), false, cameraPath);
            }
        }
        media.setDuration(duration);
        media.setWidth(newSize[0]);
        media.setHeight(newSize[1]);
        media.setPath(cameraPath);
        media.setMimeType(mimeType);
        media.setSize(size);
        media.setChooseModel(config.chooseMode);
        cameraHandleResult(media, mimeType);
    }

    /**
     * 摄像头后处理方式
     *
     * @param media
     * @param mimeType
     */
    private void cameraHandleResult(LocalMedia media, String mimeType) {
        // 不裁剪 不压缩 直接返回结果
        List<LocalMedia> result = new ArrayList<>();
        result.add(media);
        onResult(result);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeActivity();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE:
                // 存储权限
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        onTakePhoto();
                    } else {
                        closeActivity();
                        ToastUtils.s(getContext(), getString(R.string.rc_picture_camera));
                    }
                }
                break;
            case PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE:
                // 相机权限
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onTakePhoto();
                } else {
                    closeActivity();
                    ToastUtils.s(getContext(), getString(R.string.rc_picture_camera));
                }
                break;
        }
    }
}
