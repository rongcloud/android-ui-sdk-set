package io.rong.imkit.feature.expose;

import android.os.Handler;
import android.os.Looper;
import io.rong.common.rlog.RLog;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.RongCoreClient;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 批量提交管理器 用于将高频调用的操作进行批量处理，减少网络请求次数
 *
 * <p>功能特性： 1. 防抖动：在指定延迟时间内的多次调用会被合并成一次提交 2. 状态机管理：使用清晰的状态转换避免竞态条件 3. 线程安全：使用统一的状态锁确保多线程安全 4.
 * 泛型支持：支持不同类型的数据批量处理 5. 顺序保证：使用 LinkedHashSet 保持任务的插入顺序，确保先提交的任务先执行 6. 去重保证：自动去除重复任务（基于 equals 和
 * hashCode） 7. 连接状态感知：根据连接状态自动暂停/恢复任务处理
 *
 * <p>状态机： IDLE ⇄ ACTIVE
 *
 * <p>状态说明： - IDLE: 空闲状态，没有待处理数据，没有安排任务 - ACTIVE: 活跃状态，有待处理数据或正在处理中
 *
 * @param <T> 要批量处理的数据类型（需要正确实现 equals 和 hashCode 方法）
 * @since 5.30.0
 */
public abstract class ExposeBatchSubmitManager<T> {
    private static final String TAG = "BatchSubmitManager";
    private static final int DEFAULT_DELAY_MS = 100; // 默认100ms延迟
    private static final int MAX_BATCH_SIZE = 100; // 单次最多提交100条

    /** 批量提交状态枚举 */
    private enum SubmitState {
        IDLE, // 空闲状态：没有待处理数据，没有安排任务
        ACTIVE // 活跃状态：有待处理数据或正在处理中
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Object mStateLock = new Object(); // 状态锁

    // 批量处理相关字段
    private final Set<T> mPendingItems = new LinkedHashSet<>(); // 待提交的数据集合（保持插入顺序）
    private SubmitState mCurrentState = SubmitState.IDLE; // 当前状态
    private boolean mIsConnected = true; // 连接状态标志
    private int mDelayMs = DEFAULT_DELAY_MS;
    private Runnable mBatchSubmitRunnable; // 批量提交任务
    private final IRongCoreListener.ConnectionStatusListener connectionStatusListener =
            new IRongCoreListener.ConnectionStatusListener() {
                @Override
                public void onChanged(ConnectionStatus status) {
                    synchronized (mStateLock) {
                        mIsConnected = status.equals(ConnectionStatus.CONNECTED);
                        RLog.d(
                                TAG,
                                "IsConnected "
                                        + mIsConnected
                                        + ", pending items: "
                                        + mPendingItems.size());
                        if (mIsConnected) {
                            // 连接状态，如果有待处理数据且当前是空闲状态，则启动延迟任务
                            if (!mPendingItems.isEmpty() && mCurrentState == SubmitState.IDLE) {
                                scheduleDelayedSubmit();
                                mCurrentState = SubmitState.ACTIVE;
                            }
                        }
                    }
                }
            };

    /** 批量提交结果回调接口 */
    public interface BatchResultCallback {
        /**
         * 提交结果
         *
         * @param code 提交结果错误码：SUCCESS代表成功，其他代表失败
         * @param refillData 是否需要回填数据，如某些失败场景需要回填重试。
         */
        void onResult(IRongCoreEnum.CoreErrorCode code, boolean refillData);
    }

    /** 构造函数 */
    protected ExposeBatchSubmitManager() {
        mIsConnected =
                IRongCoreListener.ConnectionStatusListener.ConnectionStatus.CONNECTED.equals(
                        RongCoreClient.getInstance().getCurrentConnectionStatus());
        RongCoreClient.addConnectionStatusListener(connectionStatusListener);
    }

    /**
     * 触发批量提交
     *
     * @param items 数据
     * @param callback 执行完业务逻辑后，调用 callback#onResult通知管理器
     */
    abstract void onBatchSubmit(List<T> items, BatchResultCallback callback);

    /**
     * 添加数据到批量处理队列
     *
     * @param item 要添加的数据
     */
    public void addSubmitTask(T item) {
        if (item == null) {
            return;
        }
        synchronized (mStateLock) {
            // 添加到待处理队列（LinkedHashSet保持插入顺序并通过equals和hashCode去重）
            boolean added = mPendingItems.add(item);
            if (!added) {
                RLog.d(TAG, "Item already exists in pending queue, skipped");
                return;
            }

            RLog.d(TAG, "Added item to pending queue, total: " + mPendingItems.size());

            // 只有在连接状态且空闲状态时才需要安排新的延迟任务
            if (mIsConnected && mCurrentState == SubmitState.IDLE) {
                scheduleDelayedSubmit();
                mCurrentState = SubmitState.ACTIVE;
            }
            // 非连接状态下只添加到队列，不启动延迟任务
        }
    }

