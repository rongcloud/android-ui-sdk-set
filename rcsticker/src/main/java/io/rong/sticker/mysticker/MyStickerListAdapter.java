package io.rong.sticker.mysticker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.sticker.R;
import io.rong.sticker.StickerExtensionModule;
import io.rong.sticker.businesslogic.StickerPackageDbTask;
import io.rong.sticker.businesslogic.StickerPackageDeleteTask;
import io.rong.sticker.emoticontab.RecommendTab;
import io.rong.sticker.emoticontab.StickersTab;
import io.rong.sticker.model.StickerPackage;


/**
 * Created by luoyanlong on 2018/08/22.
 */
public class MyStickerListAdapter extends BaseAdapter {
    private static final String EXTENSION_TAG = StickerExtensionModule.class.getSimpleName();
    private OnNoStickerListener listener;
    private Context mContext;
    private List<StickerPackage> mStickerPackageList;

    MyStickerListAdapter(Context context, List<StickerPackage> list) {
        mContext = context;
        mStickerPackageList = list;
    }

    @Override
    public int getCount() {
        return mStickerPackageList.size();
    }

    @Override
    public Object getItem(int position) {
        return mStickerPackageList.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.rc_sticker_download_item, parent, false);
            viewHolder = new ViewHolder(convertView, this);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.setStickerPackage(mContext, mStickerPackageList.get(position));
        viewHolder.setIsLast(position == getCount() - 1);
        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void setOnNoStickerListener(OnNoStickerListener listener) {
        this.listener = listener;
    }

    private void removePackage(String packageId) {
        int index = -1;
        for (int i = 0; i < mStickerPackageList.size(); i++) {
            if (mStickerPackageList.get(i).getPackageId().equals(packageId)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            mStickerPackageList.remove(index);
            notifyDataSetChanged();
        }
        if (mStickerPackageList.isEmpty()) {
            if (listener != null) {
                listener.onNoSticker();
            }
        }
    }

    private static class ViewHolder {
        private MyStickerListAdapter adapter;
        private Context context;
        private ImageView iv;
        private TextView tv;
        private View btn;
        private View divider;

        private String packageId;
        private boolean isPreloadPackage;

        private View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StickerPackageDeleteTask deleteTask = new StickerPackageDeleteTask(packageId, isPreloadPackage);
                deleteTask.delete();
                adapter.removePackage(packageId);
                removeTab();
            }
        };

        public ViewHolder(View view, MyStickerListAdapter adapter) {
            this.adapter = adapter;
            iv = view.findViewById(R.id.iv);
            tv = view.findViewById(R.id.tv);
            btn = view.findViewById(R.id.btn);
            divider = view.findViewById(R.id.divider);
            view.setTag(this);
            context = view.getContext();
        }

        public void setStickerPackage(Context context, StickerPackage stickerPackage) {
            packageId = stickerPackage.getPackageId();
            isPreloadPackage = stickerPackage.isPreload() == 1;
            Glide.with(context).load(stickerPackage.getCover()).into(iv);
            tv.setText(stickerPackage.getName());
            btn.setOnClickListener(onClickListener);
        }

        void setIsLast(boolean isLast) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) divider.getLayoutParams();
            if (isLast) {
                lp.leftMargin = 0;
            } else {
                lp.leftMargin = context.getResources().getDimensionPixelSize(R.dimen.my_sticker_divider_margin_left);
            }
        }

        void removeTab() {
            if (StickerExtensionModule.sRongExtensionWeakReference != null) {
                RongExtension rongExtension = StickerExtensionModule.sRongExtensionWeakReference.get();
                RecommendTab recommendTab = null;
                if (rongExtension != null) {
                    List<IEmoticonTab> emoticonTabs = rongExtension.getEmoticonBoard().getTabList(EXTENSION_TAG);
                    for (IEmoticonTab emoticonTab : emoticonTabs) {
                        if (emoticonTab instanceof StickersTab) {
                            StickersTab tab = (StickersTab) emoticonTab;
                            if (tab.getStickerPackage().getPackageId().equals(packageId)) {
                                rongExtension.getEmoticonBoard().removeTab(tab, EXTENSION_TAG);
                                break;
                            }
                        } else if (emoticonTab instanceof RecommendTab) {
                            recommendTab = (RecommendTab) emoticonTab;
                        }
                    }

                    // 更新推荐tab
                    List<StickerPackage> notDownloadPackages = StickerPackageDbTask
                            .getInstance()
                            .getRecommendPackages();
                    if (recommendTab == null) {
                        recommendTab = new RecommendTab(notDownloadPackages);
                        rongExtension.getEmoticonBoard().addTab(recommendTab, EXTENSION_TAG);
                    } else {
                        recommendTab.setPackages(notDownloadPackages);
                    }
                }
            }
        }
    }

    public interface OnNoStickerListener {
        void onNoSticker();
    }
}
