package io.rong.sight.player;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.activity.RongBaseNoActionbarActivity;
import io.rong.imkit.event.actionevent.BaseMessageEvent;
import io.rong.imkit.event.actionevent.DeleteEvent;
import io.rong.imkit.event.actionevent.RecallEvent;
import io.rong.imlib.RongCommonDefine;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.SightMessage;
import io.rong.sight.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SightPlayerActivity extends RongBaseNoActionbarActivity {

    private static final String TAG = "SightPlayerActivity";
    private static final int VIDEO_MESSAGE_COUNT = 10; // 每次获取的图片消息数量。
    protected ViewPager2 mViewPager;
    protected SightMessage mCurrentSightMessage;
    protected Message mMessage;
    protected boolean mFromList;
    protected Conversation.ConversationType mConversationType;

    protected String mTargetId;

    private boolean isFirstTime = true;
    private VideoPagerAdapter mVideoPagerAdapter;
    private int currentSelectMessageId = -1;

    protected ViewPager2.OnPageChangeCallback mPageChangeListener =
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    if (position == (mVideoPagerAdapter.getItemCount() - 1)) {
                        if (mVideoPagerAdapter.getItemCount() > 0) {
                            getSightMessageList(
                                    mVideoPagerAdapter.getItem(position).getMessageId(),
                                    RongCommonDefine.GetMessageDirection.BEHIND);
                        }
                    } else if (position == 0) {
                        if (mVideoPagerAdapter.getItemCount() > 0) {
                            getSightMessageList(
                                    mVideoPagerAdapter.getItem(position).getMessageId(),
                                    RongCommonDefine.GetMessageDirection.FRONT);
                        }
                    }
                    currentSelectMessageId = mVideoPagerAdapter.getItem(position).getMessageId();
                }
            };

    RongIMClient.OnRecallMessageListener mOnRecallMessageListener =
            new RongIMClient.OnRecallMessageListener() {
                @Override
                public boolean onMessageRecalled(
                        Message message, RecallNotificationMessage recallNotificationMessage) {
                    if (currentSelectMessageId == message.getMessageId()) {
                        new AlertDialog.Builder(
                                        SightPlayerActivity.this,
                                        AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                .setMessage(getString(io.rong.imkit.R.string.rc_recall_success))
                                .setPositiveButton(
                                        getString(io.rong.imkit.R.string.rc_dialog_ok),
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        })
                                .setCancelable(false)
                                .show();
                    } else {
                        mVideoPagerAdapter.removeRecallItem(message.getMessageId());
                        if (mVideoPagerAdapter.getItemCount() == 0) {
                            finish();
                        }
                    }
                    return false;
                }
            };

    BaseMessageEvent mBaseMessageEvent =
            new BaseMessageEvent() {
                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    RLog.d(TAG, "MessageDeleteEvent");
                    if (event.getMessageIds() != null) {
                        for (int messageId : event.getMessageIds()) {
                            mVideoPagerAdapter.removeRecallItem(messageId);
                        }
                        if (mVideoPagerAdapter.getItemCount() == 0) {
                            finish();
                        }
                    }
                }

                @Override
                public void onRecallEvent(RecallEvent event) {
                    if (currentSelectMessageId == event.getMessageId()) {
                        new AlertDialog.Builder(
                                        SightPlayerActivity.this,
                                        AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                .setMessage(getString(io.rong.imkit.R.string.rc_recall_success))
                                .setPositiveButton(
                                        getString(io.rong.imkit.R.string.rc_dialog_ok),
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        })
                                .setCancelable(false)
                                .show();
                    } else {
                        mVideoPagerAdapter.removeRecallItem(event.getMessageId());
                        if (mVideoPagerAdapter.getItemCount() == 0) {
                            finish();
                        }
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow()
                .setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.rc_activity_sight_player);
        initView();
        initData();
        IMCenter.getInstance().addOnRecallMessageListener(mOnRecallMessageListener);
        IMCenter.getInstance().addMessageEventListener(mBaseMessageEvent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IMCenter.getInstance().removeOnRecallMessageListener(mOnRecallMessageListener);
        IMCenter.getInstance().removeMessageEventListener(mBaseMessageEvent);
    }

    private void initData() {
        mMessage = getIntent().getParcelableExtra("Message");
        mCurrentSightMessage = getIntent().getParcelableExtra("SightMessage");
        mConversationType = mMessage.getConversationType();
        mTargetId = mMessage.getTargetId();
        mMessage.setContent(mCurrentSightMessage);
        mFromList = getIntent().getBooleanExtra("fromList", false);
        initPager();
    }

    private void initPager() {
        // 阅后即焚和引用消息只显示一个
        mVideoPagerAdapter = new VideoPagerAdapter(this, mMessage.getMessageId());

        ArrayList<Message> messages = new ArrayList<>();
        mViewPager.setAdapter(mVideoPagerAdapter);

        if (mMessage.getContent().isDestruct()
                || mMessage.getContent() instanceof ReferenceMessage
                || Conversation.ConversationType.ULTRA_GROUP.equals(
                        mMessage.getConversationType())) {
            messages.add(mMessage);
            mVideoPagerAdapter.addMessage(messages, mFromList, true);
        } else {
            getSightMessageList(
                    mMessage.getMessageId(), RongCommonDefine.GetMessageDirection.FRONT);
            getSightMessageList(
                    mMessage.getMessageId(), RongCommonDefine.GetMessageDirection.BEHIND);
        }
    }

    private void getSightMessageList(
            int messageId, final RongCommonDefine.GetMessageDirection direction) {
        if (mConversationType != null && !TextUtils.isEmpty(mTargetId)) {
            RongIMClient.getInstance()
                    .getHistoryMessages(
                            mConversationType,
                            mTargetId,
                            "RC:SightMsg",
                            messageId,
                            VIDEO_MESSAGE_COUNT,
                            direction,
                            new RongIMClient.ResultCallback<List<Message>>() {
                                @Override
                                public void onSuccess(List<Message> messages) {
                                    runOnUiThread(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    List<Message> lists = new ArrayList<>();
                                                    if (messages != null) {
                                                        if (direction.equals(
                                                                RongCommonDefine.GetMessageDirection
                                                                        .FRONT)) {
                                                            Collections.reverse(messages);
                                                        }
                                                        lists.addAll(messages);
                                                    }
                                                    if (direction.equals(
                                                            RongCommonDefine.GetMessageDirection
                                                                    .FRONT)) {
                                                        if (isFirstTime) {
                                                            lists.add(mMessage);
                                                        }
                                                        mVideoPagerAdapter.addMessage(
                                                                lists, mFromList, true);
                                                        if (isFirstTime) {
                                                            int index =
                                                                    mVideoPagerAdapter
                                                                            .getIndexByMessageId(
                                                                                    mMessage
                                                                                            .getMessageId());
                                                            if (index != -1) {
                                                                mViewPager.setCurrentItem(
                                                                        index, false);
                                                            }
                                                            isFirstTime = false;
                                                        }
                                                    } else if (lists.size() > 0) {
                                                        mVideoPagerAdapter.addMessage(
                                                                lists, mFromList, false);
                                                    }
                                                }
                                            });
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {
                                    // do nothing
                                }
                            });
        }
    }

    private void initView() {
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.registerOnPageChangeCallback(mPageChangeListener);
        mViewPager.setOffscreenPageLimit(3);
    }

    public static class VideoPagerAdapter extends FragmentStateAdapter {

        private final int mDefaultMessageId;
        List<Message> mMessages;
        boolean mFromList;

        public VideoPagerAdapter(@NonNull FragmentActivity fragmentActivity, int defaultMessageId) {
            super(fragmentActivity);
            mMessages = new ArrayList<>();
            mDefaultMessageId = defaultMessageId;
        }

        public void addMessage(List<Message> messages, boolean fromList, boolean direction) {
            if (messages == null || messages.size() == 0) {
                return;
            }
            mFromList = fromList;
            List<Message> deduplication = deduplication(messages);
            if (direction) {
                mMessages.addAll(0, deduplication);
                notifyItemRangeInserted(0, deduplication.size());
            } else {
                int size = mMessages.size();
                mMessages.addAll(deduplication);
                notifyItemRangeInserted(size, deduplication.size());
            }
        }

        public List<Message> deduplication(List<Message> messages) {
            List<Message> list = new ArrayList<>();
            Set<Integer> set = new HashSet<>();
            for (Message message : mMessages) {
                set.add(message.getMessageId());
            }

            for (Message message : messages) {
                if (!set.contains(message.getMessageId())) {
                    list.add(message);
                    set.add(message.getMessageId());
                }
            }
            return list;
        }

        private SightPlayerFragment createFragment(Message message, boolean fromList) {
            SightPlayerFragment sightPlayerFragment = new SightPlayerFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable("Message", message);
            bundle.putParcelable("SightMessage", (SightMessage) message.getContent());
            bundle.putBoolean("fromList", fromList);
            bundle.putBoolean("auto_play", mDefaultMessageId == message.getMessageId());
            sightPlayerFragment.setArguments(bundle);
            return sightPlayerFragment;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return createFragment(mMessages.get(position), mFromList);
        }

        @Override
        public int getItemCount() {
            return mMessages.size();
        }

        public Message getItem(int position) {
            return mMessages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mMessages.get(position).getMessageId();
        }

        private void removeRecallItem(int messageId) {
            Iterator<Message> iterator = mMessages.iterator();
            while (iterator.hasNext()) {
                Message message = iterator.next();
                if (message.getMessageId() == messageId) {
                    int index = getIndexByMessageId(messageId);
                    if (index == -1) {
                        return;
                    }
                    iterator.remove();
                    notifyItemRemoved(index);
                    break;
                }
            }
        }

        public int getIndexByMessageId(int messageId) {
            for (int i = 0; i < mMessages.size(); i++) {
                if (mMessages.get(i).getMessageId() == messageId) {
                    return i;
                }
            }
            return -1;
        }
    }
}
