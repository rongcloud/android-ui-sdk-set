package io.rong.imkit.model;

import android.text.TextUtils;

import io.rong.common.RLog;
import io.rong.imlib.model.Conversation;

/**
 * Created by zhjchen on 4/29/15.
 */

public final class ConversationKey {

    private final static String SEPARATOR = "#@6RONG_CLOUD9@#";

    private String key;
    private String targetId;
    private Conversation.ConversationType type;

    private ConversationKey() {

    }


    public static ConversationKey obtain(String targetId, Conversation.ConversationType type) {

        if (!TextUtils.isEmpty(targetId) && type != null) {
            ConversationKey conversationKey = new ConversationKey();
            conversationKey.setTargetId(targetId);
            conversationKey.setType(type);
            conversationKey.setKey(targetId + SEPARATOR + type.getValue());
            return conversationKey;
        }

        return null;
    }

    public static ConversationKey obtain(String key) {

        if (!TextUtils.isEmpty(key) && key.contains(SEPARATOR)) {
            ConversationKey conversationKey = new ConversationKey();

            if (key.contains(SEPARATOR)) {
                String[] array = key.split(SEPARATOR);
                conversationKey.setTargetId(array[0]);

                try {
                    conversationKey.setType(Conversation.ConversationType.setValue(Integer.parseInt(array[1])));
                } catch (NumberFormatException e) {
                    RLog.e("ConversationKey ", "NumberFormatException");
                    return null;
                }

                return conversationKey;
            }
        }

        return null;
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Conversation.ConversationType getType() {
        return type;
    }

    public void setType(Conversation.ConversationType type) {
        this.type = type;
    }


}
