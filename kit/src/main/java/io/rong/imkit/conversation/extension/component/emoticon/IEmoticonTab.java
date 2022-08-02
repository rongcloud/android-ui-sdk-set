package io.rong.imkit.conversation.extension.component.emoticon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.LiveData;


public interface IEmoticonTab {

    /**
     * 构造 tab 的小图标，用于显示在 tab bar中。
     *
     * @param context 应用上下文。
     * @return 图标的 drawable，不能为 null。
     */
    Drawable obtainTabDrawable(Context context);

    /**
     * 构造 table 页面。
     *
     * @param context 应用上下文。
     * @return 构造后的 table view，不能为 null。
     */
    View obtainTabPager(Context context, ViewGroup parent);

    /**
     * 表情面板左右滑动时，回调此方法。
     *
     * @param position 当前 table 的位置。
     */
    void onTableSelected(int position);

    /**
     * 返回 tab 页对应输入框的更新信息
     * @return 输入到 EditText 的 LiveData
     */
    LiveData<String> getEditInfo();
}
