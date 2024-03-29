package io.rong.imkit.config;

import android.content.Context;
import android.content.res.Resources;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.conversationlist.provider.BaseConversationProvider;
import io.rong.imkit.conversationlist.provider.PrivateConversationProvider;
import io.rong.imkit.widget.adapter.IViewProvider;
import io.rong.imkit.widget.adapter.ProviderManager;
import io.rong.imlib.model.Conversation;
import java.util.ArrayList;
import java.util.List;

public class ConversationListConfig {

    private final String TAG = "ConversationListConfig";
    private final Conversation.ConversationType[] mSupportedTypes = {
        Conversation.ConversationType.PRIVATE,
        Conversation.ConversationType.GROUP,
        Conversation.ConversationType.SYSTEM,
        Conversation.ConversationType.CUSTOMER_SERVICE,
        Conversation.ConversationType.CHATROOM,
        Conversation.ConversationType.APP_PUBLIC_SERVICE,
        Conversation.ConversationType.PUBLIC_SERVICE,
        Conversation.ConversationType.ENCRYPTED
    };
    private ConversationListBehaviorListener mListener;
    private boolean mIsEnableConnectStateNotice = true;
    // 会话列表页是否自动下载高清语音
    private boolean mEnableAutomaticDownloadHQVoice = true;
    // 每页拉取的会话条数, 默认 100.
    private int mConversationCountPerPage = 100;
    //    会话列表延时刷新时间（防止消息量过大导致卡顿,此值不能设置为500ms）
    private int delayRefreshTime = 5000;
    // ConversationListViewModel 拉取会话列表是否优先显示置顶会话
    private boolean topPriority = true;
    private ProviderManager<BaseUiConversation> mProviderManager;
    private DataProcessor<Conversation> mDataProcessor;

    private BaseDataProcessor<Conversation> mConversationListDataProcessor =
            new DefaultConversationListProcessor();

    public ConversationListConfig() {
        List<IViewProvider<BaseUiConversation>> providerList = new ArrayList<>();
        providerList.add(new PrivateConversationProvider());
        mProviderManager = new ProviderManager<>(providerList);
        mProviderManager.setDefaultProvider(new BaseConversationProvider());
    }

    public void initConfig(Context context) {
        if (context != null) {
            Resources resources = context.getResources();
            try {
                mIsEnableConnectStateNotice =
                        resources.getBoolean(R.bool.rc_is_show_warning_notification);
            } catch (Exception e) {
                RLog.e(TAG, "rc_is_show_warning_notification not get value", e);
            }
        }
    }

    public void setBehaviorListener(ConversationListBehaviorListener listener) {
        this.mListener = listener;
    }

    public void setCountPerPage(int count) {
        this.mConversationCountPerPage = count;
    }

    public void setConversationListProvider(ProviderManager<BaseUiConversation> providerManager) {
        this.mProviderManager = providerManager;
    }

    public void setConversationProvider(BaseConversationProvider provider) {
        this.mProviderManager.addProvider(provider);
    }

    public ProviderManager<BaseUiConversation> getProviderManager() {
        return mProviderManager;
    }

    public boolean isEnableAutomaticDownloadHQVoice() {
        return mEnableAutomaticDownloadHQVoice;
    }

    public void setEnableAutomaticDownloadHQVoice(boolean enable) {
        this.mEnableAutomaticDownloadHQVoice = enable;
    }

    public DataProcessor<Conversation> getDataProcessor() {
        // mDataProcessor 的处理是为了兼容旧版本。
        if (mDataProcessor != null) {
            return mDataProcessor;
        } else {
            return mConversationListDataProcessor;
        }
    }

    /**
     * 设置数据处理器。
     *
     * @param dataFilter 处理器
     * @deprecated 此方法以废弃，请使用{@link #setDataProcessor(BaseDataProcessor)}
     */
    @Deprecated
    public void setDataProcessor(DataProcessor<Conversation> dataFilter) {
        this.mDataProcessor = dataFilter;
    }

    public void setDataProcessor(BaseDataProcessor<Conversation> dataFilter) {
        this.mConversationListDataProcessor = dataFilter;
    }

    public ConversationListBehaviorListener getListener() {
        return mListener;
    }

    public boolean isEnableConnectStateNotice() {
        return mIsEnableConnectStateNotice;
    }

    public int getConversationCountPerPage() {
        return mConversationCountPerPage;
    }

    public int getDelayRefreshTime() {
        return delayRefreshTime;
    }

    /**
     * 设置会话列表延时刷新时间，此值不能设置为500ms
     *
     * @param delayRefreshTime 毫秒
     */
    public void setDelayRefreshTime(int delayRefreshTime) {
        this.delayRefreshTime = delayRefreshTime;
    }

    public boolean isTopPriority() {
        return topPriority;
    }

    /**
     * ConversationListViewModel 拉取会话列表是否优先显示置顶会话
     *
     * @param topPriority 是否优先显示置顶会话（查询结果的排序方式，是否置顶优先，传 true 表示置顶会话优先返回，传 false 结果只以会话时间排序）
     */
    public void setTopPriority(boolean topPriority) {
        this.topPriority = topPriority;
    }
}
