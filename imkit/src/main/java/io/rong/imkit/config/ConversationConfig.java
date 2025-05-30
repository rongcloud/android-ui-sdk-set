package io.rong.imkit.config;

import android.content.Context;
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.LayoutDirection;
import androidx.core.text.TextUtilsCompat;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.component.moreaction.DeleteClickActions;
import io.rong.imkit.conversation.extension.component.moreaction.IClickActions;
import io.rong.imkit.conversation.messgelist.processor.IConversationUIRenderer;
import io.rong.imkit.conversation.messgelist.provider.CombineMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.DefaultMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.FileMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.GIFMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.GroupNotificationMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.HQVoiceMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.HistoryDivMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.IConversationSummaryProvider;
import io.rong.imkit.conversation.messgelist.provider.IMessageProvider;
import io.rong.imkit.conversation.messgelist.provider.ImageMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.InformationNotificationMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.RecallNotificationMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.RichContentMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.SightMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.TextMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.UnknownMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.VoiceMessageItemProvider;
import io.rong.imkit.conversation.messgelist.viewmodel.IMessageViewModelProcessor;
import io.rong.imkit.feature.customservice.CSConversationUIRenderer;
import io.rong.imkit.feature.customservice.provider.CSPullLeaveMsgItemProvider;
import io.rong.imkit.feature.destruct.provider.DestructGifMessageItemProvider;
import io.rong.imkit.feature.destruct.provider.DestructHQVoiceMessageItemProvider;
import io.rong.imkit.feature.destruct.provider.DestructImageMessageItemProvider;
import io.rong.imkit.feature.destruct.provider.DestructSightMessageItemProvider;
import io.rong.imkit.feature.destruct.provider.DestructTextMessageItemProvider;
import io.rong.imkit.feature.destruct.provider.DestructVoiceMessageItemProvider;
import io.rong.imkit.feature.forward.ForwardClickActions;
import io.rong.imkit.feature.publicservice.provider.PublicServiceMultiRichContentMessageProvider;
import io.rong.imkit.feature.publicservice.provider.PublicServiceRichContentMessageProvider;
import io.rong.imkit.feature.reference.ReferenceMessageItemProvider;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.CollectionsUtils;
import io.rong.imkit.widget.adapter.ProviderManager;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MessageContent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** 会话页面总配置项 */
public class ConversationConfig {

    private static final int conversationHistoryMessageMaxCount = 100;
    // 离线补偿已读回执，sp 文件名称
    public static String SP_NAME_READ_RECEIPT_CONFIG = "readReceiptConfig";

    /** 多端消息未读数同步，仅支持单群聊 */
    public static boolean enableMultiDeviceSync = true;

    private static int conversationRemoteMessageMaxCount = 100;
    private static int conversationShowUnreadMessageMaxCount = 100;
    private final String TAG = "ConversationConfig";
    // 消息撤回开关
    public boolean rc_enable_recall_message = true;
    // 消息重发开关
    public boolean rc_enable_resend_message = true;
    public int rc_message_recall_interval = 120;
    public int rc_message_recall_edit_interval = 300;
    public int rc_chatroom_first_pull_message_count = 10;
    public boolean rc_is_show_warning_notification = true;
    // 设置未听的语音消息，是否连续播放
    public boolean rc_play_audio_continuous = true;
    // 是否打开 @ 功能
    public boolean rc_enable_mentioned_message = true;
    // 设置已读回执有效时间（单位：秒）
    public int rc_read_receipt_request_interval = 120;
    // 选择媒体资源时，是否包含视频文件
    public boolean rc_media_selector_contain_video = false;
    // 在线时是否自动下载高质量语音消息
    public boolean rc_enable_automatic_download_voice_msg = true;
    // gif 自动下载的最大值， 超过就需点击手动下载（单位 KB）
    public int rc_gifmsg_auto_download_size = 1024;
    // 多选时,最大多选消息数
    public int rc_max_message_selected_count = 100;
    // 是否开启合并转发功能,默认关闭
    public boolean rc_enable_send_combine_message = false;
    private long rc_custom_service_evaluation_interval = 60 * 1000L;
    private boolean mStopCSWhenQuit = true;

