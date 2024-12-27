package io.rong.imkit.usermanage.interfaces;

/**
 * 分页数据加载接口
 *
 * @author rongcloud
 * @since 5.12.0
 */
public interface OnPagedDataLoader {

    /**
     * 加载上一页数据
     *
     * @param listener 加载完成数据监听
     */
    default void loadPrevious(OnDataChangeListener<Boolean> listener) {}

    /**
     * 加载下一页数据
     *
     * @param listener 加载完成数据监听
     */
    void loadNext(OnDataChangeListener<Boolean> listener);

    /**
     * 是否有下一页数据
     *
     * @return 是否有下一页数据
     */
    boolean hasNext();

    /**
     * 是否有上一页数据
     *
     * @return 是否有上一页数据
     */
    default boolean hasPrevious() {
        return false;
    }
}
