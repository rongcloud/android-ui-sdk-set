package io.rong.contactcard.message;

import android.os.Parcel;
import io.rong.common.RLog;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.MessageContent;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

/** Created by Beyond on 2016/12/5. */
@MessageTag(value = "RC:CardMsg", flag = MessageTag.ISCOUNTED | MessageTag.ISPERSISTED)
public class ContactMessage extends MessageContent {
    // <editor-fold desc="* 1. 自身内部变量">
    private static final String TAG = "ContactMessage";
    private static final String SEND_USER_ID = "sendUserId";
    private static final String USER_ID = "userId";
    private static final String NAME = "name";
    private static final String PORTRAIT_URI = "portraitUri";
    private static final String SEND_USER_NAME = "sendUserName";
    private static final String EXTRA = "extra";
    private static final String USER = "user";
    private static final String IS_BURN_AFTER_READ = "isBurnAfterRead";
    private static final String BURN_DURATION = "burnDuration";

    private String id;
    private String name;
    private String imgUrl;
    private String sendUserId;
    private String sendUserName;
    private String extra;
    private static final Pattern pattern = Pattern.compile("\\[/u([0-9A-Fa-f]+)\\]");

    // </editor-fold>

    // <editor-fold desc="* 2. 对外构造方法">
    public ContactMessage() {
        // default implementation ignored
    }

    public ContactMessage(
            String id,
            String name,
            String imgUrl,
            String sendUserId,
            String sendUserName,
            String extra) {
        this.id = id;
        this.name = name;
        this.imgUrl = imgUrl;
        this.sendUserId = sendUserId;
        this.sendUserName = sendUserName;
        this.extra = extra;
    }

    public static ContactMessage obtain(
            String id,
            String title,
            String imgUrl,
            String senduserId,
            String sendUserName,
            String extra) {
        return new ContactMessage(id, title, imgUrl, senduserId, sendUserName, extra);
    }

    // </editor-fold>

    // <editor-fold desc="* 3. 二进制 Encode & decode 编解码方法">
    @Override
    public byte[] encode() {
        JSONObject jsonObject = super.getBaseJsonObject();

        try {
            jsonObject.put(USER_ID, getId()); // 这里的id（联系人）不同于下边发送名片信息者的 sendUserId
            jsonObject.put(NAME, getEmotion(getName()));
            jsonObject.put(PORTRAIT_URI, getImgUrl());
            jsonObject.put(SEND_USER_ID, getSendUserId());
            jsonObject.put(SEND_USER_NAME, getEmotion(getSendUserName()));
        } catch (Exception e) {
            RLog.e(TAG, "encode " + e.getMessage());
        }

        try {
            return jsonObject.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            RLog.e(TAG, "encode " + e.getMessage());
        }
        return null;
    }

    public ContactMessage(byte[] data) {
        String jsonStr = null;
        try {
            jsonStr = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            RLog.e(TAG, e.getMessage());
        }

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            super.parseBaseJsonObject(jsonObj);
            if (jsonObj.has(USER_ID)) {
                setId(jsonObj.optString(USER_ID));
            }
            if (jsonObj.has(NAME)) {
                setName(jsonObj.optString(NAME));
            }
            if (jsonObj.has(PORTRAIT_URI)) {
                setImgUrl(jsonObj.optString(PORTRAIT_URI));
            }
            if (jsonObj.has(SEND_USER_ID)) {
                setSendUserId(jsonObj.optString(SEND_USER_ID));
            }
            if (jsonObj.has(SEND_USER_NAME)) {
                setSendUserName(jsonObj.optString(SEND_USER_NAME));
            }
        } catch (JSONException e) {
            RLog.e(TAG, "JSONException " + e.getMessage());
        }
    }

    // </editor-fold>

    // <editor-fold desc="* 4. Parcel 的序列化方法">

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToBaseInfoParcel(dest);
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(imgUrl);
        dest.writeString(sendUserId);
        dest.writeString(sendUserName);
    }

    public ContactMessage(Parcel in) {
        super.readFromBaseInfoParcel(in);
        id = in.readString();
        name = in.readString();
        imgUrl = in.readString();
        sendUserId = in.readString();
        sendUserName = in.readString();
    }

    public static final Creator<ContactMessage> CREATOR =
            new Creator<ContactMessage>() {
                @Override
                public ContactMessage createFromParcel(Parcel source) {
                    return new ContactMessage(source);
                }

                @Override
                public ContactMessage[] newArray(int size) {
                    return new ContactMessage[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    // </editor-fold>

    // <editor-fold desc="* 5. get & set 方法">
    private String getEmotion(String content) {
        Matcher matcher = pattern.matcher(content);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int inthex = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, String.valueOf(Character.toChars(inthex)));
        }

        matcher.appendTail(sb);

        return sb.toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getSendUserId() {
        return sendUserId;
    }

    public void setSendUserId(String sendUserId) {
        this.sendUserId = sendUserId;
    }

    public String getSendUserName() {
        return sendUserName;
    }

    public void setSendUserName(String sendUserName) {
        this.sendUserName = sendUserName;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
    // </editor-fold>

}
