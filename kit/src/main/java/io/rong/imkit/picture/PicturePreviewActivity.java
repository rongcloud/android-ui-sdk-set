package io.rong.imkit.picture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.View;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.rong.imkit.R;
import io.rong.imkit.picture.adapter.ViewPagerAdapter;
import io.rong.imkit.picture.anim.OptAnimationLoader;
import io.rong.imkit.picture.broadcast.BroadcastAction;
import io.rong.imkit.picture.broadcast.BroadcastManager;
import io.rong.imkit.picture.config.PictureConfig;
import io.rong.imkit.picture.config.PictureMimeType;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.picture.observable.ImagesObservable;
import io.rong.imkit.picture.tools.ScreenUtils;
import io.rong.imkit.picture.tools.ToastUtils;

/**
 * @author：luck
 * @data：2016/1/29 下午21:50
 * @描述:图片预览
 */
public class PicturePreviewActivity extends PictureBaseActivity implements
        View.OnClickListener, ViewPagerAdapter.OnCallBackActivity {
    protected ImageView picture_left_back;
    protected TextView mTvPictureOk;
    protected ViewPager2 viewPager;
    protected int position;
    protected boolean is_bottom_preview;
    protected List<LocalMedia> images = new ArrayList<>();
    protected List<LocalMedia> selectImages = new ArrayList<>();
    protected TextView check;
    protected ViewPagerAdapter adapter;
    protected Animation animation;
    protected View btnCheck;
    protected boolean refresh;
    protected int index;
    protected int screenWidth;
    protected Handler mHandler;
    protected FrameLayout selectBarLayout, topLayout;
    protected CheckBox mCbOriginal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BroadcastManager.getInstance(this).registerReceiver(commonBroadcastReceiver,
                BroadcastAction.ACTION_CLOSE_PREVIEW);
    }

    @Override
    public int getResourceId() {
        return R.layout.rc_picture_preview;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
        mHandler = new Handler();
        screenWidth = ScreenUtils.getScreenWidth(this);
        animation = OptAnimationLoader.loadAnimation(this, R.anim.rc_picture_anim_modal_in);
        picture_left_back = findViewById(R.id.picture_left_back);
        topLayout = findViewById(R.id.fl_top);
        viewPager = findViewById(R.id.preview_pager);
        btnCheck = findViewById(R.id.btnCheck);
        check = findViewById(R.id.check);
        picture_left_back.setOnClickListener(this);
        mTvPictureOk = findViewById(R.id.tv_ok);
        mCbOriginal = findViewById(R.id.cb_original);
        selectBarLayout = findViewById(R.id.select_bar_layout);
        topLayout.setOnClickListener(this);
        mTvPictureOk.setOnClickListener(this);
        selectBarLayout.setOnClickListener(this);
        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);
        btnCheck.setOnClickListener(this);
        selectImages = getIntent().
                getParcelableArrayListExtra(PictureConfig.EXTRA_SELECT_LIST);
        is_bottom_preview = getIntent().
                getBooleanExtra(PictureConfig.EXTRA_BOTTOM_PREVIEW, false);
        // 底部预览按钮过来
        if (is_bottom_preview) {
            images = getIntent().
                    getParcelableArrayListExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST);
        } else {
            images = ImagesObservable.getInstance().readPreviewMediaData();
        }

        initViewPageAdapterData();
        // 原图
        LocalMedia media = images.get(position);
        boolean eqVideo = PictureMimeType.eqVideo(media.getMimeType());
        mCbOriginal.setVisibility(eqVideo ? View.GONE : View.VISIBLE);
        mCbOriginal.setChecked(config.isCheckOriginalImage);
        mCbOriginal.setText(getString(R.string.rc_picture_original_image_size, getSize(media.getSize())));
        mCbOriginal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.isCheckOriginalImage = isChecked;
            }
        });
    }

    private String getSize(long size) {
        if (size / 1024 / 1024 < 1) {
            return String.format("%dK", size / 1024);
        } else {
            return String.format("%.2fM", size / 1024f / 1024f);
        }
    }

    /**
     * ViewPage滑动数据变化回调
     *
     * @param media
     */
    protected void onPageSelectedChange(LocalMedia media) {

    }

    /**
     * 动态设置相册主题
     */
    @Override
    public void initPictureSelectorStyle() {
        mCbOriginal.setButtonDrawable(ContextCompat
                .getDrawable(this, R.drawable.rc_picture_original_checkbox));
    }


    /**
     * 单选图片
     */
    private void singleRadioMediaImage() {
        LocalMedia media = selectImages != null && selectImages.size() > 0 ? selectImages.get(0) : null;
        if (media != null) {
            Bundle bundle = new Bundle();
            bundle.putInt("position", media.getPosition());
            bundle.putParcelableArrayList("selectImages", (ArrayList<? extends Parcelable>) selectImages);
            BroadcastManager.getInstance(this)
                    .action(BroadcastAction.ACTION_SELECTED_DATA)
                    .extras(bundle)
                    .broadcast();
            selectImages.clear();
        }
    }

    /**
     * 初始化ViewPage数据
     */
    private void initViewPageAdapterData() {
        // adapter = new PictureSimpleFragmentAdapter(config, images, this, this);
        adapter = new ViewPagerAdapter(config, images, this, this);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                LocalMedia media = images.get(position);
                index = media.getPosition();
                if (config.checkNumMode) {
                    check.setText(MessageFormat.format("{0}", media.getNum()));
                    notifyCheckChanged(media);
                }
                onImageChecked(position);
                boolean eqVideo = PictureMimeType.eqVideo(media.getMimeType());
                mCbOriginal.setVisibility(eqVideo ? View.GONE : View.VISIBLE);
                mCbOriginal.setChecked(config.isCheckOriginalImage);
                mCbOriginal.setText(getString(R.string.rc_picture_original_image_size, getSize(media.getSize())));
            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position, false);
        onSelectNumChange(false);
        onImageChecked(position);
        if (images.size() > 0) {
            LocalMedia media = images.get(position);
            index = media.getPosition();
            if (config.checkNumMode) {
                check.setText(media.getNum() + "");
                notifyCheckChanged(media);
            }
        }
    }

    /**
     * 选择按钮更新
     */
    private void notifyCheckChanged(LocalMedia imageBean) {
        if (config.checkNumMode) {
            check.setText("");
            for (LocalMedia media : selectImages) {
                if (media.getPath().equals(imageBean.getPath())) {
                    imageBean.setNum(media.getNum());
                    check.setText(String.valueOf(imageBean.getNum()));
                }
            }
        }
    }

    /**
     * 更新选择的顺序
     */
    private void subSelectPosition() {
        for (int index = 0, len = selectImages.size(); index < len; index++) {
            LocalMedia media = selectImages.get(index);
            media.setNum(index + 1);
        }
    }

    /**
     * 判断当前图片是否选中
     *
     * @param position
     */
    public void onImageChecked(int position) {
        if (images != null && images.size() > 0) {
            LocalMedia media = images.get(position);
            check.setSelected(isSelected(media));
        } else {
            check.setSelected(false);
        }
    }

    /**
     * 当前图片是否选中
     *
     * @param image
     * @return
     */
    public boolean isSelected(LocalMedia image) {
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新图片选择数量
     */

    protected void onSelectNumChange(boolean isRefresh) {
        this.refresh = isRefresh;
        boolean enable = selectImages.size() != 0;
        mTvPictureOk.setTextColor(selectImages.size() > 0 ? getResources().getColor(R.color.rc_main_theme) : getResources().getColor(R.color.rc_main_theme_lucency));
        mTvPictureOk.setText(config.selectionMode == PictureConfig.SINGLE || !enable ? getString(R.string.rc_picture_send) :
                getString(R.string.rc_picture_send_num) + "(" + selectImages.size() + ")");
        if (enable) {
            mTvPictureOk.setEnabled(true);
            mTvPictureOk.setSelected(true);
        } else {
            mTvPictureOk.setEnabled(false);
            mTvPictureOk.setSelected(false);
        }
        updateSelector(refresh);
    }

    /**
     * 更新图片列表选中效果
     *
     * @param isRefresh
     */
    protected void updateSelector(boolean isRefresh) {
        if (isRefresh) {
            Bundle bundle = new Bundle();
            bundle.putInt("position", index);
            bundle.putParcelableArrayList("selectImages", (ArrayList<? extends Parcelable>) selectImages);
            BroadcastManager.getInstance(this)
                    .action(BroadcastAction.ACTION_SELECTED_DATA)
                    .extras(bundle)
                    .broadcast();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.picture_left_back) {
            onBackPressed();
        } else if (id == R.id.tv_ok) {
            onComplete();
        } else if (id == R.id.btnCheck) {
            LocalMedia currentMedia = images.get(position);
            if (PictureMimeType.eqVideo(currentMedia.getMimeType())) {
                int maxDuration = config.videoDurationLimit;
                if (maxDuration < 1)
                    maxDuration = PictureConfig.DEFAULT_VIDEO_DURATION_LIMIT;
                if (TimeUnit.MILLISECONDS.toSeconds(currentMedia.getDuration()) > maxDuration) {
                    new AlertDialog.Builder(PicturePreviewActivity.this)
                            .setMessage(getResources().getString(R.string.rc_picsel_selected_max_time_span_with_param, maxDuration / 60))
                            .setPositiveButton(R.string.rc_confirm, null)
                            .setCancelable(false)
                            .create()
                            .show();
                    return;
                }
            }
            onCheckedComplete();
        } else if (id == R.id.select_bar_layout || id == R.id.fl_top) {
            return;
        }
    }

    protected void onCheckedComplete() {
        if (images != null && images.size() > 0) {
            LocalMedia image = images.get(viewPager.getCurrentItem());
            // 刷新图片列表中图片状态
            boolean isChecked;
            if (!check.isSelected()) {
                isChecked = true;
                check.setSelected(true);
                check.startAnimation(animation);
            } else {
                isChecked = false;
                check.setSelected(false);
            }
            if (selectImages.size() >= config.maxSelectNum && isChecked) {
                ToastUtils.s(getContext(), getString(R.string.rc_picture_message_max_num_fir)
                        + config.maxSelectNum + getString(R.string.rc_picture_message_max_num_sec));
                check.setSelected(false);
                return;
            }
            if (isChecked) {
                // 如果是单选，则清空已选中的并刷新列表(作单一选择)
                if (config.selectionMode == PictureConfig.SINGLE) {
                    singleRadioMediaImage();
                }
                selectImages.add(image);
                onSelectedChange(true, image);
                image.setNum(selectImages.size());
                if (config.checkNumMode) {
                    check.setText(String.valueOf(image.getNum()));
                }
            } else {
                for (LocalMedia media : selectImages) {
                    if (media.getPath().equals(image.getPath())) {
                        selectImages.remove(media);
                        onSelectedChange(false, image);
                        subSelectPosition();
                        notifyCheckChanged(media);
                        break;
                    }
                }
            }
            onSelectNumChange(true);
        }
    }

    /**
     * 选中或是移除
     *
     * @param isAddRemove
     * @param media
     */
    protected void onSelectedChange(boolean isAddRemove, LocalMedia media) {

    }

    protected void onComplete() {
        // 如果设置了图片最小选择数量，则判断是否满足条件
        int size = selectImages.size();
        LocalMedia image = selectImages.size() > 0 ? selectImages.get(0) : null;
        String mimeType = image != null ? image.getMimeType() : "";
        if (config.minSelectNum > 0) {
            if (size < config.minSelectNum && config.selectionMode == PictureConfig.MULTIPLE) {
                boolean eqImg = PictureMimeType.eqImage(mimeType);
                String str = eqImg ? getString(R.string.rc_picture_min_img_num, config.minSelectNum)
                        : getString(R.string.rc_picture_min_video_num, config.minSelectNum);
                ToastUtils.s(getContext(), str);
                return;
            }
        }
        onResult(selectImages);
    }

    @Override
    public void onResult(List<LocalMedia> images) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("selectImages", (ArrayList<? extends Parcelable>) images);
        BroadcastManager.getInstance(this)
                .action(BroadcastAction.ACTION_PREVIEW_COMPRESSION)
                .extras(bundle)
                .broadcast();
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        closeActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImagesObservable.getInstance().clearPreviewMediaData();
        if (commonBroadcastReceiver != null) {
            BroadcastManager.getInstance(this).unregisterReceiver(commonBroadcastReceiver,
                    BroadcastAction.ACTION_CLOSE_PREVIEW);
            commonBroadcastReceiver = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
    }

    @Override
    public void onActivityBackPressed() {
        onBackPressed();
    }

    private BroadcastReceiver commonBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BroadcastAction.ACTION_CLOSE_PREVIEW:
                    // 压缩完后关闭预览界面
                    dismissDialog();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onBackPressed();
                        }
                    }, 150);
                    break;
                default:
                    break;
            }
        }
    };

}
