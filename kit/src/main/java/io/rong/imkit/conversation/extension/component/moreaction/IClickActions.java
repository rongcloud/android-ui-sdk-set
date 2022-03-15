package io.rong.imkit.conversation.extension.component.moreaction;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;
import io.rong.imkit.model.UiMessage;

/** /~chinese Created by zwfang on 2018/3/29. */
public interface IClickActions {

    /**
     * /~chinese 获取点击按钮的图标
     *
     * @param context 上下文
     * @return 图片的Drawable, 如需高亮或者置灰，则返回类型为selector, 分别显示enable或者disable状态下的drawable
     */

    /**
     * /~english Get the icon of the click button
     *
     * @param context Context
     * @return If the Drawable of the image shall be highlighted or grayed out, the return type is
     *     selector to display the drawable in enable or disable status, respectively
     */
    Drawable obtainDrawable(Context context);

    /**
     * /~chinese 图标按钮点击事件
     *
     * @param curFragment 当前 Fragment, 请注意不要持有该 fragment, 否则容易引起内存泄露。
     */

    /**
     * /~english Icon button click event
     *
     * @param curFragment Please be careful not to hold the fragment, in the current Fragment,
     *     otherwise it will easily cause memory leakage.
     */
    void onClick(Fragment curFragment);

    /**
     * /~chinese
     *
     * @param message 消息
     * @return 返回true，表示显示
     */

    /**
     * /~english
     *
     * @param message Message
     * @return Return true indicates to show
     */
    boolean filter(UiMessage message);
}
