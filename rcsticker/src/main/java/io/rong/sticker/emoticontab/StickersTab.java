package io.rong.sticker.emoticontab;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.sticker.R;
import io.rong.sticker.businesslogic.StickerPackageStorageTask;
import io.rong.sticker.model.Sticker;
import io.rong.sticker.model.StickerPackage;
import io.rong.sticker.widget.IndicatorView;
import io.rong.sticker.widget.StickerGridItemView;
import java.util.List;

/** Created by luoyanlong on 2018/08/02. 展示一个已经下载的表情包 */
public class StickersTab implements IEmoticonTab {

    private StickerPackage stickerPackage;
    private IndicatorView indicatorView;

    public StickersTab(StickerPackage stickerPackage) {
        this.stickerPackage = stickerPackage;
    }

    @Override
    public Drawable obtainTabDrawable(Context context) {
        String iconFilePath =
                StickerPackageStorageTask.getStickerPackageIconFilePath(stickerPackage);
        return new BitmapDrawable(context.getResources(), iconFilePath);
    }

    @Override
    public View obtainTabPager(final Context context, ViewGroup viewGroup) {
        View view =
                LayoutInflater.from(context).inflate(R.layout.rc_sticker_pages, viewGroup, false);
        ViewPager2 viewPager = view.findViewById(R.id.sticker_view_pager);
        StickerPagerAdapter adapter = new StickerPagerAdapter(stickerPackage.getStickers());
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        indicatorView.setSelect(position);
                    }
                });
        indicatorView = view.findViewById(R.id.indicator_view);
        indicatorView.setCount(adapter.getItemCount());
        return view;
    }

    @Override
    public void onTableSelected(int position) {}

    @Override
    public LiveData<String> getEditInfo() {
        return null;
    }

    public StickerPackage getStickerPackage() {
        return stickerPackage;
    }

    /** 显示一页表情 */
    private class StickerPagerAdapter extends RecyclerView.Adapter<StickerPagerViewHolder> {

        /** 每页表情数 */
        private static final int STICKERS_PER_PAGE = 8;

        private List<Sticker> stickerList;

        StickerPagerAdapter(List<Sticker> stickerList) {
            this.stickerList = stickerList;
        }

        @NonNull
        @Override
        public StickerPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            GridView gridView =
                    (GridView)
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.rc_sticker_page, parent, false);
            return new StickerPagerViewHolder(parent.getContext(), gridView);
        }

        @Override
        public void onBindViewHolder(@NonNull StickerPagerViewHolder holder, int position) {
            GridView gridView = holder.gridView;
            gridView.setAdapter(new GridViewAdapter(holder.context, getStickersForPage(position)));
        }

        @Override
        public int getItemCount() {
            return (stickerList.size() - 1) / STICKERS_PER_PAGE + 1;
        }

        /**
         * 根据当前页数，返回当前页对应的表情列表
         *
         * @param page 页数，从0开始
         * @return 这页应该显示的表情
         */
        private List<Sticker> getStickersForPage(int page) {
            int start = page * STICKERS_PER_PAGE;
            int end = (page + 1) * STICKERS_PER_PAGE;
            end = Math.min(end, stickerList.size());
            return stickerList.subList(start, end);
        }
    }

    private class StickerPagerViewHolder extends RecyclerView.ViewHolder {
        Context context;
        GridView gridView;

        public StickerPagerViewHolder(Context context, @NonNull GridView gridView) {
            super(gridView);
            this.context = context;
            this.gridView = gridView;
        }
    }

    /** 显示一个表情 */
    private class GridViewAdapter extends BaseAdapter {
        Context context;
        List<Sticker> stickerList;

        GridViewAdapter(Context context, List<Sticker> stickers) {
            this.context = context;
            stickerList = stickers;
        }

        @Override
        public int getCount() {
            return stickerList == null ? 0 : stickerList.size();
        }

        @Override
        public Object getItem(int position) {
            if (stickerList != null) {
                return stickerList.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = new StickerGridItemView(context);
            }
            ((StickerGridItemView) view).setSticker(stickerList.get(position));
            return view;
        }
    }
}
