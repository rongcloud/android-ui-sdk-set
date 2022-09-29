package io.rong.imkit.model;

import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import io.rong.common.RLog;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageConfig;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.ReadReceiptInfo;
import io.rong.imlib.model.UserInfo;
import java.util.HashMap;
import java.util.Map;

public class UiMessage extends UiBaseBean {
    private final String TAG = UiMessage.class.getSimpleName();
    private Message message;
    private UserInfo userInfo;
    private @State.Value int state;
    private int progress;
    private String destructTime;
    private boolean isPlaying;
    private boolean isEdit;
    private boolean isSelected;
    private String nickname;
    /** TextMessage 和 ReferenceMessage 的 content 字段 */
    private SpannableStringBuilder contentSpannable;
    /** ReferenceMessage 的 referMsg 为 TextMessage 时 的 content 字段 */
    private SpannableStringBuilder referenceContentSpannable;
    /** 翻译之后的文本 */
    private String translatedContent;

    private @State.Value int translateStatus = State.NORMAL;

    public UiMessage(Message message) {
        setMessage(message);
        initUserInfo();
        change();
    }

    public void initUserInfo() {
        if (TextUtils.isEmpty(message.getSenderUserId())) {
            if (message.getMessageDirection().equals(Message.MessageDirection.SEND)) {
                message.setSenderUserId(RongIMClient.getInstance().getCurrentUserId());
            } else {
                RLog.e(TAG, "Invalid message with empty senderUserId!");
                return;
            }
        }
        UserInfo user = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
        if (user != null) {
            userInfo = user;
            if (userInfo.getName() == null) {
                userInfo.setName(message.getSenderUserId());
            }
        } else {
            userInfo = new UserInfo(message.getSenderUserId(), message.getSenderUserId(), null);
        }
        if (message.getConversationType().equals(Conversation.ConversationType.GROUP)) {
            GroupUserInfo groupUserInfo =
                    RongUserInfoManager.getInstance()
                            .getGroupUserInfo(message.getTargetId(), message.getSenderUserId());
            if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname())) {
                nickname = groupUserInfo.getNickname();
            }
        }
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
        if (message.getSentStatus() != null) {
            switch (message.getSentStatus()) {
                case SENDING:
                    state = State.PROGRESS;
                    break;
                case FAILED:
                    state = State.ERROR;
                    break;
                case CANCELED:
                    state = State.CANCEL;
                    break;
            }
        }
        change();
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
        change();
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
        change();
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        change();
    }

    public String getDestructTime() {
        return destructTime;
    }

    public void setDestructTime(String destructTime) {
        this.destructTime = destructTime;
        change();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        change();
    }

    public boolean isEdit() {
        return isEdit;
    }

    public void setEdit(boolean edit) {
        this.isEdit = edit;
        change();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        change();
    }

    public void onUserInfoUpdate(UserInfo user) {
        if (user.getUserId().equals(message.getSenderUserId())) {
            if (user.getName() != null) {
                userInfo.setName(user.getName());
            } else {
                userInfo.setName("");
            }
            userInfo.setAlias(user.getAlias());
            if (user.getPortraitUri() != null) {
                userInfo.setPortraitUri(Uri.parse(user.getPortraitUri().toString()));
            } else {
                userInfo.setPortraitUri(null);
            }
            userInfo.setExtra(user.getExtra());
            change();
        }
    }

    public void onGroupMemberInfoUpdate(GroupUserInfo member) {
        if (member.getUserId().equals(message.getSenderUserId())) {
            nickname = member.getNickname();
            change();
        }
    }

    public int getMessageId() {
        return message != null ? message.getMessageId() : -1;
    }

    public void setMessageId(int messageId) {
        if (message == null) {
            return;
        }
        message.setMessageId(messageId);
        change();
    }

    public String getUId() {
        return message != null ? message.getUId() : null;
    }

    public Conversation.ConversationType getConversationType() {
        return message != null ? message.getConversationType() : Conversation.ConversationType.NONE;
    }

    public void setConversationType(Conversation.ConversationType conversationType) {
        if (message == null) {
            return;
        }
        message.setConversationType(conversationType);
        change();
    }

    public String getTargetId() {
        return message != null ? message.getTargetId() : null;
    }

    public void setTargetId(String targetId) {
        if (message == null) {
            return;
        }
        message.setTargetId(targetId);
        change();
    }

    public long getReadTime() {
        return message != null ? message.getReadTime() : 0;
    }

    public void setReadTime(long readTime) {
        if (message == null) {
            return;
        }
        message.setReadTime(readTime);
        change();
    }

    public Message.MessageDirection getMessageDirection() {
        return message != null ? message.getMessageDirection() : null;
    }

    public void setMessageDirection(Message.MessageDirection messageDirection) {
        if (message == null) {
            return;
        }
        message.setMessageDirection(messageDirection);
        change();
    }

    public Message.ReceivedStatus getReceivedStatus() {
        return message != null ? message.getReceivedStatus() : null;
    }

    public void setReceivedStatus(Message.ReceivedStatus receivedStatus) {
        if (message == null) {
            return;
        }
        message.setReceivedStatus(receivedStatus);
        change();
    }

    public Message.SentStatus getSentStatus() {
        return message != null ? message.getSentStatus() : null;
    }

    public void setSentStatus(Message.SentStatus sentStatus) {
        if (message == null) {
            return;
        }
        message.setSentStatus(sentStatus);
        change();
    }

    public long getReceivedTime() {
        return message != null ? message.getReceivedTime() : 0;
    }

    public void setReceivedTime(long receivedTime) {
        if (message == null) {
            return;
        }
        message.setReceivedTime(receivedTime);
        change();
    }

    public long getSentTime() {
        return message != null ? message.getSentTime() : 0;
    }

    public void setSentTime(long sentTime) {
        if (message == null) {
            return;
        }
        message.setSentTime(sentTime);
        change();
    }

    public String getObjectName() {
        return message != null ? message.getObjectName() : null;
    }

    public void setObjectName(String objectName) {
        if (message == null) {
            return;
        }
        message.setObjectName(objectName);
        change();
    }

    public MessageContent getContent() {
        return message != null ? message.getContent() : null;
    }

    public void setContent(MessageContent content) {
        if (message == null) {
            return;
        }
        message.setContent(content);
        change();
    }

    public String getExtra() {
        return message != null ? message.getExtra() : null;
    }

    public void setExtra(String extra) {
        if (message == null) {
            return;
        }
        message.setExtra(extra);
        change();
    }

    public String getSenderUserId() {
        return message != null ? message.getSenderUserId() : null;
    }

    public void setSenderUserId(String senderUserId) {
        if (message == null) {
            return;
        }
        message.setSenderUserId(senderUserId);
        change();
    }

    public ReadReceiptInfo getReadReceiptInfo() {
        return message != null ? message.getReadReceiptInfo() : null;
    }

    public void setReadReceiptInfo(ReadReceiptInfo readReceiptInfo) {
        if (message == null) {
            return;
        }
        message.setReadReceiptInfo(readReceiptInfo);
        change();
    }

    public MessageConfig getMessageConfig() {
        return message != null ? message.getMessageConfig() : null;
    }

    public void setMessageConfig(MessageConfig messageConfig) {
        if (message == null) {
            return;
        }
        message.setMessageConfig(messageConfig);
        change();
    }

    public boolean isCanIncludeExpansion() {
        return message != null && message.isCanIncludeExpansion();
    }

    public void setCanIncludeExpansion(boolean canIncludeExpansion) {
        if (message == null) {
            return;
        }
        message.setCanIncludeExpansion(canIncludeExpansion);
        change();
    }

    public Map<String, String> getExpansion() {
        return message != null ? message.getExpansion() : null;
    }

    public void setExpansion(HashMap<String, String> expansion) {
        if (message == null) {
            return;
        }
        message.setExpansion(expansion);
        change();
    }

    public SpannableStringBuilder getContentSpannable() {
        return contentSpannable;
    }

    public void setContentSpannable(SpannableStringBuilder contentSpannable) {
        this.contentSpannable = contentSpannable;
    }

    public SpannableStringBuilder getReferenceContentSpannable() {
        return referenceContentSpannable;
    }

    public void setReferenceContentSpannable(SpannableStringBuilder referenceContentSpannable) {
        this.referenceContentSpannable = referenceContentSpannable;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDisplayName() {
        return RongUserInfoManager.getInstance().getUserDisplayName(userInfo, nickname);
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public void setTranslatedContent(String translatedContent) {
        this.translatedContent = translatedContent;
    }

    @State.Value
    public int getTranslateStatus() {
        return translateStatus;
    }

    public void setTranslateStatus(@State.Value int translateStatus) {
        this.translateStatus = translateStatus;
    }
}
