package io.rong.imkit.feature.publicservice;

import io.rong.imlib.model.Conversation;
import io.rong.imlib.publicservice.model.PublicServiceMenuItem;

public interface IPublicServiceMenuClickListener {
    /**
     * 菜单被点击。
     *
     * @param conversationType 公众服务会话类型。
     * @param targetId         公众服务 id。
     * @param menu             被点击的菜单对象 {@link PublicServiceMenuItem}。
     * @return true 时，设置监听者已经处理的点击事件，sdk 不再处理，否则，sdk 会处理
     */
    boolean onClick(Conversation.ConversationType conversationType, String targetId, PublicServiceMenuItem menu);
}
