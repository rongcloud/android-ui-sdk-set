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
import io.rong.imkit.notification.RongNotificationManager;
import io.rong.imlib.ChannelClient;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.RongIMClient.ErrorCode;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Conversation.ConversationNotificationStatus;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.ConversationStatus;

public class ConversationSettingViewModel extends AndroidViewModel {
    private ConversationIdentifier conversationIdentifier;
    MutableLiveData<Boolean> mTopStatus;
    MutableLiveData<OperationResult> mOperationResult;
    MutableLiveData<Conversation.ConversationNotificationStatus> mNotificationStatus;

    public ConversationSettingViewModel(@NonNull Application application) {
        super(application);
    }

    public ConversationSettingViewModel(
            Application application, ConversationIdentifier conversationIdentifier) {
        super(application);
        this.conversationIdentifier = conversationIdentifier;
        mOperationResult = new MutableLiveData<>();
        mTopStatus = new MutableLiveData<>();
        mNotificationStatus = new MutableLiveData<>();
        ChannelClient.getInstance()
                .getConversationTopStatus(
                        conversationIdentifier.getTargetId(),
                        conversationIdentifier.getType(),
                        conversationIdentifier.getChannelId(),
                        new IRongCoreCallback.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                mTopStatus.postValue(aBoolean);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                // do nothing
                            }
                        });
        IMCenter.getInstance().addConversationStatusListener(mConversationStatusListener);
        RongNotificationManager.getInstance()
                .getConversationNotificationStatus(
                        conversationIdentifier,
                        new RongIMClient.ResultCallback<ConversationNotificationStatus>() {
                            @Override
                            public void onSuccess(
                                    ConversationNotificationStatus conversationNotificationStatus) {
                                mNotificationStatus.postValue(conversationNotificationStatus);
                            }

                            @Override
                            public void onError(ErrorCode coreErrorCode) {
                                // do nothing
                            }
                        });
    }

    public void clearMessages(long recordTime, boolean clearRemote) {
        IMCenter.getInstance()
                .cleanHistoryMessages(
                        conversationIdentifier,
                        recordTime,
                        clearRemote,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                mOperationResult.postValue(
                                        new OperationResult(
                                                OperationResult.Action.CLEAR_CONVERSATION_MESSAGES,
                                                OperationResult.SUCCESS));
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                mOperationResult.postValue(
                                        new OperationResult(
                                                OperationResult.Action.CLEAR_CONVERSATION_MESSAGES,
                                                errorCode.getValue()));
                            }
                        });
        // 清除远端消息
        RongIMClient.getInstance()
                .cleanRemoteHistoryMessages(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        System.currentTimeMillis(),
                        null);
    }

    public void setConversationTop(final boolean isTop, boolean shouldCreateNewConversation) {
        IMCenter.getInstance()
                .setConversationToTop(
                        conversationIdentifier,
                        isTop,
                        shouldCreateNewConversation,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                mOperationResult.postValue(
                                        new OperationResult(
                                                OperationResult.Action.SET_TOP,
                                                OperationResult.SUCCESS));
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                mOperationResult.postValue(
                                        new OperationResult(
                                                OperationResult.Action.SET_TOP,
                                                errorCode.getValue()));
                            }
                        });
    }

    public void setNotificationStatus(final Conversation.ConversationNotificationStatus status) {
        IMCenter.getInstance()
                .setConversationNotificationStatus(
                        conversationIdentifier,
                        status,
                        new RongIMClient.ResultCallback<
                                Conversation.ConversationNotificationStatus>() {
                            @Override
                            public void onSuccess(
                                    Conversation.ConversationNotificationStatus
                                            conversationNotificationStatus) {
                                mOperationResult.postValue(
                                        new OperationResult(
                                                OperationResult.Action.SET_NOTIFICATION_STATUS,
                                                OperationResult.SUCCESS));
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                mOperationResult.postValue(
                                        new OperationResult(
                                                OperationResult.Action.SET_NOTIFICATION_STATUS,
                                                errorCode.getValue()));
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
        private ConversationIdentifier conversationIdentifier;
        private Application application;

        public Factory(Application application, ConversationIdentifier conversationIdentifier) {
            this.conversationIdentifier = conversationIdentifier;
            this.application = application;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            try {
                return modelClass
                        .getConstructor(Application.class, ConversationIdentifier.class)
                        .newInstance(application, conversationIdentifier);
            } catch (Exception e) {
                throw new RuntimeException("Cannot create an instance of " + modelClass, e);
            }
        }
    }

    private RongIMClient.ConversationStatusListener mConversationStatusListener =
            new RongIMClient.ConversationStatusListener() {
                @Override
                public void onStatusChanged(ConversationStatus[] conversationStatuses) {
                    if (conversationStatuses != null && conversationStatuses.length > 0) {
                        for (ConversationStatus status : conversationStatuses) {
                            if (status.getConversationType() == conversationIdentifier.getType()
                                    && status.getTargetId()
                                            .equals(conversationIdentifier.getTargetId())) {
                                if (status.getStatus() != null
                                        && !TextUtils.isEmpty(
                                                status.getStatus()
                                                        .get(ConversationStatus.TOP_KEY))) {
                                    mTopStatus.postValue(status.isTop());
                                }
                                if (status.getStatus() != null
                                        && !TextUtils.isEmpty(
                                                status.getStatus()
                                                        .get(
                                                                ConversationStatus
                                                                        .NOTIFICATION_KEY))) {
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
