package io.rong.imkit.usermanage.interfaces;

import io.rong.imkit.model.ContactModel;

/**
 * 功能描述: 联系人点击事件监听
 *
 * @author rongcloud
 * @since 5.12.0 请使用 {@link OnActionClickListener}
 */
@Deprecated
public interface OnContactClickListener {

    /**
     * 联系人点击事件
     *
     * @param contactModel 联系人信息
     */
    void onContactClick(ContactModel contactModel);
}
