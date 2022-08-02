package io.rong.imkit.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import java.util.List;

import io.rong.imkit.conversation.extension.component.moreaction.IClickActions;

/**
 * Created by zwfang on 2018/3/29.
 */

public class MoreActionLayout extends ViewGroup {

    private Context context;
    private Fragment fragment;

    public MoreActionLayout(Context context) {
        super(context);
        this.context = context;
    }

    public MoreActionLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int totalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int childWidth = 0;
        if (getChildCount() > 0) {
            childWidth = totalWidth / getChildCount();
        }
        for (int i = 0; i < getChildCount(); ++i) {
            int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
            View childView = getChildAt(i);
            childView.measure(childWidthSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int total = getMeasuredWidth();
        int height = getMeasuredHeight();
        int width = 0;
        if (getChildCount() > 0) {
            width = total / getChildCount();
        }
        for (int i = 0; i < getChildCount(); ++i) {
            View child = getChildAt(i);
            child.layout(i * width, 0, (i + 1) * width, height);
        }
    }

    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }

    public void addActions(final List<IClickActions> actions) {
        if (actions != null && actions.size() > 0) {
            for (int i = 0; i < actions.size(); ++i) {
                final IClickActions action = actions.get(i);
                ClickImageView view = new ClickImageView(context);
                view.setImageDrawable(actions.get(i).obtainDrawable(context));
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        action.onClick(fragment);
                    }
                });
                addView(view, i);
            }
            invalidate();
        }
    }

    public void refreshView(boolean enable) {
        for (int i = 0; i < getChildCount(); ++i) {
            ClickImageView view = (ClickImageView) getChildAt(i);
            view.setEnable(enable);
        }
    }
}
