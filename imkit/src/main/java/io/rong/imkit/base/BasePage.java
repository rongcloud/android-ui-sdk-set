package io.rong.imkit.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;

/**
 * 页面基类
 *
 * @author rongcloud
 * @since 5.10.4
 */
public interface BasePage {

    /**
     * 创建模块后调用，以创建要在片段或活动中使用的视图。 在此方法中，模块使用属于组件的组件来构建视图。
     *
     * @param context 上下文
     * @param inflater 可用于扩充模块中任何视图的 LayoutInflate 对象
     * @param container 父布局
     * @param args 实例化模块时提供的参数（非必须）
     * @return 返回 UI 的视图。
     */
    @NonNull
    View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @NonNull Bundle args);
}
