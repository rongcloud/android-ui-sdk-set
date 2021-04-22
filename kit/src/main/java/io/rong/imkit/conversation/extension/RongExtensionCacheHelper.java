package io.rong.imkit.conversation.extension;

import android.content.Context;
import android.content.SharedPreferences;

import io.rong.imkit.utils.StringUtils;
import io.rong.imlib.model.Conversation;


public class RongExtensionCacheHelper {
    private static final String EXTENSION_PREFERENCE = "RongExtension";
    private static final String VOICE_INPUT_MODE = "voiceInputMode";
    private static final String DESTRUCT_MODE = "destructInputMode";
    private static final String DESTRUCT_FIRST_USING = "destructFirstUsing";

    public static void saveVoiceInputMode(Context context, Conversation.ConversationType type,
                                          String targetId, boolean isVoiceMode) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        String key = StringUtils.getKey(type.getName(), targetId);
        if (isVoiceMode) {
            sharedPreferences.edit().putBoolean(VOICE_INPUT_MODE + key, true).apply();
        } else {
            sharedPreferences.edit().remove(VOICE_INPUT_MODE + key).apply();
        }
    }

    public static boolean isVoiceInputMode(Context context, Conversation.ConversationType type, String targetId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        String key = StringUtils.getKey(type.getName(), targetId);
        return sharedPreferences.getBoolean(VOICE_INPUT_MODE + key, false);
    }

    public static void saveDestructInputMode(Context context, boolean isKeyMode) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(DESTRUCT_MODE, isKeyMode).apply();
    }

    public static boolean getCachedDestructInputMode(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(DESTRUCT_MODE, false);
    }

    public static void recordDestructClickEvent(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(DESTRUCT_FIRST_USING, false).apply();
    }

    public static boolean isDestructFirstUsing(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(DESTRUCT_FIRST_USING, true);
    }

    public static void setDestructMode(Context context, Conversation.ConversationType conversationType, String targetId, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        if (value) {
            sharedPreferences.edit().putBoolean(StringUtils.getKey(conversationType.getName(), targetId), true).apply();
        } else {
            sharedPreferences.edit().remove(StringUtils.getKey(conversationType.getName(), targetId)).apply();
        }
    }

    public static boolean isDestructMode(Context context, Conversation.ConversationType conversationType, String targetId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(EXTENSION_PREFERENCE, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(StringUtils.getKey(conversationType.getName(), targetId), false);
    }
}
