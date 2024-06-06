package io.rong.imkit.feature.publicservice.provider;

import android.content.Context;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.picture.tools.FileUtils;
import io.rong.imkit.utils.GlideUtils;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongCoreClientImpl;
import io.rong.imlib.filetransfer.upload.MediaUploadAuthorInfo;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.publicservice.message.PublicServiceMultiRichContentMessage;
import io.rong.message.RichContentItem;
import java.util.ArrayList;
import java.util.List;

public class PublicServiceMultiRichContentMessageProvider
        extends BaseMessageItemProvider<PublicServiceMultiRichContentMessage> {

    /** 公众号内容只有一个时，header 高度 */
    private final int ONE_ITEM_HEAD_HEIGHT = 219;

    /** 公众号内容多个时，header 高度 */
    private final int MULTI_ITEM_HEAD_HEIGHT = 151;

    /** 公众号内容多个时，每个 item 高度 */
    private final int MULTI_ITEM_HEIGHT = 76;

    /** 公众号距右边屏幕间隔 */
    private final int RIGHT_PADDING = 14;

    public PublicServiceMultiRichContentMessageProvider() {
        mConfig.showPortrait = false;
    }

    @Override
    protected io.rong.imkit.widget.adapter.ViewHolder onCreateMessageContentViewHolder(
            ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.rc_item_public_service_multi_rich_content_message,
                                parent,
                                false);
        MyViewHolder holder = new MyViewHolder(view.getContext(), view);
        holder.height = view.getMeasuredHeight();
        return holder;
    }

    @Override
    protected void bindMessageContentViewHolder(
            final io.rong.imkit.widget.adapter.ViewHolder holder,
            io.rong.imkit.widget.adapter.ViewHolder parentHolder,
            PublicServiceMultiRichContentMessage content,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        ImageView imageView = holder.getView(R.id.rc_img);
        ListView lv = holder.getView(R.id.rc_list);
        if (!checkViewsValid(imageView, lv)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getObjectName());
            return;
        }

        final ArrayList<RichContentItem> msgList = content.getMessages();

        if (msgList.size() > 0) {
            holder.setText(R.id.rc_txt, msgList.get(0).getTitle());
            String imgUrl = msgList.get(0).getImageUrl();
            if (FileUtils.isHttp(imgUrl) && RongCoreClientImpl.isPrivateSDK()) {
                RongCoreClient.getInstance()
                        .getMediaUploadAuthorInfo(
                                GlideUtils.getUrlName(imgUrl),
                                imgUrl,
                                new IRongCoreCallback.ResultCallback<MediaUploadAuthorInfo>() {
                                    @Override
                                    public void onSuccess(MediaUploadAuthorInfo auth) {
                                        loadImg(holder.getContext(), imageView, imgUrl, auth);
                                    }

                                    @Override
                                    public void onError(IRongCoreEnum.CoreErrorCode e) {
                                        loadImg(holder.getContext(), imageView, imgUrl, null);
                                    }
                                });
            } else {
                loadImg(holder.getContext(), imageView, imgUrl, null);
            }
        }

        ViewGroup.LayoutParams params = holder.getConvertView().getLayoutParams();

        PublicAccountMsgAdapter mAdapter =
                new PublicAccountMsgAdapter(holder.getContext(), msgList);
        lv.setAdapter(mAdapter);

        lv.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {

                        RichContentItem item = msgList.get(position + 1);
                        RouteUtils.routeToWebActivity(holder.getContext(), item.getUrl());
                    }
                });

        int height = 0;
        if (msgList.size() == 1) {
            height = RongUtils.dip2px(ONE_ITEM_HEAD_HEIGHT);
        } else if (msgList.size() > 1) {
            height =
                    RongUtils.dip2px(MULTI_ITEM_HEAD_HEIGHT)
                            + RongUtils.dip2px(MULTI_ITEM_HEIGHT) * (msgList.size() - 1);
        }

        params.height = height;
        params.width = RongUtils.getScreenWidth() - RongUtils.dip2px(RIGHT_PADDING);

        holder.getConvertView().setLayoutParams(params);
        holder.getConvertView().requestLayout();
    }

    private static void loadImg(
            Context context, ImageView img, String url, MediaUploadAuthorInfo auth) {
        Glide.with(context)
                .load(GlideUtils.buildAuthUrl(Uri.parse(url), auth))
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(img);
    }

    private int getListViewHeight(ListView list) {
        int totalHeight = 0;
        View item;
        ListAdapter adapter = list.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            item = adapter.getView(i, null, list);
            totalHeight = totalHeight + item.getLayoutParams().height;
        }
        return totalHeight;
    }

    @Override
    public Spannable getSummarySpannable(
            Context context, PublicServiceMultiRichContentMessage richContentMessage) {
        List<RichContentItem> list = richContentMessage.getMessages();
        if (list.size() > 0)
            return new SpannableString(richContentMessage.getMessages().get(0).getTitle());
        else return null;
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            PublicServiceMultiRichContentMessage content,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        if (content.getMessages().size() == 0) return true;

        String url = content.getMessages().get(0).getUrl();
        RouteUtils.routeToWebActivity(holder.getContext(), url);
        return true;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof PublicServiceMultiRichContentMessage;
    }

    protected static class MyViewHolder extends io.rong.imkit.widget.adapter.ViewHolder {
        public int height;

        public MyViewHolder(Context context, View itemView) {
            super(context, itemView);
        }
    }

    private static class PublicAccountMsgAdapter extends android.widget.BaseAdapter {

        LayoutInflater inflater;
        ArrayList<RichContentItem> itemList;
        int itemCount;

        public PublicAccountMsgAdapter(Context context, ArrayList<RichContentItem> msgList) {
            inflater = LayoutInflater.from(context);
            itemList = new ArrayList<>();
            itemList.addAll(msgList);
            itemCount = msgList.size() - 1;
        }

        @Override
        public int getCount() {
            return itemCount;
        }

        @Override
        public RichContentItem getItem(int position) {
            if (itemList.size() == 0) return null;

            return itemList.get(position + 1);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            View providerConvertView =
                    inflater.inflate(R.layout.rc_item_public_service_message, parent, false);

            ImageView iv = providerConvertView.findViewById(R.id.rc_img);
            TextView tv = providerConvertView.findViewById(R.id.rc_txt);
            View divider = providerConvertView.findViewById(R.id.rc_divider);

            if (itemList.size() == 0) return null;

            String title = itemList.get(position + 1).getTitle();
            if (title != null) tv.setText(title);
            String imgUrl = itemList.get(position + 1).getImageUrl();
            if (FileUtils.isHttp(imgUrl) && RongCoreClientImpl.isPrivateSDK()) {
                RongCoreClient.getInstance()
                        .getMediaUploadAuthorInfo(
                                GlideUtils.getUrlName(imgUrl),
                                imgUrl,
                                new IRongCoreCallback.ResultCallback<MediaUploadAuthorInfo>() {
                                    @Override
                                    public void onSuccess(MediaUploadAuthorInfo auth) {
                                        loadImg(providerConvertView.getContext(), iv, imgUrl, auth);
                                    }

                                    @Override
                                    public void onError(IRongCoreEnum.CoreErrorCode e) {
                                        loadImg(providerConvertView.getContext(), iv, imgUrl, null);
                                    }
                                });
            } else {
                loadImg(providerConvertView.getContext(), iv, imgUrl, null);
            }
            if (position == getCount() - 1) {
                divider.setVisibility(View.GONE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }
            return providerConvertView;
        }
    }
}
