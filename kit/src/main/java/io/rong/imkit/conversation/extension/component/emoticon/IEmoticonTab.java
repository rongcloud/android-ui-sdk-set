package io.rong.imkit.conversation.extension.component.emoticon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import androidx.lifecycle.LiveData;

public interface IEmoticonTab {

    /**
     * /~chinese 构造 tab 的小图标，用于显示在 tab bar中。
     *
     * @param context 应用上下文。
     * @return 图标的 drawable，不能为 null。
     */

    /**
     * /~english Construct a small icon for tab to display in tab bar.
     *
     * @param context Application context.
     * @return The drawable of the icon cannot be null
     */
    Drawable obtainTabDrawable(Context context);

    /**
     * /~chinese 构造 table 页面。
     *
     * @param context 应用上下文。
     * @return 构造后的 table view，不能为 null。
     */

    /**
     * /~english Construct the table page
     *
     * @param context Application context.
     * @return The constructed table view cannot be null
     */
    View obtainTabPager(Context context, ViewGroup parent);

    /**
     * /~chinese 表情面板左右滑动时，回调此方法。
     *
     * @param position 当前 table 的位置。
     */

    /**
     * /~english Call back this method when the emoji panel slides left and right.
     *
     * @param position The location of the current table.
     */
    void onTableSelected(int position);

    /**
     * /~chinese 返回 tab 页对应输入框的更新信息
     *
     * @return 输入到 EditText 的 LiveData
     */

    /**
     * /~english Return the update information of the input box corresponding to the tab page
     *
     * @return LiveData input to EditText
     */
    LiveData<String> getEditInfo();
}
