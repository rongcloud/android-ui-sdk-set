package io.rong.imkit.conversation;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import io.rong.imkit.IMCenter;
import io.rong.imkit.model.OperationResult;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationStatus;

public class ConversationSettingViewModel extends AndroidViewModel {
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    MutableLiveData<Boolean> mTopStatus;
    MutableLiveData<OperationResult> mOperationResult;
    MutableLiveData<Conversation.ConversationNotificationStatus> mNotificationStatus;

    public ConversationSettingViewModel(@NonNull Application application) {
        super(application);
    }

    public ConversationSettingViewModel(Application application, Conversation.ConversationType conversationType, String targetId) {
        super(application);
        mConversationType = conversationType;
        mTargetId = targetId;
        mOperationResult = new MutableLiveData<>();
        mTopStatus = new MutableLiveData<>();
        mNotificationStatus = new MutableLiveData<>();
        RongIMClient.getInstance().getConversation(conversationType, targetId, new RongIMClient.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation conversation) {
                if (conversation!=null){
                    mTopStatus.postValue(conversation.isTop());
                    mNotificationStatus.postValue(conversation.getNotificationStatus());
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

            }
        });
        IMCenter.getInstance().addConversationStatusListener(mConversationStatusListener);
    }

    public void clearMessages(long recordTime, boolean clearRemote) {
        IMCenter.getInstance().cleanHistoryMessages(mConversationType, mTargetId, recordTime, clearRemote, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                mOperationResult.postValue(new OperationResult(OperationResult.Action.CLEAR_CONVERSATION_MESSAGES, OperationResult.SUCCESS));
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                mOperationResult.postValue(new OperationResult(OperationResult.Action.CLEAR_CONVERSATION_MESSAGES, errorCode.getValue()));
            }
        });
        // 清除远端消息
        RongIMClient.getInstance().cleanRemoteHistoryMessages(
                mConversationType,
                mTargetId, System.currentTimeMillis(),
                null);
    }

    public void setConversationTop(final boolean isTop, boolean shouldCreateNewConversation) {
        IMCenter.getInstance().setConversationToTop(mConversationType, mTargetId, isTop, shouldCreateNewConversation, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                mOperationResult.postValue(new OperationResult(OperationResult.Action.SET_TOP, OperationResult.SUCCESS));
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                mOperationResult.postValue(new OperationResult(OperationResult.Action.SET_TOP, errorCode.getValue()));
            }
        });
    }

    public void setNotificationStatus(final Conversation.ConversationNotificationStatus status) {
        IMCenter.getInstance().setConversationNotificationStatus(mConversationType, mTargetId, status, new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {
            @Override
            public void onSuccess(Conversation.ConversationNotificationStatus conversationNotificationStatus) {
                mOperationResult.postValue(new OperationResult(OperationResult.Action.SET_NOTIFICATION_STATUS, OperationResult.SUCCESS));
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                mOperationResult.postValue(new OperationResult(OperationResult.Action.SET_NOTIFICATION_STATUS, errorCode.getValue()));
            }
        });
    }

    public MutableLiveData<Boolean> getTopStatus() {
        return mTopStatus;
    }

    public MutableLiveData<Conversation.ConversationNotificationStatus> getNotificationStatus() {
        return mNotificationStatus;
    }

    public MutableLiveData<OperationResult> getOperationResult() {
        return mOperationResult;
    }

    public static class Factory implements ViewModelProvider.Factory {
        private String targetId;
        private Conversation.ConversationType conversationType;
        private Application application;

        public Factory(Application application, Conversation.ConversationType conversationType, String targetId) {
            this.conversationType = conversationType;
            this.targetId = targetId;
            this.application = application;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            try {
                return modelClass.getConstructor(Application.class, Conversation.ConversationType.class, String.class).newInstance(application, conversationType, targetId);
            } catch (Exception e) {
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
    }

    private RongIMClient.ConversationStatusListener mConversationStatusListener = new RongIMClient.ConversationStatusListener() {
        @Override
        public void onStatusChanged(ConversationStatus[] conversationStatuses) {
            if (conversationStatuses != null && conversationStatuses.length > 0) {
                for (ConversationStatus status : conversationStatuses) {
                    if (status.getConversationType() == mConversationType && status.getTargetId().equals(mTargetId)) {
                        if (status.getStatus() != null && !TextUtils.isEmpty(status.getStatus().get(ConversationStatus.TOP_KEY))) {
                            mTopStatus.postValue(status.isTop());
                        }
                        if (status.getStatus() != null && !TextUtils.isEmpty(status.getStatus().get(ConversationStatus.NOTIFICATION_KEY))) {
                            mNotificationStatus.postValue(status.getNotifyStatus());
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCleared() {
        super.onCleared();
        IMCenter.getInstance().removeConversationStatusListener(mConversationStatusListener);
    }
}
