package io.rong.imkit.picture;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.picture.broadcast.BroadcastAction;
import io.rong.imkit.picture.broadcast.BroadcastManager;
import io.rong.imkit.picture.config.PictureConfig;
import io.rong.imkit.picture.config.PictureMimeType;
import io.rong.imkit.picture.config.PictureSelectionConfig;
import io.rong.imkit.picture.dialog.PictureLoadingDialog;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.picture.entity.LocalMediaFolder;
import io.rong.imkit.picture.tools.AndroidQTransformUtils;
import io.rong.imkit.picture.tools.AttrsUtils;
import io.rong.imkit.picture.tools.MediaUtils;
import io.rong.imkit.picture.tools.PictureFileUtils;
import io.rong.imkit.picture.tools.SdkVersionUtils;
import io.rong.imkit.picture.tools.ToastUtils;
import io.rong.imkit.utils.language.RongConfigurationManager;


/**
 * @author：luck
 * @data：2018/3/28 下午1:00
 * @描述: Activity基类
 */
public abstract class PictureBaseActivity extends AppCompatActivity implements Handler.Callback {
    private static final int MSG_CHOOSE_RESULT_SUCCESS = 200;
    private static final int MSG_ASY_COMPRESSION_RESULT_SUCCESS = 300;
    protected PictureSelectionConfig config;
    protected boolean openWhiteStatusBar, numComplete;
    protected int colorPrimary, colorPrimaryDark;
    protected String cameraPath;
    protected String originalPath;
    protected PictureLoadingDialog dialog;
    protected PictureLoadingDialog compressDialog;
    protected List<LocalMedia> selectionMedias;
    protected Handler mHandler;
    protected View container;


    /**
     * 是否改变屏幕方向
     *
     * @return
     */
    public boolean isRequestedOrientation() {
        return true;
    }

    /**
     * 获取布局文件
     *
     * @return
     */
    public abstract int getResourceId();

    protected void initWidgets() {
    }

