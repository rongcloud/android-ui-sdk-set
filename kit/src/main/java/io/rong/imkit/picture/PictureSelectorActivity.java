package io.rong.imkit.picture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.picture.adapter.PictureAlbumDirectoryAdapter;
import io.rong.imkit.picture.adapter.PictureImageGridAdapter;
import io.rong.imkit.picture.broadcast.BroadcastAction;
import io.rong.imkit.picture.broadcast.BroadcastManager;
import io.rong.imkit.picture.config.PictureConfig;
import io.rong.imkit.picture.config.PictureMimeType;
import io.rong.imkit.picture.decoration.GridSpacingItemDecoration;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.picture.entity.LocalMediaFolder;
import io.rong.imkit.picture.model.LocalMediaLoader;
import io.rong.imkit.picture.observable.ImagesObservable;
import io.rong.imkit.picture.permissions.PermissionChecker;
import io.rong.imkit.picture.tools.DoubleUtils;
import io.rong.imkit.picture.tools.JumpUtils;
import io.rong.imkit.picture.tools.MediaUtils;
import io.rong.imkit.picture.tools.PictureFileUtils;
import io.rong.imkit.picture.tools.ScreenUtils;
import io.rong.imkit.picture.tools.SdkVersionUtils;
import io.rong.imkit.picture.tools.StringUtils;
import io.rong.imkit.picture.tools.ToastUtils;
import io.rong.imkit.picture.widget.FolderPopWindow;


