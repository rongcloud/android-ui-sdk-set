package io.rong.imkit.conversation.extension.component.emoticon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import io.rong.imkit.R;

public class EmojiTab implements IEmoticonTab {
    private final int INITIAL_INDEX = 0;
    private LayoutInflater mLayoutInflater;
    private LinearLayout mIndicator;
    private int mPreIndex = 0;
    private int mEmojiCountPerPage;
    private MutableLiveData<String> mEmojiLiveData = new MutableLiveData<>();
    static final String DELETE = "delete";

    @Override
    public Drawable obtainTabDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_tab_emoji);
    }

    @Override
    public View obtainTabPager(Context context, ViewGroup viewGroup) {
        return initView(context, viewGroup);
    }

    @Override
    public void onTableSelected(int position) {

    }

    @Override
    public LiveData<String> getEditInfo() {
        return mEmojiLiveData;
    }

    private View initView(final Context context, ViewGroup root) {
        int count = AndroidEmoji.getEmojiSize();

        try {
            mEmojiCountPerPage = context.getResources().getInteger(context.getResources().getIdentifier("rc_extension_emoji_count_per_page", "integer", context.getPackageName()));
        } catch (Exception e) {
            mEmojiCountPerPage = 20;
        }

        int pages = count / (mEmojiCountPerPage) + ((count % mEmojiCountPerPage) != 0 ? 1 : 0);

        View view = LayoutInflater.from(context).inflate(R.layout.rc_ext_emoji_pager, root, false);
        ViewPager2 viewPager = view.findViewById(R.id.rc_view_pager);
        this.mIndicator = view.findViewById(R.id.rc_indicator);
        mLayoutInflater = LayoutInflater.from(context);

        viewPager.setAdapter(new EmojiPagerAdapter(pages));
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                onIndicatorChanged(mPreIndex, position);
                mPreIndex = position;
            }
        });
        viewPager.setOffscreenPageLimit(1);

        initIndicator(pages, mIndicator);
        viewPager.setCurrentItem(INITIAL_INDEX);
        onIndicatorChanged(-1, INITIAL_INDEX);
        return view;
    }

    private class EmojiPagerAdapter extends RecyclerView.Adapter<EmojiViewHolder> {
        int count;

        public EmojiPagerAdapter(int count) {
            super();
            this.count = count;
        }

        @NonNull
        @Override
        public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            GridView gridView = (GridView) mLayoutInflater.inflate(R.layout.rc_ext_emoji_grid_view, parent, false);
            return new EmojiViewHolder(gridView);
        }

        @Override
        public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
            GridView gridView = holder.gridView;
            gridView.setAdapter(new EmojiAdapter(position * mEmojiCountPerPage, AndroidEmoji.getEmojiSize()));
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    int index = position + mPreIndex * mEmojiCountPerPage;
                    if (position == mEmojiCountPerPage) {
                        mEmojiLiveData.postValue(DELETE);
                    } else {
                        if (index >= AndroidEmoji.getEmojiSize()) {
                            mEmojiLiveData.postValue(DELETE);
                        } else {
                            int code = AndroidEmoji.getEmojiCode(index);
                            char[] chars = Character.toChars(code);
                            StringBuilder key = new StringBuilder(Character.toString(chars[0]));
                            for (int i = 1; i < chars.length; i++) {
                                key.append(chars[i]);
                            }
                            mEmojiLiveData.postValue(key.toString());
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return count;
        }
    }


    private class EmojiAdapter extends BaseAdapter {
        int count;
        int index;

        public EmojiAdapter(int index, int count) {
            this.count = Math.min(mEmojiCountPerPage, count - index);
            this.index = index;
        }

        @Override
        public int getCount() {
            return count + 1;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = mLayoutInflater.inflate(R.layout.rc_ext_emoji_item, null);
                viewHolder.emojiIV = convertView.findViewById(R.id.rc_ext_emoji_item);
                convertView.setTag(viewHolder);
            }
            viewHolder = (ViewHolder) convertView.getTag();
            if (position == mEmojiCountPerPage || position + index == AndroidEmoji.getEmojiSize()) {
                viewHolder.emojiIV.setImageResource(R.drawable.rc_icon_emoji_delete);
            } else {
                viewHolder.emojiIV.setImageDrawable(AndroidEmoji.getEmojiDrawable(parent.getContext(), index + position));
            }

            return convertView;
        }
    }

    private void initIndicator(int pages, LinearLayout indicator) {
        for (int i = 0; i < pages; i++) {
            ImageView imageView = (ImageView) mLayoutInflater.inflate(R.layout.rc_ext_indicator, null);
            imageView.setImageResource(R.drawable.rc_ext_indicator);
            indicator.addView(imageView);
        }
    }

    private void onIndicatorChanged(int pre, int cur) {
        int count = mIndicator.getChildCount();
        if (count > 0 && pre < count && cur < count) {
            if (pre >= 0) {
                ImageView preView = (ImageView) mIndicator.getChildAt(pre);
                preView.setImageResource(R.drawable.rc_ext_indicator);
            }
            if (cur >= 0) {
                ImageView curView = (ImageView) mIndicator.getChildAt(cur);
                curView.setImageResource(R.drawable.rc_ext_indicator_hover);
            }
        }
    }

    private static class EmojiViewHolder extends RecyclerView.ViewHolder {
        GridView gridView;

        EmojiViewHolder(@NonNull View itemView) {
            super(itemView);
            gridView = (GridView) itemView;
        }
    }

    private static class ViewHolder {
        ImageView emojiIV;
    }
}
