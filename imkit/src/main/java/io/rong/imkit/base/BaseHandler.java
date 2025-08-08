package io.rong.imkit.base;

/**
 * 业务逻辑处理-包含生命周期
 *
 * @author rongcloud
 * @since 5.10.4
 */
public abstract class BaseHandler {

    private volatile boolean isAlive = true;

    /** 结束 */
    public void stop() {
        this.isAlive = false;
    }

    /**
     * 处理器 是否存活
     *
     * @return true:存活
     */
    protected final boolean isAlive() {
        return isAlive;
    }
}
