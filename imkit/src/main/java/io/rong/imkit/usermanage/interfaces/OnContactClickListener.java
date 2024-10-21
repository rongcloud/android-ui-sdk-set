package io.rong.imkit.usermanage.interfaces;

import io.rong.imkit.model.ContactModel;

/**
 * 功能描述: 联系人点击事件监听
 *
 * <p>创建时间: 2024/8/23
 *
 * @since 5.10.4
 * @author rongcloud
 */
public interface OnContactClickListener {

    /**
     * 联系人点击事件
     *
     * @param contactModel 联系人信息
     */
    void onContactClick(ContactModel contactModel);
}
