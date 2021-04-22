package io.rong.imkit.conversation.extension.component.emoticon;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.RongExtensionManager;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.utils.RongUtils;
import io.rong.imlib.model.Conversation;

public class EmoticonBoard {
    private final String TAG = EmoticonBoard.class.getSimpleName();

    private View mContainer;
    private IEmoticonTab mCurrentTab;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private ViewPager mViewPager;
    private TabPagerAdapter mAdapter;
    private ViewGroup mScrollTab;
    private int mSelected = 0;
    private View mTabAdd;
    private View mTabSetting;
    private boolean mTabBarEnabled = true;
    private boolean mAddEnabled = false;
    private boolean mSettingEnabled = false;
    private IEmoticonClickListener mEmoticonClickListener;
    private IEmoticonSettingClickListener mEmoticonSettingClickListener;
    private Map<String, List<IEmoticonTab>> mEmotionTabs = new LinkedHashMap<>();
    private View mExtraTabBarItem;
    private RongExtensionViewModel mExtensionViewModel;
    private Fragment mFragment;
    private ViewGroup mRoot;

    public EmoticonBoard(Fragment fragment, ViewGroup parent, Conversation.ConversationType type, String targetId) {
        mFragment = fragment;
        mRoot = parent;
        mConversationType = type;
        mTargetId = targetId;
        mExtensionViewModel = new ViewModelProvider(fragment).get(RongExtensionViewModel.class);
        initView(fragment.getContext(), mRoot);
    }

    public View getView() {
        initEmotionTabs();
        return mContainer;
    }

