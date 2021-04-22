package io.rong.imkit.conversation.extension.component.moreaction;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.fragment.app.Fragment;
import io.rong.imkit.model.UiMessage;

/**
 * Created by zwfang on 2018/3/29.
 */

public interface IClickActions {

    /**
     * 获取点击按钮的图标
     *
     * @param context 上下文
     * @return 图片的Drawable, 如需高亮或者置灰，则返回类型为selector, 分别显示enable或者disable状态下的drawable
     */
    Drawable obtainDrawable(Context context);

    /**
     * 图标按钮点击事件
     *
     * @param curFragment 当前 Fragment, 请注意不要持有该 fragment, 否则容易引起内存泄露。
     */
    void onClick(Fragment curFragment);

    /**
     * @param message 消息
     * @return 返回true，表示显示
     */
    boolean filter(UiMessage message);
}
