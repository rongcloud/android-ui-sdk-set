/*
 * Copyright (c) 2018.
 * Author：Zhao
 * Email：joeyzhao1005@gmail.com
 */

package io.rong.imkit.picture.decoration;

import android.content.Context;
import android.util.AttributeSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.common.rlog.RLog;

/**
 * Created by luck on 2017/12/4.
 *
 * <p>RecyclerView Bug：IndexOutOfBoundsException: Inconsistency detected. Invalid view holder
 * adapter的解决方案 只是把这个异常捕获了，不让他奔溃了，这个问题的终极解决方案还是得让google去修复。
 */
public class WrapContentLinearLayoutManager extends LinearLayoutManager {
    private static final String TAG = WrapContentLinearLayoutManager.class.getSimpleName();

    public WrapContentLinearLayoutManager(Context context) {
        super(context);
    }

    public WrapContentLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public WrapContentLinearLayoutManager(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException e) {
            RLog.e(TAG, e.getMessage());
        }
    }
}