public class PictureSelectorActivity extends PictureBaseActivity implements View.OnClickListener,
        PictureAlbumDirectoryAdapter.OnItemClickListener,
        PictureImageGridAdapter.OnPhotoSelectChangedListener {
    protected static final int SHOW_DIALOG = 0;
    protected static final int DISMISS_DIALOG = 1;
    protected ImageView mIvArrow;
    protected TextView mTvPictureTitle, mTvCancel, mTvPictureOk, mTvEmpty, mTvPicturePreview;
    protected RecyclerView mPictureRecycler;
    protected FrameLayout mBottomLayout;
    protected PictureImageGridAdapter adapter;
    protected List<LocalMedia> images = new ArrayList<>();
    protected List<LocalMediaFolder> foldersList = new ArrayList<>();
    protected FolderPopWindow folderWindow;
    protected Animation animation = null;
    protected boolean anim = false;
    protected LocalMediaLoader mediaLoader;
    protected boolean isFirstEnterActivity = false;
    protected FrameLayout mTopLayout;
    private LinearLayout llAlbum;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SHOW_DIALOG:
                    showPleaseDialog();
                    break;
                case DISMISS_DIALOG:
                    dismissDialog();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BroadcastManager.getInstance(this)
                .registerReceiver(commonBroadcastReceiver, BroadcastAction.ACTION_SELECTED_DATA,
                        BroadcastAction.ACTION_PREVIEW_COMPRESSION);
        loadAllMediaData();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            // 防止拍照内存不足时activity被回收，导致拍照后的图片未选中
            selectionMedias = PictureSelector.obtainSelectorList(savedInstanceState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public int getResourceId() {
        return R.layout.rc_picture_selector;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
        container = findViewById(R.id.container);
        mTvPictureTitle = findViewById(R.id.picture_title);
        mTvCancel = findViewById(R.id.picture_cancel);
        mTvPictureOk = findViewById(R.id.picture_tv_ok);
        mIvArrow = findViewById(R.id.ivArrow);
        mTvPicturePreview = findViewById(R.id.picture_id_preview);
        mPictureRecycler = findViewById(R.id.picture_recycler);
        mBottomLayout = findViewById(R.id.fl_bottom);
        mTopLayout = findViewById(R.id.fl_top);
        mTvEmpty = findViewById(R.id.tv_empty);
        llAlbum = findViewById(R.id.ll_Album);
        isNumComplete(numComplete);
        mTvPicturePreview.setOnClickListener(this);
        mBottomLayout.setVisibility(config.selectionMode == PictureConfig.SINGLE
                && config.isSingleDirectReturn ? View.GONE : View.VISIBLE);
        mTvCancel.setOnClickListener(this);
        mTvPictureOk.setOnClickListener(this);
        llAlbum.setOnClickListener(this);
        String title = getString(R.string.picture_camera_roll);
        mTvPictureTitle.setText(title);
        folderWindow = new FolderPopWindow(this, config);
        folderWindow.setArrowImageView(mIvArrow);
        folderWindow.setOnItemClickListener(this);
        mPictureRecycler.setHasFixedSize(true);
        mPictureRecycler.addItemDecoration(new GridSpacingItemDecoration(config.imageSpanCount,
                ScreenUtils.dip2px(this, 2), false));
        mPictureRecycler.setLayoutManager(new GridLayoutManager(getContext(), config.imageSpanCount));
        // 解决调用 notifyItemChanged 闪烁问题,取消默认动画
        ((SimpleItemAnimator) mPictureRecycler.getItemAnimator())
                .setSupportsChangeAnimations(false);
        mTvEmpty.setText(getString(R.string.picture_empty));
        StringUtils.tempTextFont(mTvEmpty, config.chooseMode);
        adapter = new PictureImageGridAdapter(getContext(), config);
        adapter.setOnPhotoSelectChangedListener(this);
        adapter.bindSelectImages(selectionMedias);
        mPictureRecycler.setAdapter(adapter);
        adapter.bindSelectImages(selectionMedias);
    }


    /**
     * change image selector state
     *
     * @param selectImages
     */
    protected void changeImageNumber(List<LocalMedia> selectImages) {
        String mimeType = selectImages.size() > 0
                ? selectImages.get(0).getMimeType() : "";
        boolean isVideo = PictureMimeType.eqVideo(mimeType);
        boolean eqVideo = config.chooseMode == PictureConfig.TYPE_VIDEO;
        config.isCheckOriginalImage = !isVideo && !eqVideo && config.isCheckOriginalImage;
        boolean enable = selectImages.size() != 0;
        mTvPictureOk.setTextColor(selectImages.size() > 0 ? getResources().getColor(R.color.rc_main_theme) : getResources().getColor(R.color.rc_main_theme_lucency));
        mTvPictureOk.setText(config.selectionMode == PictureConfig.SINGLE || !enable ? getString(R.string.picture_send) :
                getString(R.string.picture_send_num) + "(" + selectImages.size() + ")");
        if (enable) {
            mTvPictureOk.setEnabled(true);
            mTvPictureOk.setSelected(true);
            mTvPicturePreview.setEnabled(true);
            mTvPicturePreview.setSelected(true);
        } else {
            mTvPictureOk.setEnabled(false);
            mTvPictureOk.setSelected(false);
            mTvPicturePreview.setEnabled(false);
            mTvPicturePreview.setSelected(false);
        }
    }
    /**
     * 加载数据
     */
    private void loadAllMediaData() {
        if (PermissionChecker
                .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                PermissionChecker
                        .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            mHandler.sendEmptyMessage(SHOW_DIALOG);
            readLocalMedia();
        } else {
            PermissionChecker.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE);
        }
    }

    /**
     * 动态设置相册主题
     */
    @Override
    public void initPictureSelectorStyle() {
        //llAlbum.setBackgroundResource(R.drawable.picture_album_bg);
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.rc_picture_icon_wechat_down);
        mIvArrow.setImageDrawable(drawable);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null) {
            List<LocalMedia> selectedImages = adapter.getSelectedImages();
            PictureSelector.saveSelectorList(outState, selectedImages);
        }
    }

    /**
     * none number style
     */
    private void isNumComplete(boolean numComplete) {
        if (!numComplete) {
            animation = AnimationUtils.loadAnimation(this, R.anim.rc_picture_anim_modal_in);
        }
        animation = numComplete ? null : AnimationUtils.loadAnimation(this, R.anim.rc_picture_anim_modal_in);
    }

    /**
     * get LocalMedia s
     */
    protected void readLocalMedia() {
        if (mediaLoader == null) {
            mediaLoader = new LocalMediaLoader(this, config);
        }
        mediaLoader.loadAllMedia();
        mediaLoader.setCompleteListener(new LocalMediaLoader.LocalMediaLoadListener() {
            @Override
            public void loadComplete(List<LocalMediaFolder> folders) {
                if (folders.size() > 0) {
                    foldersList = folders;
                    LocalMediaFolder folder = folders.get(0);
                    folder.setChecked(true);
                    List<LocalMedia> localImg = folder.getImages();
                    // 这里解决有些机型会出现拍照完，相册列表不及时刷新问题
                    // 因为onActivityResult里手动添加拍照后的照片，
                    // 如果查询出来的图片大于或等于当前adapter集合的图片则取更新后的，否则就取本地的
                    int size = images.size();
                    if (localImg.size() >= size) {
                        images = localImg;
                        folderWindow.bindFolder(folders);
                    }
                }
                if (adapter != null && images != null) {
                    adapter.bindImagesData(images);
                    boolean isEmpty = images.size() > 0;
                    if (!isEmpty) {
                        mTvEmpty.setText(getString(R.string.picture_empty));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            mTvEmpty.setCompoundDrawablesRelativeWithIntrinsicBounds
                                    (0, R.drawable.rc_picture_icon_no_data, 0, 0);
                        }
                    }
                    mTvEmpty.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);
                }
                mHandler.sendEmptyMessage(DISMISS_DIALOG);
            }

            @Override
            public void loadMediaDataError() {
                mHandler.sendEmptyMessage(DISMISS_DIALOG);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mTvEmpty.setCompoundDrawablesRelativeWithIntrinsicBounds
                            (0, R.drawable.rc_picture_icon_data_error, 0, 0);
                }
                mTvEmpty.setText(getString(R.string.picture_data_exception));
                mTvEmpty.setVisibility(images.size() > 0 ? View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    /**
     * open camera
     */
    public void startCamera() {
        // 防止快速点击，但是单独拍照不管
        if (!DoubleUtils.isFastDoubleClick()) {
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
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.picture_cancel) {
            if (folderWindow != null && folderWindow.isShowing()) {
                folderWindow.dismiss();
            } else {
                closeActivity();
            }
        }
        if (id == R.id.ll_Album) {
            if (folderWindow.isShowing()) {
                folderWindow.dismiss();
            } else {
                if (images != null && images.size() > 0) {
                    folderWindow.showAsDropDown(mTopLayout);
                    if (!config.isSingleDirectReturn) {
                        List<LocalMedia> selectedImages = adapter.getSelectedImages();
                        folderWindow.notifyDataCheckedStatus(selectedImages);
                    }
                }
            }
        }

        if (id == R.id.picture_id_preview) {
            onPreview();
        }

        if (id == R.id.picture_tv_ok) {
            if (folderWindow != null
                    && folderWindow.isShowing()) {
                folderWindow.dismiss();
            } else {
                onComplete();
            }

        }
    }

    private void onPreview() {
        List<LocalMedia> selectedImages = adapter.getSelectedImages();
        List<LocalMedia> medias = new ArrayList<>();
        int size = selectedImages.size();
        for (int i = 0; i < size; i++) {
            LocalMedia media = selectedImages.get(i);
            medias.add(media);
        }
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(PictureConfig.EXTRA_PREVIEW_SELECT_LIST, (ArrayList<? extends Parcelable>) medias);
        bundle.putParcelableArrayList(PictureConfig.EXTRA_SELECT_LIST, (ArrayList<? extends Parcelable>) selectedImages);
        bundle.putBoolean(PictureConfig.EXTRA_BOTTOM_PREVIEW, true);
        JumpUtils.startPicturePreviewActivity(getContext(), bundle);

        overridePendingTransition(R.anim.rc_picture_anim_enter, R.anim.rc_picture_anim_fade_in);
    }

    private void onComplete() {
        List<LocalMedia> images = adapter.getSelectedImages();
        LocalMedia image = images.size() > 0 ? images.get(0) : null;
        String mimeType = image != null ? image.getMimeType() : "";
        // 如果设置了图片最小选择数量，则判断是否满足条件
        int size = images.size();
        boolean eqImg = PictureMimeType.eqImage(mimeType);
        if (config.minSelectNum > 0 && config.selectionMode == PictureConfig.MULTIPLE) {
            if (size < config.minSelectNum) {
                String str = eqImg ? getString(R.string.picture_min_img_num, config.minSelectNum)
                        : getString(R.string.picture_min_video_num, config.minSelectNum);
                ToastUtils.s(getContext(), str);
                return;
            }
        }
        onResult(images);
    }

    @Override
    public void onItemClick(boolean isCameraFolder, String folderName, List<LocalMedia> images) {
        boolean camera = config.isCamera ? isCameraFolder : false;
        adapter.setShowCamera(camera);
        mTvPictureTitle.setText(folderName);
        folderWindow.dismiss();
        adapter.bindImagesData(images);
        mPictureRecycler.smoothScrollToPosition(0);
    }

    @Override
    public void onTakePhoto() {
        // 启动相机拍照,先判断手机是否有拍照权限
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            startCamera();
        } else {
            PermissionChecker
                    .requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA}, PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onChange(List<LocalMedia> selectImages) {
        changeImageNumber(selectImages);
    }

    @Override
    public void onPictureClick(LocalMedia media, int position) {
        if (config.selectionMode == PictureConfig.SINGLE && config.isSingleDirectReturn) {
            List<LocalMedia> list = new ArrayList<>();
            list.add(media);
            handlerResult(list);
        } else {
            List<LocalMedia> images = adapter.getImages();
            startPreview(images, position);
        }
    }

    /**
     * preview image and video
     *
     * @param previewImages
     * @param position
     */
    public void startPreview(List<LocalMedia> previewImages, int position) {
        LocalMedia media = previewImages.get(position);
        String mimeType = media.getMimeType();
        Bundle bundle = new Bundle();
        List<LocalMedia> result = new ArrayList<>();
//        if (PictureMimeType.eqVideo(mimeType)) {
//            if (config.selectionMode == PictureConfig.SINGLE) {
//                result.add(media);
//                onResult(result);
//            } else {
//                bundle.putString("video_path", media.getPath());
//                JumpUtils.startPictureVideoPlayActivity(getContext(), bundle);
//            }
//        } else {
        List<LocalMedia> selectedImages = adapter.getSelectedImages();
        ImagesObservable.getInstance().savePreviewMediaData(new ArrayList<>(previewImages));
        bundle.putParcelableArrayList(PictureConfig.EXTRA_SELECT_LIST, (ArrayList<? extends Parcelable>) selectedImages);
        bundle.putInt(PictureConfig.EXTRA_POSITION, position);
        JumpUtils.startPicturePreviewActivity(getContext(), bundle);
        overridePendingTransition(R.anim.rc_picture_anim_enter, R.anim.rc_picture_anim_fade_in);
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.REQUEST_CAMERA:
                    requestCamera(data);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 摄像头后处理方式
     *
     * @param media
     * @param mimeType
     */
    private void cameraHandleResult(LocalMedia media, String mimeType) {
        // 如果是单选 拍照后直接返回
        // 不裁剪 不压缩 直接返回结果
        List<LocalMedia> result = new ArrayList<>();
        result.add(media);
        onResult(result);

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
        if (adapter != null) {
            if (config.selectionMode == PictureConfig.SINGLE) {
                // 单选模式
                if (config.isSingleDirectReturn) {
                    cameraHandleResult(media, mimeType);
                } else {
                    // 如果是单选，则清空已选中的并刷新列表(作单一选择)
                    images.add(0, media);
                    List<LocalMedia> selectedImages = adapter.getSelectedImages();
                    mimeType = selectedImages.size() > 0 ? selectedImages.get(0).getMimeType() : "";
                    boolean mimeTypeSame = PictureMimeType.isMimeTypeSame(mimeType, media.getMimeType());
                    // 类型相同或还没有选中才加进选中集合中
                    if (mimeTypeSame || selectedImages.size() == 0) {
                        singleRadioMediaImage();
                        selectedImages.add(media);
                        adapter.bindSelectImages(selectedImages);
                    }
                }
            } else {
                // 多选模式
                images.add(0, media);
                List<LocalMedia> selectedImages = adapter.getSelectedImages();
                // 没有到最大选择量 才做默认选中刚拍好的
                if (selectedImages.size() < config.maxSelectNum) {
                    selectedImages.add(media);
                    adapter.bindSelectImages(selectedImages);
                } else {
                    ToastUtils.s(this, getString(R.string.picture_message_max_num_fir)
                            + config.maxSelectNum + getString(R.string.picture_message_max_num_sec));
                }
            }
            adapter.notifyDataSetChanged();
            // 解决部分手机拍照完Intent.ACTION_MEDIA_SCANNER_SCAN_FILE，不及时刷新问题手动添加
            manualSaveFolder(media);
            mTvEmpty.setVisibility(images.size() > 0 ? View.INVISIBLE : View.VISIBLE);
            onPictureClick(media, 0);
        }
    }


    /**
     * 单选图片
     */
    private void singleRadioMediaImage() {
        List<LocalMedia> selectImages = adapter.getSelectedImages();
        if (selectImages != null
                && selectImages.size() > 0) {
            selectImages.clear();
        }
    }

    /**
     * 手动添加拍照后的相片到图片列表，并设为选中
     *
     * @param media
     */
    private void manualSaveFolder(LocalMedia media) {
        try {
            createNewFolder(foldersList);
            LocalMediaFolder folder = getImageFolder(media.getPath(), foldersList);
            LocalMediaFolder cameraFolder = foldersList.size() > 0 ? foldersList.get(0) : null;
            if (cameraFolder != null && folder != null) {
                // 相机胶卷
                cameraFolder.setFirstImagePath(media.getPath());
                cameraFolder.setImages(images);
                cameraFolder.setImageNum(cameraFolder.getImageNum() + 1);
                // 拍照相册
                int num = folder.getImageNum() + 1;
                folder.setImageNum(num);
                folder.getImages().add(0, media);
                folder.setFirstImagePath(cameraPath);
                folderWindow.bindFolder(foldersList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commonBroadcastReceiver != null) {
            BroadcastManager.getInstance(this)
                    .unregisterReceiver(commonBroadcastReceiver,
                            BroadcastAction.ACTION_SELECTED_DATA,
                            BroadcastAction.ACTION_PREVIEW_COMPRESSION);
            commonBroadcastReceiver = null;
        }
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
    }

    private BroadcastReceiver commonBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras;
            switch (action) {
                case BroadcastAction.ACTION_SELECTED_DATA:
                    // 预览时勾选图片更新回调
                    extras = intent.getExtras();
                    if (extras != null && adapter != null) {
                        List<LocalMedia> selectImages = extras.
                                getParcelableArrayList("selectImages");
                        int position = extras.getInt("position");
                        anim = true;
                        adapter.bindSelectImages(selectImages);
                        adapter.notifyItemChanged(position);
                    }
                    break;
                case BroadcastAction.ACTION_PREVIEW_COMPRESSION:
                    extras = intent.getExtras();
                    if (extras != null) {
                        List<LocalMedia> selectImages = extras.getParcelableArrayList("selectImages");
                        if (selectImages != null && selectImages.size() > 0) {
                            // 取出第1个判断是否是图片，视频和图片只能二选一，不必考虑图片和视频混合
                            String mimeType = selectImages.get(0).getMimeType();
                            onResult(selectImages);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PictureConfig.APPLY_STORAGE_PERMISSIONS_CODE:
                // 存储权限
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mHandler.sendEmptyMessage(SHOW_DIALOG);
                    readLocalMedia();
                } else {
                    ToastUtils.s(getContext(), getString(R.string.picture_jurisdiction));
                    onBackPressed();
                }
                break;
            case PictureConfig.APPLY_CAMERA_PERMISSIONS_CODE:
                // 相机权限
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onTakePhoto();
                } else {
                    ToastUtils.s(getContext(), getString(R.string.picture_camera));
                }
                break;
        }
    }
}
