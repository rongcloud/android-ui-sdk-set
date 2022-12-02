package io.rong.imkit.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.common.RLog;

/** RecyclerView官方的BUG，继承封装LinearLayoutManager类，重写onLayoutChildren()方法，try-catch捕获该异常 */
public class FixedLinearLayoutManager extends LinearLayoutManager {

    private static final String TAG = FixedLinearLayoutManager.class.getSimpleName();

    public FixedLinearLayoutManager(Context context) {
        super(context);
    }

    public FixedLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public FixedLinearLayoutManager(
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