    /** 已读回执，仅支持，单，群聊 */
    private boolean mEnableReadReceipt = true;

    private Set<Conversation.ConversationType> mSupportReadReceiptConversationTypes =
            new HashSet<>(4);

    /** 单聊是否显示头像 */
    private boolean showReceiverUserTitle = false;

    /** 新消息是否显示未读，目前支持 单，群聊 */
    private boolean showNewMessageBar = true;

    /** 历史消息是否显示，目前仅支持，单，群聊 */
    private boolean showHistoryMessageBar = true;

    /** 长按是否显示更多 */
    private boolean showMoreClickAction = true;

    /** 是否显示，历史消息模板 */
    private boolean showHistoryDividerMessage = true;

    /**
     * true:长按删除消息，会把本地消息、远端消息都删除
     *
     * <p>false:长按只删除本地消息，
     *
     * <p>5.6.3 版本将默认值改为 true
     */
    private boolean needDeleteRemoteMessage = true;

    /** 默认为 true 当会话页面删除消息后列表消息为空时，是否重新刷新列表 */
    private boolean needRefreshWhenListIsEmptyAfterDelete = true;

    private ConversationClickListener mConversationClickListener;
    private ProviderManager<UiMessage> mMessageListProvider = new ProviderManager<>();
    private List<IConversationUIRenderer> mConversationViewProcessors = new ArrayList<>();
    private CopyOnWriteArrayList<IConversationSummaryProvider> mConversationSummaryProviders =
            new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<IClickActions> mMoreClickActions = new CopyOnWriteArrayList<>();
    private IMessageProvider defaultMessageProvider = new DefaultMessageItemProvider();
    private IMessageViewModelProcessor mViewModelProcessor;
    // 是否显示未读 @消息
    private boolean showNewMentionMessageBar = true;
    // 进入会话界面，默认拉取历史消息数量
    private int conversationHistoryMessageCount = 10;
    // 进入会话界面，默认拉取远端历史消息数量
    private int conversationRemoteMessageCount = 10;
    // 进入会话界面，默认显示未读消息数量
    private int conversationShowUnreadMessageCount = 10;
    private IRongCoreEnum.ConversationLoadMessageType conversationLoadMessageType =
            IRongCoreEnum.ConversationLoadMessageType.ALWAYS;

    ConversationConfig() {
        initMessageProvider();
        initViewProcessor();
        initMoreClickAction();
        mSupportReadReceiptConversationTypes.add(Conversation.ConversationType.PRIVATE);
        mSupportReadReceiptConversationTypes.add(Conversation.ConversationType.ENCRYPTED);
        mSupportReadReceiptConversationTypes.add(Conversation.ConversationType.GROUP);
        mSupportReadReceiptConversationTypes.add(Conversation.ConversationType.DISCUSSION);
    }

