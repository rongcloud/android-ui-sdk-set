package io.rong.sticker.message;

import android.os.Parcel;
import io.rong.common.rlog.RLog;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.MessageContent;
import io.rong.sticker.model.Sticker;
import java.io.UnsupportedEncodingException;
import org.json.JSONException;
import org.json.JSONObject;

/** Created by luoyanlong on 2018/08/03. 一个表情消息 */
@MessageTag(value = "RC:StkMsg", flag = MessageTag.ISCOUNTED)
public class StickerMessage extends MessageContent {
    // <editor-fold desc="* 1. 自身内部变量">
    private static final String TAG = StickerMessage.class.getSimpleName();
    private String packageId;
    private String stickerId;
    private String digest;
    private int width;
    private int height;
    // </editor-fold>

    // <editor-fold desc="* 2. 对外构造方法">
    public StickerMessage() {
        // default implementation ignored
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

    // </editor-fold>

    // <editor-fold desc="* 3. 二进制 Encode & decode 编解码方法">
    @Override
    public byte[] encode() {
        JSONObject jsonObj = super.getBaseJsonObject();
        try {
            jsonObj.put("packageId", packageId);
            jsonObj.put("stickerId", stickerId);
            jsonObj.put("digest", digest);
            jsonObj.put("width", width);
            jsonObj.put("height", height);
        } catch (JSONException e) {
            RLog.e(TAG, e.getMessage());
        }

        try {
            return jsonObj.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            RLog.e(TAG, e.getMessage());
        }

        return null;
    }

    public StickerMessage(byte[] data) {
        try {
            String jsonStr = new String(data, "UTF-8");
            JSONObject jsonObj = new JSONObject(jsonStr);
            super.parseBaseJsonObject(jsonObj);
            packageId = jsonObj.optString("packageId");
            stickerId = jsonObj.optString("stickerId");
            digest = jsonObj.optString("digest");
            width = jsonObj.optInt("width");
            height = jsonObj.optInt("height");
        } catch (UnsupportedEncodingException | JSONException e) {
            RLog.e(TAG, e.getMessage());
        }
    }
    // </editor-fold>

    // <editor-fold desc="* 4. Parcel 的序列化方法">
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToBaseInfoParcel(dest);
        dest.writeString(this.packageId);
        dest.writeString(this.stickerId);
        dest.writeString(this.digest);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
    }

    public StickerMessage(Parcel in) {
        super.readFromBaseInfoParcel(in);
        this.packageId = in.readString();
        this.stickerId = in.readString();
        this.digest = in.readString();
        this.width = in.readInt();
        this.height = in.readInt();
    }

    public static final Creator<StickerMessage> CREATOR =
            new Creator<StickerMessage>() {
                @Override
                public StickerMessage createFromParcel(Parcel source) {
                    return new StickerMessage(source);
                }

                @Override
                public StickerMessage[] newArray(int size) {
                    return new StickerMessage[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }
    // </editor-fold>

    // <editor-fold desc="* 5. get & set 方法">
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
    // <

}
