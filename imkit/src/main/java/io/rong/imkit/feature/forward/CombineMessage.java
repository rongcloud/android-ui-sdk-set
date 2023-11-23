package io.rong.imkit.feature.forward;

import android.net.Uri;
import android.os.Parcel;
import android.text.TextUtils;
import io.rong.common.ParcelUtils;
import io.rong.common.RLog;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.Conversation;
import io.rong.message.MediaMessageContent;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@MessageTag(value = "RC:CombineMsg", flag = MessageTag.ISCOUNTED | MessageTag.ISPERSISTED)
public class CombineMessage extends MediaMessageContent {
    private static final String TAG = CombineMessage.class.getSimpleName();

    private String title;
    // 这两个参数用来拼装默认消息的标题
    // 区分合并消息是在群聊里还是单聊里
    private Conversation.ConversationType conversationType = Conversation.ConversationType.PRIVATE;
    // 单聊里最多有两个,群聊不记录
    private List<String> nameList = new ArrayList<>();
    // 默认消息的内容
    private List<String> summaryList = new ArrayList<>();

    protected CombineMessage() {
        // default implementation ignored
    }

    public static CombineMessage obtain(Uri url) {
        CombineMessage model = new CombineMessage();
        if (url.toString().startsWith("file")) {
            model.setLocalPath(url);
        } else {
            model.setMediaUrl(url);
        }
        return model;
    }

    @Override
    public byte[] encode() {
        JSONObject jsonObj = super.getBaseJsonObject();
        try {
            if (!TextUtils.isEmpty(getTitle())) {
                jsonObj.put("title", getTitle());
            }
            if (!TextUtils.isEmpty(getName())) {
                jsonObj.put("name", getName());
            }
            if (getLocalPath() != null) {
                jsonObj.put("localPath", getLocalPath().toString());
            }
            if (getMediaUrl() != null) {
                jsonObj.put("remoteUrl", getMediaUrl().toString());
            }
            jsonObj.put("conversationType", conversationType.getValue());

            JSONArray jsonArray = new JSONArray();
            for (String name : nameList) {
                jsonArray.put(name);
            }
            jsonObj.put("nameList", jsonArray);

            jsonArray = new JSONArray();
            for (String summary : summaryList) {
                jsonArray.put(summary);
            }
            jsonObj.put("summaryList", jsonArray);
        } catch (JSONException e) {
            RLog.e(TAG, "JSONException " + e.getMessage());
        }
        try {
            return jsonObj.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            RLog.e(TAG, "UnsupportedEncodingException", e);
        }
        return null;
    }

    public CombineMessage(byte[] data) {
        String jsonStr = null;
        try {
            jsonStr = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            RLog.e(TAG, "UnsupportedEncodingException", e);
        }

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            super.parseBaseJsonObject(jsonObj);
            if (jsonObj.has("title")) setTitle(jsonObj.optString("title"));
            if (jsonObj.has("name")) setName(jsonObj.optString("name"));
            if (jsonObj.has("localPath")) setLocalPath(Uri.parse(jsonObj.optString("localPath")));
            if (jsonObj.has("remoteUrl")) setMediaUrl(Uri.parse(jsonObj.optString("remoteUrl")));
            setConversationType(
                    Conversation.ConversationType.setValue(jsonObj.optInt("conversationType")));

            JSONArray jsonArray = jsonObj.optJSONArray("nameList");
            List<String> nameList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                nameList.add((String) jsonArray.get(i));
            }
            setNameList(nameList);

            jsonArray = jsonObj.optJSONArray("summaryList");
            List<String> summaryList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                summaryList.add((String) jsonArray.get(i));
            }
            setSummaryList(summaryList);

        } catch (JSONException e) {
            RLog.e(TAG, "JSONException " + e.getMessage());
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToBaseInfoParcel(dest);
        ParcelUtils.writeToParcel(dest, getName());
        ParcelUtils.writeToParcel(dest, getLocalPath());
        ParcelUtils.writeToParcel(dest, getMediaUrl());
        ParcelUtils.writeToParcel(dest, getConversationType().getValue());
        ParcelUtils.writeToParcel(dest, getNameList());
        ParcelUtils.writeToParcel(dest, getSummaryList());
    }

    /**
     * 构造函数。
     *
     * @param in 初始化传入的 Parcel。
     */
    private CombineMessage(Parcel in) {
        super.readFromBaseInfoParcel(in);
        setName(ParcelUtils.readFromParcel(in));
        setLocalPath(ParcelUtils.readFromParcel(in, Uri.class));
        setMediaUrl(ParcelUtils.readFromParcel(in, Uri.class));
        setConversationType(
                Conversation.ConversationType.setValue(ParcelUtils.readIntFromParcel(in)));
        setNameList(ParcelUtils.readListFromParcel(in, String.class));
        setSummaryList(ParcelUtils.readListFromParcel(in, String.class));
    }

    public static final Creator<CombineMessage> CREATOR =
            new Creator<CombineMessage>() {
                @Override
                public CombineMessage createFromParcel(Parcel source) {
                    return new CombineMessage(source);
                }

                @Override
                public CombineMessage[] newArray(int size) {
                    return new CombineMessage[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Conversation.ConversationType getConversationType() {
        return conversationType;
    }

    public void setConversationType(Conversation.ConversationType conversationType) {
        this.conversationType = conversationType;
    }

    public List<String> getNameList() {
        return nameList;
    }

    public void setNameList(List<String> nameList) {
        this.nameList = nameList;
    }

    public List<String> getSummaryList() {
        return summaryList;
    }

    public void setSummaryList(List<String> summaryList) {
        this.summaryList = summaryList;
    }
}
