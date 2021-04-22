package io.rong.imkit.feature.destruct.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.utils.TextViewUtils;
import io.rong.imkit.widget.ILinkClickListener;
import io.rong.imkit.widget.LinkTextViewMovementMethod;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.TextMessage;

public class DestructTextMessageItemProvider extends BaseMessageItemProvider<TextMessage> {
    private static final String TAG = DestructTextMessageItemProvider.class.getSimpleName();

    public DestructTextMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View textView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_destruct_text_message, parent, false);
        return new ViewHolder(parent.getContext(), textView);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder, ViewHolder parentHolder, TextMessage message, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        holder.getConvertView().setTag(uiMessage.getMessage().getUId());
        boolean isSender = uiMessage.getMessage().getMessageDirection() == Message.MessageDirection.SEND;
        holder.setBackgroundRes(R.id.tv_unread, isSender ? R.drawable.rc_ic_bubble_right : R.drawable.rc_ic_bubble_left);
        holder.setBackgroundRes(R.id.rc_text, isSender ? R.drawable.rc_ic_bubble_right : R.drawable.rc_ic_bubble_left);
        if (isSender) {
            holder.setVisible(R.id.tv_unread, false);
            holder.setVisible(R.id.rc_text, true);
            holder.setVisible(R.id.fl_send_fire, true);
            holder.setVisible(R.id.fl_receiver_fire, false);
            processTextView(holder, message, uiMessage, position);
        } else {
            holder.setVisible(R.id.fl_send_fire, false);
            holder.setVisible(R.id.fl_receiver_fire, true);
            DestructManager.getInstance().addListener(uiMessage.getMessage().getUId(), new DestructListener(holder, uiMessage), TAG);
            //getReadTime>0，证明已读，开始倒计时
            if (uiMessage.getMessage().getReadTime() > 0) {
                holder.setVisible(R.id.tv_unread, false);
                holder.setVisible(R.id.rc_text, true);
                holder.setVisible(R.id.tv_receiver_fire, true);
                holder.setVisible(R.id.iv_receiver_fire, false);
                String unFinishTime;
                if (TextUtils.isEmpty(uiMessage.getDestructTime())) {
                    unFinishTime = DestructManager.getInstance().getUnFinishTime(uiMessage.getMessage().getUId());
                } else {
                    unFinishTime = uiMessage.getDestructTime();
                }
                holder.setText(R.id.tv_receiver_fire, unFinishTime);
                processTextView(holder, message, uiMessage, position);
                DestructManager.getInstance().startDestruct(uiMessage.getMessage());
            } else {
                holder.setVisible(R.id.tv_unread, true);
                holder.setVisible(R.id.rc_text, false);
                holder.setVisible(R.id.tv_receiver_fire, false);
                holder.setVisible(R.id.iv_receiver_fire, true);
            }
        }
    }

    @Override
    protected boolean onItemClick(ViewHolder holder, TextMessage message, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        if (message != null && message.isDestruct() && !(uiMessage.getMessage().getReadTime() > 0)) {
            holder.setVisible(R.id.tv_unread, false);
            holder.setVisible(R.id.rc_text, true);
            holder.setVisible(R.id.tv_receiver_fire, true);
            holder.setVisible(R.id.iv_receiver_fire, false);
            processTextView(holder, message, uiMessage, position);
            DestructManager.getInstance().startDestruct(uiMessage.getMessage());
        }
        return true;
    }

    private void processTextView(final ViewHolder holder, TextMessage message, final UiMessage uiMessage, int position) {
        final TextView view = holder.getView(R.id.rc_text);
        view.setTag(uiMessage.getMessageId());
        if (uiMessage.getContentSpannable() == null) {
            SpannableStringBuilder spannable = TextViewUtils.getSpannable(message.getContent(), new TextViewUtils.RegularCallBack() {
                @Override
                public void finish(SpannableStringBuilder spannable) {
                    uiMessage.setContentSpannable(spannable);
                    if (view.getTag().equals(uiMessage.getMessageId())) {
                        view.post(new Runnable() {
                            @Override
                            public void run() {
                                view.setText(uiMessage.getContentSpannable());
                            }
                        });
                    }
                }
            });
            uiMessage.setContentSpannable(spannable);
        }
        view.setText(uiMessage.getContentSpannable());
        view.setMovementMethod(new LinkTextViewMovementMethod(new ILinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                boolean result = false;
                if (RongConfigCenter.conversationConfig().getConversationClickListener() != null) {
                    result = RongConfigCenter.conversationConfig().getConversationClickListener().onMessageLinkClick(holder.getContext(), link, uiMessage.getMessage());
                }
                if (result)
                    return true;
                String str = link.toLowerCase();
                if (str.startsWith("http") || str.startsWith("https")) {
                    RouteUtils.routeToWebActivity(view.getContext(), link);
                    result = true;
                }

                return result;
            }
        }));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewParent parent = view.getParent().getParent();
                if (parent instanceof View) {
                    ((View) parent).performClick();
                }
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ViewParent parent = view.getParent().getParent();
                if (parent instanceof View) {
                    return ((View) parent).performLongClick();
                }
                return false;
            }
        });
    }


    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof TextMessage && messageContent.isDestruct();
    }

    @Override
    public Spannable getSummarySpannable(Context context, TextMessage message) {
        return new SpannableString(context.getString(R.string.rc_conversation_summary_content_burn));
    }

    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<ViewHolder> mHolder;
        private UiMessage mUIMessage;

        public DestructListener(ViewHolder pHolder, UiMessage pUIMessage) {
            mHolder = new WeakReference<>(pHolder);
            mUIMessage = pUIMessage;
        }

        @Override
        public void onTick(long millisUntilFinished, String messageId) {
            if (mUIMessage.getMessage().getUId().equals(messageId)) {
                ViewHolder holder = mHolder.get();
                if (holder != null && messageId.equals(holder.getConvertView().getTag())) {
                    holder.setVisible(R.id.tv_receiver_fire, true);
                    holder.setVisible(R.id.iv_receiver_fire, false);
                    String unDestructTime = String.valueOf(Math.max(millisUntilFinished, 1));
                    holder.setText(R.id.tv_receiver_fire, unDestructTime);
                    mUIMessage.setDestructTime(unDestructTime);
                }
            }
        }

        @Override
        public void onStop(String messageId) {
            if (mUIMessage.getMessage().getUId().equals(messageId)) {
                ViewHolder holder = mHolder.get();
                if (holder != null && messageId.equals(holder.getConvertView().getTag())) {
                    holder.setVisible(R.id.tv_receiver_fire, false);
                    holder.setVisible(R.id.iv_receiver_fire, true);
                    mUIMessage.setDestructTime(null);
                }
            }
        }
    }
}
