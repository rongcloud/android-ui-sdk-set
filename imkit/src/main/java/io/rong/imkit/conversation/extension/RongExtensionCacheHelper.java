package io.rong.imkit.conversation.extension;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.feature.editmessage.EditMessageConfig;
import io.rong.imkit.feature.mention.MentionBlock;
import io.rong.imkit.utils.StringUtils;
import io.rong.imlib.model.Conversation;
import io.rong.message.ReferenceMessage.ReferenceMessageStatus;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RongExtensionCacheHelper {
    private static final String EXTENSION_PREFERENCE = "RongExtension";
    private static final String VOICE_INPUT_MODE = "voiceInputMode";
    private static final String DESTRUCT_MODE = "destructInputMode";
    private static final String DESTRUCT_FIRST_USING = "destructFirstUsing";
    private static final String EDIT_MESSAGE = "editMessage_";

    public static void saveVoiceInputMode(
            Context context,
            Conversation.ConversationType type,
            String targetId,
            boolean isVoiceMode) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        String key = StringUtils.getKey(type.getName(), targetId);
        if (isVoiceMode) {
            sharedPreferences.edit().putBoolean(VOICE_INPUT_MODE + key, true).apply();
        } else {
            sharedPreferences.edit().remove(VOICE_INPUT_MODE + key).apply();
        }
    }

    public static boolean isVoiceInputMode(
            Context context, Conversation.ConversationType type, String targetId) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        String key = StringUtils.getKey(type.getName(), targetId);
        return sharedPreferences.getBoolean(VOICE_INPUT_MODE + key, false);
    }

    public static void saveDestructInputMode(Context context, boolean isKeyMode) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(DESTRUCT_MODE, isKeyMode).apply();
    }

    public static boolean getCachedDestructInputMode(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(DESTRUCT_MODE, false);
    }

    public static void recordDestructClickEvent(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(DESTRUCT_FIRST_USING, false).apply();
    }

    public static boolean isDestructFirstUsing(Context context) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(DESTRUCT_FIRST_USING, true);
    }

    public static void setDestructMode(
            Context context,
            Conversation.ConversationType conversationType,
            String targetId,
            boolean value) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        if (value) {
            sharedPreferences
                    .edit()
                    .putBoolean(StringUtils.getKey(conversationType.getName(), targetId), true)
                    .apply();
        } else {
            sharedPreferences
                    .edit()
                    .remove(StringUtils.getKey(conversationType.getName(), targetId))
                    .apply();
        }
    }

    public static boolean isDestructMode(
            Context context, Conversation.ConversationType conversationType, String targetId) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(
                StringUtils.getKey(conversationType.getName(), targetId), false);
    }

    public static void setEditMessageConfig(
            Context context,
            Conversation.ConversationType conversationType,
            String targetId,
            EditMessageConfig config) {
        if (context == null
                || conversationType == null
                || TextUtils.isEmpty(targetId)
                || config == null
                || TextUtils.isEmpty(config.uid)) {
            return;
        }
        SharedPreferences sp =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        if (sp == null) {
            return;
        }
        String key = EDIT_MESSAGE + StringUtils.getKey(conversationType.getName(), targetId);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uid", config.uid);
            jsonObject.put("content", config.content);
            String content = !TextUtils.isEmpty(config.referContent) ? config.referContent : "";
            jsonObject.put("referContent", content);
            jsonObject.put("referUid", config.referUid);
            jsonObject.put("sentTime", config.sentTime);
            if (config.mentionBlocks != null && !config.mentionBlocks.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                for (MentionBlock mentionBlock : config.mentionBlocks) {
                    jsonArray.put(mentionBlock.toJson());
                }
                jsonObject.put("mentionBlocks", jsonArray);
            }
        } catch (JSONException e) {
            RLog.e("RongExtensionCacheHelper", "setEditMessageConfig: " + e);
        }
        sp.edit().putString(key, jsonObject.toString()).apply();
    }

    public static void clearEditMessageConfig(
            Context context, Conversation.ConversationType conversationType, String targetId) {
        if (context == null || conversationType == null || TextUtils.isEmpty(targetId)) {
            return;
        }
        SharedPreferences sp =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        if (sp == null) {
            return;
        }
        String key = EDIT_MESSAGE + StringUtils.getKey(conversationType.getName(), targetId);
        sp.edit().remove(key).apply();
    }

    public static EditMessageConfig getEditMessageConfig(
            Context context, Conversation.ConversationType conversationType, String targetId) {
        if (context == null || conversationType == null || TextUtils.isEmpty(targetId)) {
            return null;
        }
        SharedPreferences sp =
                context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        if (sp == null) {
            return null;
        }
        String key = EDIT_MESSAGE + StringUtils.getKey(conversationType.getName(), targetId);
        String configJson = sp.getString(key, "");
        if (TextUtils.isEmpty(configJson)) {
            return null;
        }
        EditMessageConfig config = new EditMessageConfig();
        // 尝试解析为 JSON 格式
        try {
            JSONObject editMessageConfig = new JSONObject(configJson);
            config.uid = editMessageConfig.optString("uid", "");
            config.content = editMessageConfig.optString("content", "");
            if (TextUtils.isEmpty(config.uid) || TextUtils.isEmpty(config.content)) {
                // 数据异常，移除sp
                sp.edit().remove(key).apply();
                return null;
            }
            config.sentTime = editMessageConfig.optLong("sentTime", 0);
            config.referContent = editMessageConfig.optString("referContent", "");
            config.referUid = editMessageConfig.optString("referUid", "");
            config.referStatus = ReferenceMessageStatus.DEFAULT;
            JSONArray mentionBlocks = editMessageConfig.optJSONArray("mentionBlocks");
            if (mentionBlocks != null) {
                List<MentionBlock> mentionBlocksList = new ArrayList<>();
                for (int i = 0; i < mentionBlocks.length(); i++) {
                    String json = mentionBlocks.optString(i);
                    MentionBlock block = new MentionBlock(json);
                    mentionBlocksList.add(block);
                }
                config.mentionBlocks = mentionBlocksList;
            }
            return config;
        } catch (Exception e) {
            RLog.e("RongExtensionCacheHelper", "getEditMessageConfig: " + e);
        }
        return null;
    }
}