    protected void initPictureSelectorStyle() {

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = RongConfigurationManager.getInstance().getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            config = savedInstanceState.getParcelable(PictureConfig.EXTRA_CONFIG);
            cameraPath = savedInstanceState.getString(PictureConfig.BUNDLE_CAMERA_PATH);
            originalPath = savedInstanceState.getString(PictureConfig.BUNDLE_ORIGINAL_PATH);
        } else {
            config = PictureSelectionConfig.getInstance();
        }
        setTheme(config.themeStyleId);
        super.onCreate(savedInstanceState);
        if (isRequestedOrientation()) {
            setNewRequestedOrientation();
        }
        mHandler = new Handler(Looper.getMainLooper(), this);
        initConfig();
        int layoutResID = getResourceId();
        if (layoutResID != 0) {
            setContentView(layoutResID);
        }
        initWidgets();
        initPictureSelectorStyle();
    }

    /**
     * 设置屏幕方向
     */
    protected void setNewRequestedOrientation() {
        if (config != null) {
            setRequestedOrientation(config.requestedOrientation);
        }
    }

    /**
     * 获取Context上下文
     *
     * @return
     */
    protected Context getContext() {
        return this;
    }

    /**
     * 获取配置参数
     */
    private void initConfig() {
        // 已选图片列表
        selectionMedias = config.selectionMedias == null ? new ArrayList<LocalMedia>() : config.selectionMedias;
        openWhiteStatusBar = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_statusFontColor);

        numComplete = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_style_numComplete);

        config.checkNumMode = AttrsUtils.getTypeValueBoolean(this, R.attr.picture_style_checkNumMode);

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


    /**
     * loading dialog
     */
    protected void showPleaseDialog() {
        if (!isFinishing()) {
            dismissDialog();
            dialog = new PictureLoadingDialog(getContext());
            dialog.show();
        }
    }

    /**
     * dismiss dialog
     */
    protected void dismissDialog() {
        try {
            if (dialog != null
                    && dialog.isShowing()) {
                dialog.dismiss();
                dialog = null;
            }
        } catch (Exception e) {
            dialog = null;
            e.printStackTrace();
        }
    }

    /**
     * compress loading dialog
     */
    protected void showCompressDialog() {
        if (!isFinishing()) {
            dismissCompressDialog();
            compressDialog = new PictureLoadingDialog(this);
            compressDialog.show();
        }
    }

    /**
     * dismiss compress dialog
     */
    protected void dismissCompressDialog() {
        try {
            if (!isFinishing()
                    && compressDialog != null
                    && compressDialog.isShowing()) {
                compressDialog.dismiss();
                compressDialog = null;
            }
        } catch (Exception e) {
            compressDialog = null;
            e.printStackTrace();
        }
    }


    /**
     * 重新构造已压缩的图片返回集合
     *
     * @param images
     * @param files
     */
    private void handleCompressCallBack(List<LocalMedia> images, List<File> files) {
        if (images == null || files == null) {
            closeActivity();
            return;
        }
        boolean isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        int size = images.size();
        if (files.size() == size) {
            for (int i = 0, j = size; i < j; i++) {
                // 压缩成功后的地址
                File file = files.get(i);
                String path = file.getPath();
                LocalMedia image = images.get(i);
                // 如果是网络图片则不压缩
                boolean http = PictureMimeType.isHttp(path);
                boolean flag = !TextUtils.isEmpty(path) && http;
                image.setCompressed(flag ? false : true);
                image.setCompressPath(flag ? "" : path);
                if (isAndroidQ) {
                    image.setAndroidQToPath(path);
                }
            }
        }
        BroadcastManager.getInstance(getApplicationContext())
                .action(BroadcastAction.ACTION_CLOSE_PREVIEW).broadcast();
        onResult(images);
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
            String folderName = getString(R.string.picture_camera_roll);
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
        boolean isAndroidQ = SdkVersionUtils.checkedAndroid_Q();
        boolean isVideo = PictureMimeType.eqVideo(images.size() > 0
                ? images.get(0).getMimeType() : "");
        if (isAndroidQ && !isVideo) {
            showCompressDialog();
        }
        if (isAndroidQ && config.isAndroidQTransform) {
            onResultToAndroidAsy(images);
        } else {
            dismissCompressDialog();
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
                    media.setOriginalPath(media.getPath());
                }
            }
            Intent intent = PictureSelector.putIntentResult(images);
            setResult(RESULT_OK, intent);
            closeActivity();
        }
    }

    /**
     * 针对Android 异步处理
     *
     * @param images
     */
    private void onResultToAndroidAsy(final List<LocalMedia> images) {
        AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {

                // Android Q 版本做拷贝应用内沙盒适配
                int size = images.size();
                for (int i = 0; i < size; i++) {
                    LocalMedia media = images.get(i);
                    if (media == null || TextUtils.isEmpty(media.getPath())) {
                        continue;
                    }
                    boolean isCopyAndroidQToPath = !media.isCut()
                            && !media.isCompressed()
                            && TextUtils.isEmpty(media.getAndroidQToPath());
                    if (isCopyAndroidQToPath) {
                        media.setAndroidQToPath(getPathToAndroidQ(media));
                        if (config.isCheckOriginalImage) {
                            media.setOriginal(true);
                            media.setOriginalPath(media.getAndroidQToPath());
                        }
                    } else if (media.isCut() && media.isCompressed()) {
                        media.setAndroidQToPath(media.getCompressPath());
                    } else {
                        if (config.isCheckOriginalImage) {
                            media.setOriginal(true);
                            media.setOriginalPath(media.getAndroidQToPath());
                        }
                    }
                }
                // 线程切换
                mHandler.sendMessage(mHandler.obtainMessage(MSG_CHOOSE_RESULT_SUCCESS, images));
            }
        });
    }

    /**
     * 复制一份至自己应用沙盒内
     *
     * @param media
     * @return
     */
    private String getPathToAndroidQ(LocalMedia media) {
        if (PictureMimeType.eqVideo(media.getMimeType())) {
            return AndroidQTransformUtils.parseVideoPathToAndroidQ
                    (getApplicationContext(), media.getPath(), config.cameraFileName, media.getMimeType());
        } else {
            return AndroidQTransformUtils.parseImagePathToAndroidQ
                    (getApplicationContext(), media.getPath(), config.cameraFileName, media.getMimeType());
        }
    }

    /**
     * Close Activity
     */
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
        dismissCompressDialog();
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
            ContentResolver cr = getContentResolver();
            Uri uri = eqVideo ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String selection = eqVideo ? MediaStore.Video.Media._ID + "=?"
                    : MediaStore.Images.Media._ID + "=?";
            cr.delete(uri,
                    selection,
                    new String[]{Long.toString(id)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * start to camera、preview、crop
     */
    protected void startOpenCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Uri imageUri;
            if (SdkVersionUtils.checkedAndroid_Q()) {
                imageUri = MediaUtils.createImageUri(getApplicationContext());
                if (imageUri != null) {
                    cameraPath = imageUri.toString();
                }
            } else {
                int chooseMode = config.chooseMode == PictureConfig.TYPE_ALL ? PictureConfig.TYPE_IMAGE
                        : config.chooseMode;
                File cameraFile = PictureFileUtils.createCameraFile(getApplicationContext(),
                        chooseMode, config.cameraFileName, config.suffixType);
                cameraPath = cameraFile.getAbsolutePath();
                imageUri = PictureFileUtils.parUri(this, cameraFile);
            }
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
        }
    }


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_CHOOSE_RESULT_SUCCESS:
                // 选择完成回调
                List<LocalMedia> images = (List<LocalMedia>) msg.obj;
                dismissCompressDialog();
                if (images != null) {
                    if (config.camera
                            && config.selectionMode == PictureConfig.MULTIPLE
                            && selectionMedias != null) {
                        images.addAll(images.size() > 0 ? images.size() - 1 : 0, selectionMedias);
                    }
                    Intent intent = PictureSelector.putIntentResult(images);
                    setResult(RESULT_OK, intent);
                    closeActivity();
                }
                break;
            case MSG_ASY_COMPRESSION_RESULT_SUCCESS:
                // 异步压缩回调
                if (msg.obj != null && msg.obj instanceof Object[]) {
                    Object[] objects = (Object[]) msg.obj;
                    if (objects.length > 0) {
                        List<LocalMedia> result = (List<LocalMedia>) objects[0];
                        List<File> files = (List<File>) objects[1];
                        handleCompressCallBack(result, files);
                    }
                }
                break;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PictureConfig.APPLY_AUDIO_PERMISSIONS_CODE:
                // 录音权限
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA);
                    }
                } else {
                    ToastUtils.s(getContext(), getString(R.string.picture_audio));
                }
                break;
        }
    }
}
