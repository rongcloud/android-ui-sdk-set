package io.rong.imkit.feature.mention;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luoyanlong on 2018/11/23.
 * 保存草稿时如果有@信息，把MentionBlock序列化保存起来，
 * 之后可通过反序列化添加到RongMentionManager中
 */
public class DraftHelper {

    private static final String CONTENT = "content";
    private static final String MENTION = "mention";

    private String content;
    private List<MentionBlock> mentionBlocks;

    public DraftHelper(String s) {
        if (!TextUtils.isEmpty(s)) {
            try {
                JSONObject jsonObject = new JSONObject(s);
                content = jsonObject.getString(CONTENT);
                String mentionInfo = jsonObject.optString(MENTION);
                mentionBlocks = getMentionBlocks(mentionInfo);
            } catch (JSONException e) {
                // 没有@信息是用普通文本保存的
                content = s;
            }
        }
    }

    /**
     * 没有@信息用普通文本保存
     * 有@信息用json格式保存
     */
    public static String encode(String content, String mentionBlocks) {
        if (TextUtils.isEmpty(mentionBlocks)) {
            // 没有@信息用普通文本保存
            return content;
        } else {
            // 有@信息用json格式保存
            JSONObject jsonObject = new JSONObject();
            try {
                return jsonObject
                        .putOpt(CONTENT, content)
                        .putOpt(MENTION, mentionBlocks)
                        .toString();
            } catch (JSONException e) {
                return content;
            }
        }
    }

    public String decode() {
        return content;
    }

    public void restoreMentionInfo() {
        if (mentionBlocks != null) {
            for (MentionBlock mentionBlock : mentionBlocks) {
//                RongMentionManager.getInstance().addMentionBlock(mentionBlock);
            }
        }
    }

    public static String getDraftContent(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.getString(CONTENT);
        } catch (Exception e) {
            return json;
        }
    }

    @Nullable
    private List<MentionBlock> getMentionBlocks(String mentionInfo) {
        try {
            JSONArray jsonArray = new JSONArray(mentionInfo);
            List<MentionBlock> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                String s = jsonArray.getString(i);
                MentionBlock mentionBlock = new MentionBlock(s);
                list.add(mentionBlock);
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }

}
