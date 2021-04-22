package io.rong.imkit.conversation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.rong.imkit.IMCenter;
import io.rong.imkit.model.TypingInfo;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.typingmessage.TypingStatus;
import io.rong.message.TextMessage;
import io.rong.message.VoiceMessage;

public class ConversationViewModel extends AndroidViewModel {

    private MediatorLiveData<TypingInfo> typingStatusInfo = new MediatorLiveData<>();

    public ConversationViewModel(Application application) {
        super(application);
        IMCenter.getInstance().addTypingStatusListener(typingStatusListener);
    }

    public ConversationViewModel(String targetId, Conversation.ConversationType conversationType, String title, @NonNull Application application) {
        super(application);
        IMCenter.getInstance().addTypingStatusListener(typingStatusListener);
    }

    /**
     * 获取正在输入的状态信息
     *
     * @return 输入状态
     */
    public MediatorLiveData<TypingInfo> getTypingStatusInfo() {
        return typingStatusInfo;
    }

    private RongIMClient.TypingStatusListener typingStatusListener = new RongIMClient.TypingStatusListener() {
        @Override
        public void onTypingStatusChanged(Conversation.ConversationType type, String targetId, Collection<TypingStatus> typingStatusSet) {
            TypingInfo info = new TypingInfo();
            info.conversationType = type;
            info.targetId = targetId;
            int count = typingStatusSet.size();
            if (count > 0) {
                List<TypingInfo.TypingUserInfo> typingUserInfoList = new ArrayList<>();
                Iterator iterator = typingStatusSet.iterator();
                while (iterator.hasNext()) {
                    TypingInfo.TypingUserInfo typing = new TypingInfo.TypingUserInfo();
                    TypingStatus status = (TypingStatus) iterator.next();
                    String objectName = status.getTypingContentType();
                    MessageTag textTag = TextMessage.class.getAnnotation(MessageTag.class);
                    MessageTag voiceTag = VoiceMessage.class.getAnnotation(MessageTag.class);

                    //匹配对方正在输入的是文本消息还是语音消息
                    if (objectName.equals(textTag.value())) {
                        typing.type = TypingInfo.TypingUserInfo.Type.text;
                    } else if (objectName.equals(voiceTag.value())) {
                        typing.type = TypingInfo.TypingUserInfo.Type.voice;
                    }

                    typing.sendTime = status.getSentTime();
                    typing.userId = status.getUserId();
                    typingUserInfoList.add(typing);
                }
                info.typingList = typingUserInfoList;
            }
            typingStatusInfo.postValue(info);
        }
    };


    @Override
    protected void onCleared() {
        super.onCleared();
        IMCenter.getInstance().removeTypingStatusListener(typingStatusListener);
    }

    /**
     * 根据会话类型，清空某一会话的所有聊天消息记录,回调方式获取清空是否成功。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         清空是否成功的回调。
     */
    public void clearMessages(final Conversation.ConversationType conversationType, final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
//        RongIMClient.getInstance().clearMessages(conversationType, targetId, new RongIMClient.ResultCallback<Boolean>() {
//            @Override
//            public void onSuccess(Boolean bool) {
//                if (bool)
//                    RongContext.getInstance().getEventBus().post(new Event.MessagesClearEvent(conversationType, targetId));
//
//                if (callback != null)on
//                    callback.onSuccess(bool);
//            }
//
//            @Override
//            public void onError(RongIMClient.ErrorCode e) {
//                if (callback != null)
//                    callback.onError(e);
//            }
//        });
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private String targetId;
        private String title;
        private Conversation.ConversationType conversationType;
        private Application application;

        public Factory(String targetId, Conversation.ConversationType conversationType, String title, Application application) {
            this.conversationType = conversationType;
            this.targetId = targetId;
            this.title = title;
            this.application = application;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            try {
                return modelClass.getConstructor(String.class, Conversation.ConversationType.class, String.class, Application.class).newInstance(targetId, conversationType, title, application);
            } catch (Exception e) {
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
    }


}
