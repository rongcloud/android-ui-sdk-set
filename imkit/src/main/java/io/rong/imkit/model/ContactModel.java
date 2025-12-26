package io.rong.imkit.model;

/**
 * 功能描述:
 *
 * <p>创建时间: 2024/8/23
 *
 * @author haogaohui
 * @since 1.0
 */
public class ContactModel<T> {

    private CheckType checkType;
    private ItemType itemType;
    private T bean;
    private Object extra;

    public static <T> ContactModel<T> obtain(T bean, ItemType itemType) {
        return new ContactModel<>(bean, itemType, CheckType.NONE);
    }

    public static <T> ContactModel<T> obtain(T bean, ItemType itemType, CheckType checkType) {
        return new ContactModel<>(bean, itemType, checkType);
    }

    protected ContactModel(T bean, ItemType itemType, CheckType checkType) {
        this.bean = bean;
        this.itemType = itemType;
        this.checkType = checkType;
    }

    public void putExtra(Object value) {
        this.extra = value;
    }

    @SuppressWarnings("unchecked")
    public <E> E getExtra() {
        return (E) extra;
    }

    public T getBean() {
        return bean;
    }

    public void setBean(T bean) {
        this.bean = bean;
    }

    public ItemType getContactType() {
        return itemType;
    }

    public CheckType getCheckType() {
        return checkType;
    }

    public void setCheckType(CheckType checkType) {
        this.checkType = checkType;
    }

    public enum CheckType {
        NONE,
        CHECKED,
        UNCHECKED,
        DISABLE
    }

    public enum ItemType {
        TITLE,
        CONTENT,
    }
}
