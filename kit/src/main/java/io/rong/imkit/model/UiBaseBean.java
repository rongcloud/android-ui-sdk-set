package io.rong.imkit.model;

/**
 * 使用 diffUtils 进行列表局部刷新，在使用同一份内存无法比较变更时，可以继承此 bean 对象
 * 在重新设置属性时，调用 change() 方法，在 diffUtils 比较时判断 isChange 属性
 */
public class UiBaseBean {

    /**
     * 数据更新需要修改此属性
     */
    private boolean isChange;

    public boolean isChange() {
        return isChange;
    }

    public void change() {
        isChange = true;
    }

    public void setChange(boolean change) {
        this.isChange = change;
    }
}
