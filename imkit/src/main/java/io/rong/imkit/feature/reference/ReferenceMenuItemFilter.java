package io.rong.imkit.feature.reference;

import io.rong.imkit.model.UiMessage;

/** 引用菜单入口准入回调。 */
public interface ReferenceMenuItemFilter {
    /**
     * 是否显示当前消息的引用入口。
     *
     * <p>SDK 会先校验引用功能开关、会话类型、消息发送状态、阅后即焚等基础条件；基础条件满足后，再调用该方法。
     *
     * @param uiMessage 当前长按的消息
     * @return true 表示允许显示引用入口
     */
    boolean shouldShowReferenceMenuItem(UiMessage uiMessage);
}