    public void initConfig(Context context) {
        if (context != null) {
            Resources resources = context.getResources();
            try {
                rc_enable_recall_message = resources.getBoolean(R.bool.rc_enable_message_recall);
            } catch (Exception e) {
                RLog.e(TAG, "rc_enable_recall_message not get value", e);
            }
            try {
                rc_message_recall_interval =
                        resources.getInteger(R.integer.rc_message_recall_interval);
            } catch (Exception e) {
                RLog.e(TAG, "rc_message_recall_interval not get value", e);
            }
            try {
                rc_message_recall_edit_interval =
                        resources.getInteger(R.integer.rc_message_recall_edit_interval);
            } catch (Exception e) {
                RLog.e(TAG, "rc_message_recall_edit_interval not get value", e);
            }
            try {
                rc_chatroom_first_pull_message_count =
                        resources.getInteger(R.integer.rc_chatroom_first_pull_message_count);
            } catch (Exception e) {
                RLog.e(TAG, "rc_chatroom_first_pull_message_count not get value", e);
            }
            try {
                mEnableReadReceipt = resources.getBoolean(R.bool.rc_read_receipt);
            } catch (Exception e) {
                RLog.e(TAG, "rc_read_receipt not get value", e);
            }
            try {
                enableMultiDeviceSync = resources.getBoolean(R.bool.rc_enable_sync_read_status);
            } catch (Exception e) {
                RLog.e(TAG, "rc_enable_sync_read_status not get value", e);
            }
            try {
                rc_play_audio_continuous = resources.getBoolean(R.bool.rc_play_audio_continuous);
            } catch (Exception e) {
                RLog.e(TAG, "rc_play_audio_continuous not get value", e);
            }
            try {
                rc_enable_mentioned_message =
                        resources.getBoolean(R.bool.rc_enable_mentioned_message);
            } catch (Exception e) {
                RLog.e(TAG, "rc_enable_mentioned_message not get value", e);
            }
            try {
                rc_read_receipt_request_interval =
                        resources.getInteger(R.integer.rc_read_receipt_request_interval);
            } catch (Exception e) {
                RLog.e(TAG, "rc_enable_mentioned_message not get value", e);
            }
            try {
                rc_media_selector_contain_video =
                        resources.getBoolean(R.bool.rc_media_selector_contain_video);
            } catch (Exception e) {
                RLog.e(TAG, "rc_media_selector_contain_video not get value", e);
            }
            try {
                rc_enable_automatic_download_voice_msg =
                        resources.getBoolean(R.bool.rc_enable_automatic_download_voice_msg);
            } catch (Exception e) {
                RLog.e(TAG, "rc_enable_automatic_download_voice_msg not get value", e);
            }
            try {
                rc_gifmsg_auto_download_size =
                        resources.getInteger(R.integer.rc_gifmsg_auto_download_size);
            } catch (Exception e) {
                RLog.e(TAG, "rc_gifmsg_auto_download_size not get value", e);
            }
            try {
                rc_max_message_selected_count =
                        resources.getInteger(R.integer.rc_max_message_selected_count);
            } catch (Exception e) {
                RLog.e(TAG, "rc_max_message_selected_count not get value", e);
            }
            try {
                rc_enable_send_combine_message =
                        resources.getBoolean(R.bool.rc_enable_send_combine_message);
            } catch (Exception e) {
                RLog.e(TAG, "rc_enable_send_combine_message not get value", e);
            }

            try {
                showNewMentionMessageBar = resources.getBoolean(R.bool.rc_enable_unread_mention);
            } catch (Exception e) {
                RLog.e(TAG, "rc_enable_unread_mention not get value", e);
            }

            try {
                conversationHistoryMessageCount =
                        resources.getInteger(R.integer.rc_conversation_history_message_count);
                if (conversationHistoryMessageCount > conversationHistoryMessageMaxCount) {
                    conversationHistoryMessageCount = conversationHistoryMessageMaxCount;
                }
            } catch (Exception e) {
                RLog.e(TAG, "rc_conversation_history_message_count not get value", e);
            }

            try {
                conversationRemoteMessageCount =
                        resources.getInteger(R.integer.rc_conversation_remote_message_count);
                if (conversationRemoteMessageCount > conversationRemoteMessageMaxCount) {
                    conversationRemoteMessageCount = conversationRemoteMessageMaxCount;
                }
            } catch (Exception e) {
                RLog.e(TAG, "rc_conversation_remote_message_count not get value", e);
            }

            try {
                conversationShowUnreadMessageCount =
                        resources.getInteger(R.integer.rc_conversation_show_unread_message_count);
                if (conversationShowUnreadMessageCount > conversationShowUnreadMessageMaxCount) {
                    conversationShowUnreadMessageCount = conversationShowUnreadMessageMaxCount;
                }
            } catch (Exception e) {
                RLog.e(TAG, "rc_conversation_show_unread_message_count not get value", e);
            }
        }
    }

