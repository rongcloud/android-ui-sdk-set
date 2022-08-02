package io.rong.imkit.picture;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.picture.config.PictureConfig;
import io.rong.imkit.picture.config.PictureMimeType;
import io.rong.imkit.picture.config.PictureSelectionConfig;
import io.rong.imkit.picture.engine.ImageEngine;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.picture.tools.DoubleUtils;

/**
 * @author：luck
 * @date：2017-5-24 21:30
 * @describe：PictureSelectionModel
 */

public class PictureSelectionModel {
    private PictureSelectionConfig selectionConfig;
    private PictureSelector selector;

    public PictureSelectionModel(PictureSelector selector, int chooseMode) {
        this.selector = selector;
        selectionConfig = PictureSelectionConfig.getCleanInstance();
        selectionConfig.chooseMode = chooseMode;
    }

    public PictureSelectionModel(PictureSelector selector, int chooseMode, boolean camera) {
        this.selector = selector;
        selectionConfig = PictureSelectionConfig.getCleanInstance();
        selectionConfig.camera = camera;
        selectionConfig.chooseMode = chooseMode;
    }


    /**
     * Change the desired orientation of this activity.  If the activity
     * is currently in the foreground or otherwise impacting the screen
     * orientation, the screen will immediately be changed (possibly causing
     * the activity to be restarted). Otherwise, this will be used the next
     * time the activity is visible.
     *
     * @param requestedOrientation An orientation constant as used in
     *                             {@link ActivityInfo#screenOrientation ActivityInfo.screenOrientation}.
     */
    public PictureSelectionModel setRequestedOrientation(int requestedOrientation) {
        selectionConfig.requestedOrientation = requestedOrientation;
        return this;
    }

    /**
     * @param engine Image Load the engine
     * @return
     */
    public PictureSelectionModel loadImageEngine(ImageEngine engine) {
        if (selectionConfig.imageEngine != engine) {
            selectionConfig.imageEngine = engine;
        }
        return this;
    }

    /**
     * @param selectionMode PictureSelector Selection model and PictureConfig.MULTIPLE or PictureConfig.SINGLE
     * @return
     */
    public PictureSelectionModel selectionMode(int selectionMode) {
        selectionConfig.selectionMode = selectionMode;
        return this;
    }

    /**
     * @param maxSelectNum PictureSelector max selection
     * @return
     */
    public PictureSelectionModel maxSelectNum(int maxSelectNum) {
        selectionConfig.maxSelectNum = maxSelectNum;
        return this;
    }

    /**
     * @param minSelectNum PictureSelector min selection
     * @return
     */
    public PictureSelectionModel minSelectNum(int minSelectNum) {
        selectionConfig.minSelectNum = minSelectNum;
        return this;
    }

    /**
     * @param Select whether to return directly
     * @return
     */
    public PictureSelectionModel isSingleDirectReturn(boolean isSingleDirectReturn) {
        selectionConfig.isSingleDirectReturn = selectionConfig.selectionMode
                == PictureConfig.SINGLE ? isSingleDirectReturn : false;
        return this;
    }


    /**
     * @param imageSpanCount PictureSelector image span count
     * @return
     */
    public PictureSelectionModel imageSpanCount(int imageSpanCount) {
        selectionConfig.imageSpanCount = imageSpanCount;
        return this;
    }


    /**
     * Camera custom local file name
     * # Such as xxx.png
     *
     * @param fileName
     * @return
     */
    public PictureSelectionModel cameraFileName(String fileName) {
        selectionConfig.cameraFileName = fileName;
        return this;
    }


    /**
     * @param zoomAnim Picture list zoom anim
     * @return
     */
    public PictureSelectionModel isZoomAnim(boolean zoomAnim) {
        selectionConfig.zoomAnim = zoomAnim;
        return this;
    }

