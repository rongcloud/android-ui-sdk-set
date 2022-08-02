package io.rong.sticker.message;

import android.os.Parcel;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import io.rong.sticker.model.Sticker;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.MessageContent;

/**
 * Created by luoyanlong on 2018/08/03.
 * 一个表情消息
 */
@MessageTag(value = "RC:StkMsg", flag = MessageTag.ISCOUNTED)
public class StickerMessage extends MessageContent {

    private String packageId;
    private String stickerId;
    private String digest;
    private int width;
    private int height;

    @Override
    public byte[] encode() {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("packageId", packageId);
            jsonObj.put("stickerId", stickerId);
            jsonObj.put("digest", digest);
            jsonObj.put("width", width);
            jsonObj.put("height", height);
        } catch (JSONException e) {
            Log.e("JSONException", e.getMessage());
        }

        try {
            return jsonObj.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageId);
        dest.writeString(this.stickerId);
        dest.writeString(this.digest);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
    }

    public StickerMessage() {
    }

    public StickerMessage(byte[] data) {
        try {
            String jsonStr = new String(data, "UTF-8");
            JSONObject jsonObj = new JSONObject(jsonStr);
            packageId = jsonObj.optString("packageId");
            stickerId = jsonObj.optString("stickerId");
            digest = jsonObj.optString("digest");
            width = jsonObj.optInt("width");
            height = jsonObj.optInt("height");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static StickerMessage obtain(Sticker sticker) {
        StickerMessage stickerMessage = new StickerMessage();
        stickerMessage.setPackageId(sticker.getPackageId());
        stickerMessage.setStickerId(sticker.getStickerId());
        stickerMessage.setDigest(sticker.getDigest());
        stickerMessage.setWidth(sticker.getWidth());
        stickerMessage.setHeight(sticker.getHeight());
        return stickerMessage;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getStickerId() {
        return stickerId;
    }

    public void setStickerId(String stickerId) {
        this.stickerId = stickerId;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
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

    public StickerMessage(Parcel in) {
        this.packageId = in.readString();
        this.stickerId = in.readString();
        this.digest = in.readString();
        this.width = in.readInt();
        this.height = in.readInt();
    }

    public static final Creator<StickerMessage> CREATOR = new Creator<StickerMessage>() {
        @Override
        public StickerMessage createFromParcel(Parcel source) {
            return new StickerMessage(source);
        }

        @Override
        public StickerMessage[] newArray(int size) {
            return new StickerMessage[size];
        }
    };
}