    private void initMessageProvider() {
        mMessageListProvider.setDefaultProvider(defaultMessageProvider);
        addMessageProvider(new TextMessageItemProvider());
        addMessageProvider(new ImageMessageItemProvider());
        addMessageProvider(new DestructImageMessageItemProvider());
        addMessageProvider(new HQVoiceMessageItemProvider());
        addMessageProvider(new FileMessageItemProvider());
        addMessageProvider(new GIFMessageItemProvider());
        addMessageProvider(new InformationNotificationMessageItemProvider());
        addMessageProvider(new RecallNotificationMessageItemProvider());
        addMessageProvider(new RichContentMessageItemProvider());
        addMessageProvider(new ReferenceMessageItemProvider());
        addMessageProvider(new SightMessageItemProvider());
        addMessageProvider(new DestructSightMessageItemProvider());
        addMessageProvider(new DestructGifMessageItemProvider());
        addMessageProvider(new DestructTextMessageItemProvider());
        addMessageProvider(new DestructHQVoiceMessageItemProvider());
        addMessageProvider(new DestructVoiceMessageItemProvider());
        addMessageProvider(new HistoryDivMessageItemProvider());
        addMessageProvider(new CombineMessageItemProvider());
        addMessageProvider(new VoiceMessageItemProvider());
        addMessageProvider(new GroupNotificationMessageItemProvider());
        addMessageProvider(new CSPullLeaveMsgItemProvider());
        addMessageProvider(new UnknownMessageItemProvider());
        addMessageProvider(new PublicServiceMultiRichContentMessageProvider());
        addMessageProvider(new PublicServiceRichContentMessageProvider());
    }

    private void initViewProcessor() {
        mConversationViewProcessors.add(new CSConversationUIRenderer());
    }