    /**
     * @param isCamera Whether to open camera button
     * @return
     */
    public PictureSelectionModel isCamera(boolean isCamera) {
        selectionConfig.isCamera = isCamera;
        return this;
    }


    /**
     * @param isGif Whether to open gif
     * @return
     */
    public PictureSelectionModel isGif(boolean isGif) {
        selectionConfig.isGif = isGif;
        return this;
    }

    /**
     * @param enablePreview Do you want to preview the picture?
     * @return
     */
    public PictureSelectionModel previewImage(boolean enablePreview) {
        selectionConfig.enablePreview = enablePreview;
        return this;
    }

    /**
     * @param enPreviewVideo Do you want to preview the video?
     * @return
     */
    public PictureSelectionModel previewVideo(boolean enPreviewVideo) {
        selectionConfig.enPreviewVideo = enPreviewVideo;
        return this;
    }

    /**
     * @param isNotPreviewDownload Previews do not show downloads
     * @return
     */
    public PictureSelectionModel isNotPreviewDownload(boolean isNotPreviewDownload) {
        selectionConfig.isNotPreviewDownload = isNotPreviewDownload;
        return this;
    }

    /**
     * @param Specify get image format
     * @return
     */
    public PictureSelectionModel querySpecifiedFormatSuffix(String specifiedFormat) {
        selectionConfig.specifiedFormat = specifiedFormat;
        return this;
    }


    /**
     * @param selectionMedia Select the selected picture set
     * @return
     */
    public PictureSelectionModel selectionMedia(List<LocalMedia> selectionMedia) {
        if (selectionMedia == null) {
            selectionMedia = new ArrayList<>();
        }
        if (selectionConfig.selectionMode == PictureConfig.SINGLE
                && selectionConfig.isSingleDirectReturn) {
            selectionMedia.clear();
        }
        selectionConfig.selectionMedias = selectionMedia;
        return this;
    }


    /**
     * # If you want to handle the Android Q path, if not, just return the uri，
     * The getAndroidQToPath(); field will be empty
     *
     * @param isAndroidQTransform
     * @return
     */
    public PictureSelectionModel isAndroidQTransform(boolean isAndroidQTransform) {
        selectionConfig.isAndroidQTransform = isAndroidQTransform;
        return this;
    }


    /**
     * Start to select media and wait for result.
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    public void forResult(int requestCode) {
        if (!DoubleUtils.isFastDoubleClick()) {
            Activity activity = selector.getActivity();
            if (activity == null || selectionConfig == null) {
                return;
            }
            Intent intent = new Intent(activity, PictureSelectorActivity.class);
            Fragment fragment = selector.getFragment();
            if (fragment != null) {
                fragment.startActivityForResult(intent, requestCode);
            } else {
                activity.startActivityForResult(intent, requestCode);
            }
            activity.overridePendingTransition(R.anim.rc_picture_anim_enter, R.anim.rc_picture_anim_fade_in);
        }
    }

    /**
     * # replace for setPictureWindowAnimationStyle();
     * Start to select media and wait for result.
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    @Deprecated
    public void forResult(int requestCode, int enterAnim, int exitAnim) {
        if (!DoubleUtils.isFastDoubleClick()) {
            Activity activity = selector.getActivity();
            if (activity == null) {
                return;
            }
            Intent intent = new Intent(activity, selectionConfig != null && selectionConfig.camera
                    ? PictureSelectorCameraEmptyActivity.class :
                    PictureSelectorActivity.class);
            Fragment fragment = selector.getFragment();
            if (fragment != null) {
                fragment.startActivityForResult(intent, requestCode);
            } else {
                activity.startActivityForResult(intent, requestCode);
            }
            activity.overridePendingTransition(enterAnim, exitAnim);
        }
    }


    /**
     * 不可发送video时长limit
     *
     * @param duration 单位秒
     * @return
     */
    public PictureSelectionModel videoDurationLimit(int duration) {
        selectionConfig.videoDurationLimit = duration;
        return this;
    }
}
