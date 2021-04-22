package io.rong.imkit.feature.destruct;

import android.content.Context;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.lifecycle.ViewModelProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.rong.imkit.IMCenter;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionCacheHelper;
import io.rong.imkit.conversation.extension.RongExtensionManager;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.feature.mention.IExtensionEventWatcher;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.destruct.DestructionTaskManager;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.TextMessage;

/**
 * Created by Android Studio.
 * User: lvhongzhen
 * Date: 2019-09-11
 * Time: 10:26
 */
public class DestructManager implements IExtensionEventWatcher {
    public static int VOICE_DESTRUCT_TIME = 10;
    public static int IMAGE_DESTRUCT_TIME = 30;
    public static int SIGHT_DESTRUCT_TIME = 10;
    private Map<String, Map<String, RongIMClient.DestructCountDownTimerListener>> mMap;
    private DestructInputPanel mDestructInputPanel;
    private Map<String, String> mUnFinishTimes;
    private RongExtensionViewModel mExtensionViewModel;


    private DestructManager() {
        mMap = new HashMap<>();
        mUnFinishTimes = new HashMap<>();
    }

    private static class DestructManagerHolder {
        private static DestructManager instance = new DestructManager();
    }

    public static DestructManager getInstance() {
        return DestructManagerHolder.instance;
    }

    /**
     * 激活阅后即焚模式
     *
     * @param context
     */
    void activeDestructMode(Context context) {
        if (DestructExtensionModule.sRongExtension != null && DestructExtensionModule.sFragment != null) {
            RongExtension extension = DestructExtensionModule.sRongExtension.get();
            RelativeLayout container = extension.getContainer(RongExtension.ContainerType.INPUT);
            container.removeAllViews();
            mDestructInputPanel = new DestructInputPanel(DestructExtensionModule.sFragment.get(), extension.getContainer(RongExtension.ContainerType.INPUT)
                    , extension.getConversationType(), extension.getTargetId());
            container.addView(mDestructInputPanel.getRootView());

            mExtensionViewModel = new ViewModelProvider(DestructExtensionModule.sFragment.get()).get(RongExtensionViewModel.class);
            mExtensionViewModel.setEditTextWidget(mDestructInputPanel.getEditText());
            mExtensionViewModel.collapseExtensionBoard();

            RongExtensionCacheHelper.setDestructMode(context, extension.getConversationType(), extension.getTargetId(), true);
            RongExtensionManager.getInstance().addExtensionEventWatcher(this);
        }
    }

    public void exitDestructMode() {
        RongExtension extension = DestructExtensionModule.sRongExtension.get();
        RongExtensionCacheHelper.setDestructMode(extension.getContext(), extension.getConversationType(), extension.getTargetId(), false);
        extension.resetToDefaultView();
    }

    public static boolean isActive() {
        if (DestructExtensionModule.sRongExtension != null && DestructExtensionModule.sFragment != null) {
            RongExtension extension = DestructExtensionModule.sRongExtension.get();
            if (extension != null && extension.getContext() != null) {
                return RongExtensionCacheHelper.isDestructMode(extension.getContext(), extension.getConversationType(), extension.getTargetId());
            }
        }
        return false;
    }

    @Override
    public void onTextChanged(Context context, Conversation.ConversationType type, String targetId, int cursorPos, int count, String text) {

    }

    @Override
    public void onSendToggleClick(Message message) {
        MessageContent messageContent = message.getContent();
        if (messageContent instanceof TextMessage && isActive()) {
            int length = ((TextMessage) messageContent).getContent().length();
            long time;
            if (length <= 20) {
                time = 10;
            } else {
                time = Math.round((length - 20) * 0.5 + 10);
            }
            messageContent.setDestructTime(time);
            messageContent.setDestruct(true);
            message.setContent(messageContent);
        }
    }

    @Override
    public void onDeleteClick(Conversation.ConversationType type, String targetId, EditText editText, int cursorPos) {

    }

    @Override
    public void onDestroy(Conversation.ConversationType type, String targetId) {
        if (mDestructInputPanel != null) {
            mDestructInputPanel.onDestroy();
            mDestructInputPanel = null;
        }
        if (mExtensionViewModel != null) {
            mExtensionViewModel = null;
        }
        RongExtensionManager.getInstance().removeExtensionEventWatcher(this);
    }

    public void addListener(String pUId, RongIMClient.DestructCountDownTimerListener pDestructListener, String pTag) {
        if (mMap.containsKey(pUId)) {
            Map<String, RongIMClient.DestructCountDownTimerListener> map = mMap.get(pUId);
            if (map != null) {
                map.put(pTag, pDestructListener);
            }
        } else {
            HashMap<String, RongIMClient.DestructCountDownTimerListener> map = new HashMap<>();
            map.put(pTag, pDestructListener);
            mMap.put(pUId, map);
        }
    }

    public void deleteMessage(Message pMessage) {
        DestructionTaskManager.getInstance().deleteMessage(pMessage);
    }

    public void deleteMessages(Conversation.ConversationType pConversationType, String pTargetId, Message[] pDeleteMessages) {
        DestructionTaskManager.getInstance().deleteMessages(pConversationType, pTargetId, pDeleteMessages);
    }

    public String getUnFinishTime(String pMessageId) {
        return mUnFinishTimes.get(pMessageId);
    }

    public void startDestruct(final Message pMessage) {
        RongIMClient.getInstance().beginDestructMessage(pMessage, new RongIMClient.DestructCountDownTimerListener() {
            @Override
            public void onTick(final long untilFinished, final String messageId) {
                if (mMap.containsKey(messageId)) {
                    Map<String, RongIMClient.DestructCountDownTimerListener> map = mMap.get(messageId);
                    if (map != null) {
                        for (String key : map.keySet()) {
                            RongIMClient.DestructCountDownTimerListener destructCountDownTimerListener = map.get(key);
                            if (destructCountDownTimerListener != null) {
                                destructCountDownTimerListener.onTick(untilFinished, messageId);
                            }
                        }
                    }
                    if (untilFinished == 0) {
                        if (map != null) {
                            map.clear();
                        }
                        mMap.remove(messageId);
                        mUnFinishTimes.remove(messageId);
                        IMCenter.getInstance().deleteMessages(pMessage.getConversationType(), pMessage.getTargetId(), new int[]{pMessage.getMessageId()}, null);
//                        EventBus.getDefault().post(new Event.MessageDeleteEvent(pMessage.getMessageId()));
                    } else {
                        mUnFinishTimes.put(messageId, String.valueOf(untilFinished));
                    }
                }

            }

            @Override
            public void onStop(final String messageId) {
                if (mMap.containsKey(messageId)) {
                    Map<String, RongIMClient.DestructCountDownTimerListener> map = mMap.get(messageId);
                    if (map != null) {
                        for (String key : map.keySet()) {
                            RongIMClient.DestructCountDownTimerListener destructCountDownTimerListener = map.get(key);
                            if (destructCountDownTimerListener != null) {
                                destructCountDownTimerListener.onStop(messageId);
                            }
                        }
                    }
                    mUnFinishTimes.remove(messageId);
                }
            }
        });
    }

    public void stopDestruct(Message pMessage) {
        RongIMClient.getInstance().stopDestructMessage(pMessage);
    }
}