    private void initMoreClickAction() {
        mMoreClickActions.add(new ForwardClickActions());
        mMoreClickActions.add(new DeleteClickActions());
        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == LayoutDirection.RTL) {
            CollectionsUtils.reverse(mMoreClickActions);
        }
    }

    /**
     * @param index 添加位置
     * @param action 添加点击更多事件
     */
    public void addMoreClickAction(int index, IClickActions action) {
        if (action != null) mMoreClickActions.add(index, action);
    }

    /**
     * 移除点击事件
     *
     * @param action 移除的点击事件
     */
    public void removeMoreClickAction(IClickActions action) {
        if (action != null) {
            mMoreClickActions.remove(action);
        }
    }

    /**
     * ConversationFragment 处理器
     *
     * @param processor
     */
    public void addViewProcessor(IConversationUIRenderer processor) {
        mConversationViewProcessors.add(processor);
    }

    /**
     * @return ConversationFragment 处理器
     */
    public List<IConversationUIRenderer> getViewProcessors() {
        return mConversationViewProcessors;
    }

    /**
     * @param provider 消息列表 item 提供者
     */
    public void addMessageProvider(IMessageProvider provider) {
        if (provider != null) {
            mMessageListProvider.addProvider(provider);
            mConversationSummaryProviders.add(provider);
        }
    }

    /**
     * 替换已有的模板
     *
     * @param oldProviderClass 旧模板 class 类
     * @param provider 新模板
     */
    public void replaceMessageProvider(Class oldProviderClass, IMessageProvider provider) {
        mMessageListProvider.replaceProvider(oldProviderClass, provider);
        int index = -1;
        for (int i = 0; i < mConversationSummaryProviders.size(); i++) {
            IConversationSummaryProvider item = mConversationSummaryProviders.get(i);
            if (item.getClass().equals(oldProviderClass)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            mConversationSummaryProviders.set(index, provider);
        }
    }

    /**
     * @return 获得消息模板列表
     */
    public ProviderManager<UiMessage> getMessageListProvider() {
        return mMessageListProvider;
    }

    /**
     * 获得消息展示信息
     *
     * @param context 上下文
     * @param messageContent 消息类型
     * @return
     */
    public Spannable getMessageSummary(Context context, MessageContent messageContent) {
        Spannable spannable = new SpannableString("");
        Spannable defaultSpannable =
                defaultMessageProvider.getSummarySpannable(context, messageContent);
        if (messageContent == null) {
            return spannable;
        }
        for (IConversationSummaryProvider item : mConversationSummaryProviders) {
            if (item.isSummaryType(messageContent)) {
                try {
                    // SDK内置Provider均用Context拿资源但未判空，所以加判断
                    spannable = item.getSummarySpannable(context, messageContent);
                } catch (Exception e) {
                    RLog.e(TAG, "getMessageSummary error", e);
                    spannable = null;
                }
            }
        }
        return spannable == null ? defaultSpannable : spannable;
    }

    /**
     * 获得消息展示信息
     *
     * @param context 上下文
     * @param conversation 消息内容
     * @return
     * @since 5.2.5
     */
    public Spannable getMessageSummary(Context context, Conversation conversation) {
        Spannable spannable = new SpannableString("");
        Spannable defaultSpannable =
                defaultMessageProvider.getSummarySpannable(context, conversation);
        // 如果最后一条消息不存在,则不展示
        if (conversation.getLatestMessage() == null || conversation.getLatestMessageId() == -1) {
            return spannable;
        }
        for (IConversationSummaryProvider item : mConversationSummaryProviders) {
            if (item.isSummaryType(conversation.getLatestMessage())) {
                try {
                    // SDK内置Provider均用Context拿资源但未判空，所以加判断
                    spannable = item.getSummarySpannable(context, conversation);
                } catch (Exception e) {
                    RLog.e(TAG, "getMessageSummary error", e);
                    spannable = null;
                }
            }
        }
        return spannable == null ? defaultSpannable : spannable;
    }

    /**
     * 是否在消息列表显示名称
     *
     * @param messageContent 消息类型
     * @return
     */
    public boolean showSummaryWithName(MessageContent messageContent) {
        if (messageContent == null) {
            return false;
        }
        for (IConversationSummaryProvider item : mConversationSummaryProviders) {
            if (item.isSummaryType(messageContent)) {
                return item.showSummaryWithName();
            }
        }
        return false;
    }

    /**
     * @param showReceiverUserTitle 单聊是否显示用户昵称
     */
    public void setShowReceiverUserTitle(boolean showReceiverUserTitle) {
        this.showReceiverUserTitle = showReceiverUserTitle;
    }

    /**
     * 单聊是否显示用户昵称 仅支持配置单聊属性
     *
     * @param type 会话类型
     * @return 是否显示
     */
    public boolean isShowReceiverUserTitle(Conversation.ConversationType type) {
        if (!showReceiverUserTitle) {
            switch (type) {
                case PRIVATE:
                case ENCRYPTED:
                    return false;
            }
        }
        return true;
    }

    /**
     * @return 长按是否显示更多选项
     */
    public boolean isShowMoreClickAction() {
        return showMoreClickAction;
    }

    /**
     * @param showMoreClickAction 长按是否显示更多选项
     */
    public void setShowMoreClickAction(boolean showMoreClickAction) {
        this.showMoreClickAction = showMoreClickAction;
    }

    /**
     * @return 是否显示历史消息模板
     */
    public boolean isShowHistoryDividerMessage() {
        return showHistoryDividerMessage;
    }

    /**
     * @param showHistoryDividerMessage 是否显示历史消息模板
     */
    public void setShowHistoryDividerMessage(boolean showHistoryDividerMessage) {
        this.showHistoryDividerMessage = showHistoryDividerMessage;
    }

    /**
     * @param showNewMessageBar 新消息是否显示未读气泡，目前仅支持单群聊（聊天室等，设置无效）
     */
    public void setShowNewMessageBar(boolean showNewMessageBar) {
        this.showNewMessageBar = showNewMessageBar;
    }

    /**
     * 新消息是否显示未读气泡，目前仅支持单群聊
     *
     * @param type 会话类型
     * @return 不支持类型返回false，支持类型返回 showHistoryMessageBar 值
     */
    public boolean isShowNewMessageBar(Conversation.ConversationType type) {
        if (showNewMessageBar) {
            switch (type) {
                case PRIVATE:
                case GROUP:
                case DISCUSSION:
                case ENCRYPTED:
                    return true;
            }
        }
        return false;
    }

    /**
     * 会话页面右上角的未读 @ 消息数提示，目前仅支持群聊
     *
     * @param type 会话类型
     * @return 不支持类型返回 false，支持类型返回 showNewMentionMessageBar 值
     */
    public boolean isShowNewMentionMessageBar(Conversation.ConversationType type) {
        if (showNewMentionMessageBar) {
            switch (type) {
                case GROUP:
                case DISCUSSION:
                    return true;
            }
        }
        return false;
    }

    /**
     * @param showNewMentionMessageBar 是否显示会话页面右上角的未读 @ 消息数提示，仅支持设置群组
     */
    public void setShowNewMentionMessageBar(boolean showNewMentionMessageBar) {
        this.showNewMentionMessageBar = showNewMentionMessageBar;
    }

    public int getConversationHistoryMessageCount() {
        return conversationHistoryMessageCount;
    }

    public void setConversationHistoryMessageCount(int conversationHistoryMessageCount) {
        this.conversationHistoryMessageCount = conversationHistoryMessageCount;
    }

    public int getConversationRemoteMessageCount() {
        return conversationRemoteMessageCount;
    }

    public void setConversationRemoteMessageCount(int conversationRemoteMessageCount) {
        this.conversationRemoteMessageCount = conversationRemoteMessageCount;
    }

    public int getConversationShowUnreadMessageCount() {
        return conversationShowUnreadMessageCount;
    }

    public void setConversationShowUnreadMessageCount(int conversationShowUnreadMessageCount) {
        this.conversationShowUnreadMessageCount = conversationShowUnreadMessageCount;
    }

    /**
     * @param showHistoryMessageBar 是否显示历史未读消息气泡，仅支持设置私聊，群组
     */
    public void setShowHistoryMessageBar(boolean showHistoryMessageBar) {
        this.showHistoryMessageBar = showHistoryMessageBar;
    }

    /**
     * 是否显示历史未读消息气泡，仅支持设置私聊，群组
     *
     * @param type 会话类型
     * @return 不支持类型返回false，支持类型返回 showHistoryMessageBar 值
     */
    public boolean isShowHistoryMessageBar(Conversation.ConversationType type) {
        if (showHistoryMessageBar) {
            switch (type) {
                case PRIVATE:
                case GROUP:
                case DISCUSSION:
                case ENCRYPTED:
                    return true;
            }
        }
        return false;
    }

    public ConversationClickListener getConversationClickListener() {
        return mConversationClickListener;
    }

    public void setConversationClickListener(ConversationClickListener conversationClickListener) {
        mConversationClickListener = conversationClickListener;
    }

    /**
     * 获取会话页面长按消息，弹出框里点击"更多"选项时，底部需要显示的条目。 可以通过对此列表的增删，进行自定义显示。
     *
     * @return 当前设置的点击"更多"时底部显示的条目列表。
     */
    public List<IClickActions> getMoreClickActions() {
        return mMoreClickActions;
    }

    public IMessageViewModelProcessor getViewModelProcessor() {
        return mViewModelProcessor;
    }

    public void setViewModelProcessor(IMessageViewModelProcessor viewModelProcessor) {
        mViewModelProcessor = viewModelProcessor;
    }

    /**
     * 设置已读回执，仅支持单聊，群聊，讨论组，密聊，其余不生效
     *
     * @param enable 回执开关
     */
    public void setEnableReadReceipt(boolean enable) {
        mEnableReadReceipt = enable;
    }

    public void setSupportReadReceiptConversationType(Conversation.ConversationType... types) {
        mSupportReadReceiptConversationTypes.clear();
        mSupportReadReceiptConversationTypes.addAll(Arrays.asList(types));
    }

    /**
     * 仅适用单聊和加密
     *
     * @param type 会话类型
     * @return 不支持类型返回 false, 支持类型 enableReadReceipt 值
     */
    public boolean isShowReadReceipt(Conversation.ConversationType type) {
        if (mEnableReadReceipt) {
            switch (type) {
                case PRIVATE:
                case ENCRYPTED:
                    return mSupportReadReceiptConversationTypes.contains(type);
            }
        }
        return false;
    }

    /**
     * 仅适用群聊和讨论组
     *
     * @param type 会话类型
     * @return 不支持类型返回 false, 支持类型返回 enableReadReceipt 值
     */
    public boolean isShowReadReceiptRequest(Conversation.ConversationType type) {
        if (mEnableReadReceipt) {
            switch (type) {
                case GROUP:
                case DISCUSSION:
                    return mSupportReadReceiptConversationTypes.contains(type);
                default:
                    break;
            }
        }
        return false;
    }

    /**
     * 是否打开多端阅读状态同步功能。 开启之后，在其它端阅读过的消息，当前客户端会同步清掉未读数。
     *
     * @param type 会话类型。该功能仅支持单聊、群聊。
     * @return 功能是否开启。
     */
    public boolean isEnableMultiDeviceSync(Conversation.ConversationType type) {
        if (enableMultiDeviceSync) {
            switch (type) {
                case PRIVATE:
                case ENCRYPTED:
                case GROUP:
                case ULTRA_GROUP:
                case DISCUSSION:
                case SYSTEM: // 5.6.2需求
                case PUBLIC_SERVICE: // 5.6.2需求
                case APP_PUBLIC_SERVICE: // 5.6.2需求
                    return true;
            }
        }
        return false;
    }

    public void setEnableMultiDeviceSync(boolean enableMultiDeviceSync) {
        this.enableMultiDeviceSync = enableMultiDeviceSync;
    }

    public void setConversationLoadMessageType(
            IRongCoreEnum.ConversationLoadMessageType conversationLoadMessageType) {
        this.conversationLoadMessageType = conversationLoadMessageType;
    }

    public IRongCoreEnum.ConversationLoadMessageType getConversationLoadMessageType() {
        return conversationLoadMessageType;
    }

    /**
     * 是否删除远端消息
     *
     * @return 是否删除远端消息
     */
    public boolean isNeedDeleteRemoteMessage() {
        return needDeleteRemoteMessage;
    }

    /**
     * 设置是否删除远端消息
     *
     * @param needDeleteRemoteMessage 是否删除远端消息
     */
    public void setNeedDeleteRemoteMessage(boolean needDeleteRemoteMessage) {
        this.needDeleteRemoteMessage = needDeleteRemoteMessage;
    }

    /**
     * 获取当会话页面删除消息后列表消息为空时，设置是否重新刷新页面
     *
     * @return 是否重新刷新页面
     */
    public boolean isNeedRefreshWhenListIsEmptyAfterDelete() {
        return needRefreshWhenListIsEmptyAfterDelete;
    }

    /**
     * 当会话页面删除消息后列表消息为空时，设置是否重新刷新页面
     *
     * @param needRefreshWhenListIsEmptyAfterDelete 是否重新刷新页面
     */
    public void setNeedRefreshWhenListIsEmptyAfterDelete(
            boolean needRefreshWhenListIsEmptyAfterDelete) {
        this.needRefreshWhenListIsEmptyAfterDelete = needRefreshWhenListIsEmptyAfterDelete;
    }

    private HashMap<String, Integer> mFileSuffixTypeMap = new HashMap<>();

    /**
     * 注册文件消息后缀类型对应的ICON配置列表
     *
     * @param map String：文件后缀（例如："png"、"pdf" 等），如果要替换默认文件图标， key 使用 "default" 进行配置即可 Integer：ICON的
     *     Android 资源 id，需要把 ICON 添加到 drawable 资源目录中
     */
    public void registerFileSuffixTypes(HashMap<String, Integer> map) {
        if (map == null) {
            return;
        }
        this.mFileSuffixTypeMap = map;
    }

    /**
     * 内部使用，客户不需要调用
     *
     * @return 客户注册的文件消息后缀类型Map
     */
    public HashMap<String, Integer> getFileSuffixTypes() {
        return mFileSuffixTypeMap;
    }
}