    private void initView(Context context, ViewGroup parent) {
        mContainer = LayoutInflater.from(context).inflate(R.layout.rc_ext_emoticon_tab_container, parent, false);
        mViewPager = mContainer.findViewById(R.id.rc_view_pager);
        mScrollTab = mContainer.findViewById(R.id.rc_emotion_scroll_tab);
        mTabAdd = mContainer.findViewById(R.id.rc_emoticon_tab_add);
        mTabAdd.setVisibility(mAddEnabled ? View.VISIBLE : View.GONE);
        mTabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEmoticonClickListener != null) {
                    mEmoticonClickListener.onAddClick(v);
                }
            }
        });
        mTabSetting = mContainer.findViewById(R.id.rc_emoticon_tab_setting);
        mTabSetting.setVisibility(mSettingEnabled ? View.VISIBLE : View.GONE);
        mTabSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEmoticonSettingClickListener != null) {
                    mEmoticonSettingClickListener.onSettingClick(v);
                }
            }
        });
        LinearLayout tabBar = mContainer.findViewById(R.id.rc_emotion_tab_bar);
        if (mTabBarEnabled) {
            tabBar.setVisibility(View.VISIBLE);
            if (mExtraTabBarItem != null && mAddEnabled) {
                tabBar.addView(mExtraTabBarItem, 1);
            }
        } else {
            tabBar.setVisibility(View.GONE);
        }

        mAdapter = new TabPagerAdapter();
        mViewPager.setAdapter(mAdapter);
        mViewPager.setOffscreenPageLimit(6);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                onPageChanged(mSelected, position);
                mSelected = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        int index;
        if (mCurrentTab != null && (index = getIndex(mCurrentTab)) >= 0) {
            mCurrentTab = null;
            onPageChanged(-1, index);
            mViewPager.setCurrentItem(index);
        } else {
            onPageChanged(-1, 0);
            mAdapter.startUpdate(mViewPager);
        }
    }

    public void initEmotionTabs() {
        if (mEmotionTabs != null && mEmotionTabs.size() > 0) {
            return;
        }
        mEmotionTabs = RongExtensionManager.getInstance().getExtensionConfig().getEmoticonTabs(mConversationType, mTargetId);
        for (IEmoticonTab tab : getAllTabs()) {
            View view = getTabIcon(mFragment.getContext(), tab);
            mScrollTab.addView(view);
        }
        onPageChanged(-1, 0);
        mAdapter.notifyDataSetChanged();
        subscribeUi();
    }

    private void subscribeUi() {
        for (List<IEmoticonTab> tabList : mEmotionTabs.values()) {
            if (tabList != null && tabList.size() > 0) {
                for (IEmoticonTab tab : tabList) {
                    if (tab.getEditInfo() != null) {
                        tab.getEditInfo().observe(mFragment, new Observer<String>() {
                            @Override
                            public void onChanged(String s) {
                                if (s.equals(EmojiTab.DELETE)) {
                                    mExtensionViewModel.getEditTextWidget().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                                } else {
                                    int start = mExtensionViewModel.getEditTextWidget().getSelectionStart();
                                    mExtensionViewModel.getEditTextWidget().getText().insert(start, s);
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    public void setOnEmoticonClickListener(IEmoticonClickListener listener) {
        mEmoticonClickListener = listener;
    }

    public void setOnEmoticonSettingClickListener(IEmoticonSettingClickListener listener) {
        mEmoticonSettingClickListener = listener;
    }

    private int getIndex(IEmoticonTab tab) {
        return getAllTabs().indexOf(tab);
    }

    private IEmoticonTab getTab(int index) {
        return getAllTabs().get(index);
    }

    public List<IEmoticonTab> getTabList(String tag) {
        return mEmotionTabs.get(tag);
    }

    public void addTab(IEmoticonTab tab, String tag) {
        List<IEmoticonTab> tabs = mEmotionTabs.get(tag);
        if (tabs == null) {
            tabs = new ArrayList<>();
            tabs.add(tab);
            mEmotionTabs.put(tag, tabs);
        } else {
            tabs.add(tab);
        }
        int idx = getIndex(tab);
        if (mAdapter != null && mViewPager != null) {
            View view = getTabIcon(mViewPager.getContext(), tab);
            mScrollTab.addView(view, idx);
            mAdapter.notifyDataSetChanged();
            mViewPager.setCurrentItem(idx <= mSelected ? mSelected + 1 : mSelected);
        }
    }

    public void addTab(int index, IEmoticonTab tab, String tag) {
        List<IEmoticonTab> tabs = mEmotionTabs.get(tag);
        if (tabs == null) {
            tabs = new ArrayList<>();
            tabs.add(tab);
            mEmotionTabs.put(tag, tabs);
        } else {
            int count = tabs.size();
            if (index <= count)
                tabs.add(index, tab);
        }
        int idx = getIndex(tab);
        if (mAdapter != null && mViewPager != null) {
            View view = getTabIcon(mViewPager.getContext(), tab);
            mScrollTab.addView(view, idx);
            mAdapter.notifyDataSetChanged();
            mViewPager.setCurrentItem(idx <= mSelected ? mSelected + 1 : mSelected);
        }
    }

    public void removeTab(IEmoticonTab tab, String tag) {
        if (!mEmotionTabs.containsKey(tag)) return;

        List<IEmoticonTab> list = mEmotionTabs.get(tag);
        int index = getIndex(tab);
        if (list != null && list.remove(tab)) {
            if (list.size() == 0) {
                mEmotionTabs.remove(tag);
            }
            mScrollTab.removeViewAt(index);
            mAdapter.notifyDataSetChanged();

            if (mSelected == index) {
                mViewPager.setCurrentItem(mSelected);
                onPageChanged(-1, mSelected);
            }
        }
    }

    public void setCurrentTab(IEmoticonTab tab, String tag) {
        if (mEmotionTabs.containsKey(tag)) {
            mCurrentTab = tab;
            if (mAdapter != null && mViewPager != null) {
                int index = getIndex(tab);
                if (index >= 0) {
                    mViewPager.setCurrentItem(index);
                    mCurrentTab = null;
                }
            }
        }
    }

    public void setVisibility(int visibility) {
        if (mContainer != null) {
            if (visibility == View.VISIBLE) {
                mContainer.setVisibility(View.VISIBLE);
            } else {
                mContainer.setVisibility(View.GONE);
            }
        }
    }

    public int getVisibility() {
        return mContainer != null ? mContainer.getVisibility() : View.GONE;
    }

    public void setTabViewEnable(boolean enable) {
        mTabBarEnabled = enable;
    }

    public void setAddEnable(boolean enable) {
        mAddEnabled = enable;
        if (mTabAdd != null) {
            mTabAdd.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    public void setSettingEnable(boolean enable) {
        mSettingEnabled = enable;
        if (mTabSetting != null) {
            mTabSetting.setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    public void addExtraTab(Context context, Drawable drawable, View.OnClickListener clickListener) {
        mExtraTabBarItem = getTabIcon(context, drawable);
        mExtraTabBarItem.setOnClickListener(clickListener);
    }


    private List<IEmoticonTab> getAllTabs() {
        Collection<List<IEmoticonTab>> c = mEmotionTabs.values();
        List<IEmoticonTab> list = new ArrayList<>();
        for (List<IEmoticonTab> tabs : c) {
            for (int i = 0; tabs != null && i < tabs.size(); i++) {
                list.add(tabs.get(i));
            }
        }
        return list;
    }

    private View getTabIcon(Context context, IEmoticonTab tab) {
        Drawable drawable = tab.obtainTabDrawable(context);
        return getTabIcon(context, drawable);
    }

    private View getTabIcon(Context context, Drawable drawable) {
        View item = LayoutInflater.from(context).inflate(R.layout.rc_ext_emoticon_tab_item, null);
        item.setLayoutParams(new RelativeLayout.LayoutParams(RongUtils.dip2px(60), RongUtils.dip2px(36)));
        ImageView iv = item.findViewById(R.id.rc_emoticon_tab_iv);
        iv.setImageDrawable(drawable);
        item.setOnClickListener(tabClickListener);
        return item;
    }

    private void onPageChanged(int pre, int cur) {
        int count = mScrollTab.getChildCount();
        if (count > 0 && cur < count) {
            if (pre >= 0 && pre < count) {
                ViewGroup preView = (ViewGroup) mScrollTab.getChildAt(pre);
                preView.setBackgroundColor(Color.TRANSPARENT);
            }
            if (cur >= 0) {
                ViewGroup curView = (ViewGroup) mScrollTab.getChildAt(cur);
                curView.setBackgroundColor(curView.getContext().getResources().getColor(R.color.rc_EmoticonTab_bg_select_color));
                int w = curView.getMeasuredWidth();
                if (w != 0) {
                    int screenW = RongUtils.getScreenWidth();
                    if (mAddEnabled) {
                        int addW = mTabAdd.getMeasuredWidth();
                        screenW = screenW - addW;
                    }
                    HorizontalScrollView scrollView = (HorizontalScrollView) mScrollTab.getParent();
                    int scrollX = scrollView.getScrollX();
                    int offset = scrollX - (scrollX / w) * w;
                    if (cur * w < scrollX) {
                        scrollView.smoothScrollBy(offset == 0 ? -w : -offset, 0);
                    } else if (cur * w - scrollX > screenW - w) {
                        scrollView.smoothScrollBy(w - offset, 0);
                    }
                }
            }
        }
        if (cur >= 0 && cur < count) {
            IEmoticonTab curTab = getTab(cur);
            if (curTab != null) curTab.onTableSelected(cur);
        }
    }


    private View.OnClickListener tabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int count = mScrollTab.getChildCount();
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    if (v.equals(mScrollTab.getChildAt(i))) {
                        mViewPager.setCurrentItem(i);
                        break;
                    }
                }
            }
        }
    };

    private class TabPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mEmotionTabs.size();
        }

        @NonNull
        @Override
        public View instantiateItem(ViewGroup container, int position) {
            IEmoticonTab tab = getTab(position);
            View view = tab.obtainTabPager(container.getContext(), container);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
            View layout = (View) object;
            container.removeView(layout);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            if (object instanceof EmojiTab) {
                return 0;
            }
            return POSITION_NONE;
        }
    }
}
