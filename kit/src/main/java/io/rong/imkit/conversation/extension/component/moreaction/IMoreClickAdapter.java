package io.rong.imkit.conversation.extension.component.moreaction;

import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import java.util.List;

/** Created by zwfang on 2018/4/8. */
public interface IMoreClickAdapter {
    /**
     * /~chinese 显示底部更多点击事件布局
     *
     * @param viewGroup 父布局
     * @param fragment 当前fragment
     * @param actions 点击事件
     */

    /**
     * /~english Show more click event layout at the bottom
     *
     * @param viewGroup Parent layout
     * @param fragment Current fragment
     * @param actions Click event
     */
    void bindView(ViewGroup viewGroup, Fragment fragment, List<IClickActions> actions);

    /** /~chinese 隐藏底部点击事件 */

    /** /~english Hide the bottom click event */
    void hideMoreActionLayout();

    /**
     * /~chinese 底部点击事件是否高亮
     *
     * @param enable 是否高亮
     */

    /**
     * /~english Whether the click event at the bottom is highlighted
     *
     * @param enable Whether it is highlighted
     */
    void setMoreActionEnable(boolean enable);

    /**
     * /~chinese 点击按钮是否可见
     *
     * @return true 可见状态，false 隐藏状态
     */

    /**
     * /~english Whether the click of the button is visible
     *
     * @return True visible state, false hidden state
     */
    boolean isMoreActionShown();
}
