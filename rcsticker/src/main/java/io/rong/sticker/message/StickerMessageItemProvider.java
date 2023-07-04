package io.rong.sticker.message;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.felipecsl.gifimageview.library.GifImageView;
import io.rong.common.RLog;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.MessageContent;
import io.rong.sticker.R;
import io.rong.sticker.businesslogic.GifImageLoader;
import java.util.List;
import java.util.Locale;

/** Created by luoyanlong on 2018/08/03. 表情消息提供者 */
public class StickerMessageItemProvider extends BaseMessageItemProvider<StickerMessage> {
    private static final String FORMAT = "[%s]";

    public StickerMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rc_sticker_messsage_item, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            final ViewHolder holder,
            ViewHolder parentHolder,
            StickerMessage stickerMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        ViewGroup.LayoutParams lp = holder.getConvertView().getLayoutParams();
        lp.width = RongUtils.dip2px(stickerMessage.getWidth() / 2);
        lp.height = RongUtils.dip2px(stickerMessage.getHeight() / 2);
        holder.getConvertView().setLayoutParams(lp);
        final GifImageView gifImageView = holder.getView(R.id.gif_view);
        final View loading = holder.getView(R.id.loading);
        final View fail = holder.getView(R.id.fail);
        if (!checkViewsValid(gifImageView, loading, fail)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getObjectName());
            return;
        }
        showLoading(gifImageView, loading, fail);
        final String tag = stickerMessage.getPackageId() + stickerMessage.getStickerId();
        holder.getConvertView().setTag(tag);
        GifImageLoader.getInstance()
                .obtain(
                        stickerMessage,
                        new GifImageLoader.SimpleCallback() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                if (holder.getConvertView().getTag() != null
                                        && holder.getConvertView().getTag().equals(tag)) {
                                    showContent(gifImageView, loading, fail);
                                    gifImageView.setBytes(bytes);
                                    gifImageView.startAnimation();
                                }
                            }

                            @Override
                            public void onFail() {
                                if (holder.getConvertView().getTag() != null
                                        && holder.getConvertView().getTag().equals(tag)) {
                                    showFail(gifImageView, loading, fail);
                                }
                            }
                        });
    }

    private void showLoading(GifImageView gifImageView, View loading, View fail) {
        loading.setVisibility(View.VISIBLE);
        gifImageView.setVisibility(View.GONE);
        fail.setVisibility(View.GONE);
    }

    private void showFail(GifImageView gifImageView, View loading, View fail) {
        loading.setVisibility(View.GONE);
        gifImageView.setVisibility(View.GONE);
        fail.setVisibility(View.VISIBLE);
    }

    private void showContent(GifImageView gifImageView, View loading, View fail) {
        loading.setVisibility(View.GONE);
        gifImageView.setVisibility(View.VISIBLE);
        fail.setVisibility(View.GONE);
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            StickerMessage stickerMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof StickerMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, StickerMessage stickerMessage) {
        if (stickerMessage != null && !TextUtils.isEmpty(stickerMessage.getDigest())) {
            String content = String.format(Locale.getDefault(), FORMAT, stickerMessage.getDigest());
            return new SpannableString(content);
        } else {
            return new SpannableString("");
        }
    }
}
