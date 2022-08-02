package io.rong.imkit.model;

import java.util.List;

import io.rong.imlib.model.Conversation;

public class TypingInfo {
    public Conversation.ConversationType conversationType; // 会话类型
    public String targetId;// 会话 Id
    public List<TypingUserInfo> typingList; // 正在输入的用户信息

    /**
     * 正在输入的用户信息
     */
    public static class TypingUserInfo {
        public enum Type {
            voice,
            text
        }

        public Type type;
        public long sendTime;
        public String userId;
    }
}
