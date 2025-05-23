package io.rong.imkit.picture.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class LocalMedia implements Parcelable {
    /** original path */
    private String path;

    /** video duration */
    private long duration;

    /** If the selected */
    private boolean isChecked;

    /** media position of list */
    public int position;

    /** The media number of qq choose styles */
    private int num;

    /** The media resource type */
    private String mimeType;

    /** Gallery selection mode */
    private int chooseModel;

    /** image or video width */
    private int width;

    /** image or video height */
    private int height;

    /** file size */
    private long size;

    /** Whether the original image is displayed */
    private boolean isOriginal;

    public LocalMedia() {
        // default implementation ignored
    }

    public LocalMedia(String path, long duration, int chooseModel, String mimeType) {
        this.path = path;
        this.duration = duration;
        this.chooseModel = chooseModel;
        this.mimeType = mimeType;
    }

    public LocalMedia(
            String path,
            long duration,
            int chooseModel,
            String mimeType,
            int width,
            int height,
            long size) {
        this.path = path;
        this.duration = duration;
        this.chooseModel = chooseModel;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
        this.size = size;
    }

    public LocalMedia(
            String path, long duration, boolean isChecked, int position, int num, int chooseModel) {
        this.path = path;
        this.duration = duration;
        this.isChecked = isChecked;
        this.position = position;
        this.num = num;
        this.chooseModel = chooseModel;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public String getMimeType() {
        return TextUtils.isEmpty(mimeType) ? "image/jpeg" : mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getChooseModel() {
        return chooseModel;
    }

    public void setChooseModel(int chooseModel) {
        this.chooseModel = chooseModel;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isOriginal() {
        return isOriginal;
    }

    public void setOriginal(boolean original) {
        isOriginal = original;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.path);
        dest.writeLong(this.duration);
        dest.writeByte(this.isChecked ? (byte) 1 : (byte) 0);
        dest.writeInt(this.position);
        dest.writeInt(this.num);
        dest.writeString(this.mimeType);
        dest.writeInt(this.chooseModel);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeLong(this.size);
        dest.writeByte(this.isOriginal ? (byte) 1 : (byte) 0);
    }

    protected LocalMedia(Parcel in) {
        this.path = in.readString();
        this.duration = in.readLong();
        this.isChecked = in.readByte() != 0;
        this.position = in.readInt();
        this.num = in.readInt();
        this.mimeType = in.readString();
        this.chooseModel = in.readInt();
        this.width = in.readInt();
        this.height = in.readInt();
        this.size = in.readLong();
        this.isOriginal = in.readByte() != 0;
    }

    public static final Creator<LocalMedia> CREATOR =
            new Creator<LocalMedia>() {
                @Override
                public LocalMedia createFromParcel(Parcel source) {
                    return new LocalMedia(source);
                }

                @Override
                public LocalMedia[] newArray(int size) {
                    return new LocalMedia[size];
                }
            };
}
