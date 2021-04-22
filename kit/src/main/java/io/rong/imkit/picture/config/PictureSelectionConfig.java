package io.rong.imkit.picture.config;

import android.content.pm.ActivityInfo;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.StyleRes;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.picture.engine.ImageEngine;
import io.rong.imkit.picture.entity.LocalMedia;
import io.rong.imkit.picture.tools.PictureFileUtils;


public final class PictureSelectionConfig implements Parcelable {
    public int chooseMode;
    public boolean camera;
    public boolean isSingleDirectReturn;
    public String suffixType;
    public String specifiedFormat;
    public int requestedOrientation;
    public boolean isAndroidQTransform;
    @StyleRes
    public int themeStyleId;
    public int selectionMode;
    public int maxSelectNum;
    public int minSelectNum;
    public int imageSpanCount;
    public boolean zoomAnim;
    public boolean isCamera;
    public boolean isGif;
    public boolean enablePreview;
    public boolean enPreviewVideo;
    public boolean checkNumMode;
    public boolean isNotPreviewDownload;
    public ImageEngine imageEngine;
    public List<LocalMedia> selectionMedias;
    public String cameraFileName;
    public boolean isCheckOriginalImage;
    public int videoDurationLimit;

    private void reset() {
        chooseMode = PictureMimeType.ofImage();
        camera = false;
        themeStyleId = R.style.picture_WeChat_style;
        selectionMode = PictureConfig.MULTIPLE;
        maxSelectNum = 9;
        minSelectNum = 0;
        imageSpanCount = 4;
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
        isAndroidQTransform = true;
        isCamera = true;
        isGif = false;
        isCheckOriginalImage = false;
        isSingleDirectReturn = false;
        enablePreview = true;
        enPreviewVideo = true;
        checkNumMode = false;
        isNotPreviewDownload = false;
        zoomAnim = true;
        suffixType = PictureFileUtils.POSTFIX;
        cameraFileName = "";
        specifiedFormat = "";
        selectionMedias = new ArrayList<>();
        imageEngine = null;
        videoDurationLimit = PictureConfig.DEFAULT_VIDEO_DURATION_LIMIT;
    }

    public static PictureSelectionConfig getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public static PictureSelectionConfig getCleanInstance() {
        PictureSelectionConfig selectionSpec = getInstance();
        selectionSpec.reset();
        return selectionSpec;
    }

    private static final class InstanceHolder {
        private static final PictureSelectionConfig INSTANCE = new PictureSelectionConfig();
    }

    public PictureSelectionConfig() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.chooseMode);
        dest.writeByte(this.camera ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isSingleDirectReturn ? (byte) 1 : (byte) 0);
        dest.writeString(this.suffixType);
        dest.writeString(this.cameraFileName);
        dest.writeString(this.specifiedFormat);
        dest.writeInt(this.themeStyleId);
        dest.writeInt(this.selectionMode);
        dest.writeInt(this.maxSelectNum);
        dest.writeInt(this.minSelectNum);
        dest.writeInt(this.requestedOrientation);
        dest.writeInt(this.imageSpanCount);
        dest.writeByte(this.isCheckOriginalImage ? (byte) 1 : (byte) 0);
        dest.writeByte(this.zoomAnim ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isCamera ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isGif ? (byte) 1 : (byte) 0);
        dest.writeByte(this.enablePreview ? (byte) 1 : (byte) 0);
        dest.writeByte(this.enPreviewVideo ? (byte) 1 : (byte) 0);
        dest.writeByte(this.checkNumMode ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isNotPreviewDownload ? (byte) 1 : (byte) 0);
        dest.writeTypedList(this.selectionMedias);
        dest.writeInt(this.videoDurationLimit);
    }

    protected PictureSelectionConfig(Parcel in) {
        this.chooseMode = in.readInt();
        this.camera = in.readByte() != 0;
        this.isSingleDirectReturn = in.readByte() != 0;
        this.suffixType = in.readString();
        this.cameraFileName = in.readString();
        this.specifiedFormat = in.readString();
        this.themeStyleId = in.readInt();
        this.selectionMode = in.readInt();
        this.maxSelectNum = in.readInt();
        this.minSelectNum = in.readInt();
        this.requestedOrientation = in.readInt();
        this.imageSpanCount = in.readInt();
        this.zoomAnim = in.readByte() != 0;
        this.isCamera = in.readByte() != 0;
        this.isGif = in.readByte() != 0;
        this.isCheckOriginalImage = in.readByte() != 0;
        this.enablePreview = in.readByte() != 0;
        this.enPreviewVideo = in.readByte() != 0;
        this.checkNumMode = in.readByte() != 0;
        this.isNotPreviewDownload = in.readByte() != 0;
        this.selectionMedias = in.createTypedArrayList(LocalMedia.CREATOR);
        this.videoDurationLimit = in.readInt();
    }

    public static final Creator<PictureSelectionConfig> CREATOR = new Creator<PictureSelectionConfig>() {
        @Override
        public PictureSelectionConfig createFromParcel(Parcel source) {
            return new PictureSelectionConfig(source);
        }

        @Override
        public PictureSelectionConfig[] newArray(int size) {
            return new PictureSelectionConfig[size];
        }
    };
}
