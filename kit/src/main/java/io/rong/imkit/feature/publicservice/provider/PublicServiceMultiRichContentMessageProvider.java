package io.rong.imkit.feature.publicservice.provider;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.publicservice.message.PublicServiceMultiRichContentMessage;
import io.rong.message.RichContentItem;

public class PublicServiceMultiRichContentMessageProvider extends BaseMessageItemProvider<PublicServiceMultiRichContentMessage> {

    @Override
    protected io.rong.imkit.widget.adapter.ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_public_service_multi_rich_content_message, parent, false);
        MyViewHolder holder = new MyViewHolder(view.getContext(), view);
        holder.height = view.getMeasuredHeight();
        return holder;
    }

    @Override
    protected void bindMessageContentViewHolder(final io.rong.imkit.widget.adapter.ViewHolder holder, io.rong.imkit.widget.adapter.ViewHolder parentHolder, PublicServiceMultiRichContentMessage content, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        final ArrayList<RichContentItem> msgList = content.getMessages();

        if (msgList.size() > 0) {
            holder.setText(R.id.rc_txt,msgList.get(0).getTitle());
            Glide.with(holder.getContext()).load(msgList.get(0).getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into((ImageView) holder.getView(R.id.rc_img));
        }

        int height;
        ViewGroup.LayoutParams params = holder.getConvertView().getLayoutParams();

        PublicAccountMsgAdapter mAdapter = new PublicAccountMsgAdapter(holder.getContext(), msgList);
        ListView lv = holder.getView(R.id.rc_list);
        lv.setAdapter(mAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                RichContentItem item = msgList.get(position + 1);
                RouteUtils.routeToWebActivity(holder.getContext(),item.getUrl());
            }
        });

        height = getListViewHeight(lv) + ((MyViewHolder)holder).height;
        params.height = height;
        params.width = RongUtils.getScreenWidth() - RongUtils.dip2px(32);

        holder.getConvertView().setLayoutParams(params);
        holder.getConvertView().requestLayout();
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
    public Spannable getSummarySpannable(Context context, PublicServiceMultiRichContentMessage richContentMessage) {
        List<RichContentItem> list = richContentMessage.getMessages();
        if (list.size() > 0)
            return new SpannableString(richContentMessage.getMessages().get(0).getTitle());
        else
            return null;
    }

    @Override
    protected boolean onItemClick(ViewHolder holder, PublicServiceMultiRichContentMessage content, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        if (content.getMessages().size() == 0)
            return true;

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
            if (itemList.size() == 0)
                return null;

            return itemList.get(position + 1);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            View providerConvertView = inflater.inflate(R.layout.rc_item_public_service_message, parent, false);

            ImageView iv = providerConvertView.findViewById(R.id.rc_img);
            TextView tv = providerConvertView.findViewById(R.id.rc_txt);
            View divider = providerConvertView.findViewById(R.id.rc_divider);

            if (itemList.size() == 0)
                return null;

            String title = itemList.get(position + 1).getTitle();
            if (title != null)
                tv.setText(title);
            Glide.with(providerConvertView.getContext()).load(itemList.get(position + 1).getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(iv);
            if (position == getCount() - 1) {
                divider.setVisibility(View.GONE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }
            return providerConvertView;
        }
    }
}