    /** 安排延迟提交任务 */
    private void scheduleDelayedSubmit() {
        // 取消之前的任务（如果存在）
        if (mBatchSubmitRunnable != null) {
            mHandler.removeCallbacks(mBatchSubmitRunnable);
        }
        // 创建新的延迟任务
        mBatchSubmitRunnable = this::executeBatchSubmit;
        // 延迟执行
        mHandler.postDelayed(mBatchSubmitRunnable, mDelayMs);
    }

    /** 执行批量提交 */
    private void executeBatchSubmit() {
        final List<T> itemsToSubmit = new ArrayList<>();
        synchronized (mStateLock) {
            if (mPendingItems.isEmpty()) {
                // 没有待处理数据，回到空闲状态
                mCurrentState = SubmitState.IDLE;
                return;
            }

            // 单次最多提交100条，超过的部分留在队列中
            int count = 0;
            for (T item : mPendingItems) {
                if (count >= MAX_BATCH_SIZE) {
                    break;
                }
                itemsToSubmit.add(item);
                count++;
            }

            // 从待处理队列中移除本次要提交的数据
            mPendingItems.removeAll(itemsToSubmit);

            RLog.d(
                    TAG,
                    "Preparing to submit "
                            + itemsToSubmit.size()
                            + " items, remaining: "
                            + mPendingItems.size());
        }

        if (!itemsToSubmit.isEmpty()) {
            try {
                onBatchSubmit(
                        itemsToSubmit,
                        new BatchResultCallback() {
                            @Override
                            public void onResult(
                                    IRongCoreEnum.CoreErrorCode code, boolean refillData) {
                                RLog.d(TAG, "Batch submit result: " + code);
                                // 提交失败，将数据回填到待处理队列头部（保证失败重试的数据优先处理）
                                if (refillData) {
                                    refillData(itemsToSubmit);
                                }
                                onBatchSubmitComplete();
                            }
                        });
            } catch (Exception e) {
                RLog.e(TAG, "Exception during batch submit: " + e.getMessage());
                // 发生异常，将数据回填到待处理队列头部
                refillData(itemsToSubmit);
                onBatchSubmitComplete();
            }
        } else {
            onBatchSubmitComplete();
        }
    }

    private void refillData(List<T> itemsToSubmit) {
        synchronized (mStateLock) {
            Set<T> newPendingItems = new LinkedHashSet<>(itemsToSubmit);
            newPendingItems.addAll(mPendingItems);
            mPendingItems.clear();
            mPendingItems.addAll(newPendingItems);
            RLog.w(
                    TAG,
                    "refillData "
                            + itemsToSubmit.size()
                            + " items to queue head, total pending: "
                            + mPendingItems.size());
        }
    }

    /** 批量提交完成后的处理 */
    private void onBatchSubmitComplete() {
        synchronized (mStateLock) {
            if (mPendingItems.isEmpty()) {
                // 没有新数据，回到空闲状态
                mCurrentState = SubmitState.IDLE;
            } else if (mIsConnected) {
                // 有新数据且处于连接状态，安排下一轮提交，保持ACTIVE状态
                scheduleDelayedSubmit();
            } else {
                // 有新数据但未连接，回到空闲状态，等待连接恢复后重新启动
                mCurrentState = SubmitState.IDLE;
                RLog.d(TAG, "Has pending items but not connected, waiting for connection restore");
            }
        }
    }

    /**
     * 释放外部资源并清理内部状态 停止接收连接状态通知，取消待执行的任务，清空等待队列 适用场景：需要完全停止批量提交功能时调用
     *
     * <p>注意：调用此方法后，BatchSubmitManager 将立即停止所有任务处理， 所有待处理的数据将被清除，不会再继续执行
     */
    public void release() {
        // 移除连接状态监听器，停止接收新的连接状态通知
        RongCoreClient.removeConnectionStatusListener(connectionStatusListener);

        // 如果有网，则不清理缓存队列
        if (mIsConnected) {
            RLog.d(TAG, "BatchSubmitManager mIsConnected not released");
            return;
        }

        // 取消待执行的延迟任务
        if (mBatchSubmitRunnable != null) {
            mHandler.removeCallbacks(mBatchSubmitRunnable);
        }

        // 清空待处理队列
        synchronized (mStateLock) {
            mPendingItems.clear();
        }

        RLog.d(TAG, "BatchSubmitManager released");
    }
}
