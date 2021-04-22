package io.rong.imkit.conversation.extension.component.moreaction;

import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import java.util.List;

/**
 * Created by zwfang on 2018/4/8.
 */

public interface IMoreClickAdapter {
    /**
     * 显示底部更多点击事件布局
     *
     * @param viewGroup 父布局
     * @param fragment  当前fragment
     * @param actions   点击事件
     */
    void bindView(ViewGroup viewGroup, Fragment fragment, List<IClickActions> actions);

    /**
     * 隐藏底部点击事件
     */
    void hideMoreActionLayout();

    /**
     * 底部点击事件是否高亮
     *
     * @param enable 是否高亮
     */
    void setMoreActionEnable(boolean enable);

    /**
     * 点击按钮是否可见
     *
     * @return true 可见状态，false 隐藏状态
     */
    boolean isMoreActionShown();
}
