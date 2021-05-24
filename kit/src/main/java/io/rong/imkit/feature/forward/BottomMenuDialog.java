package io.rong.imkit.feature.forward;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;

import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.picture.widget.BaseDialogFragment;

/**
 * 转发功能点击时的底部对话框
 *
 * @author jenny_zhou
 */

public class BottomMenuDialog extends BaseDialogFragment implements View.OnClickListener {

    Button step;
    Button combine;
    Button cancel;
    private View.OnClickListener confirmListener;
    private View.OnClickListener middleListener;
    private View.OnClickListener cancelListener;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDialog.setCanceledOnTouchOutside(true);
    }

    @Override
    protected int getGravity() {
        return Gravity.BOTTOM;
    }

    @Override
    protected float getScreenWidthProportion() {
        return 1f;
    }

    @Override
    protected void findView() {
        step = mRootView.findViewById(R.id.bt_by_step);
        combine = mRootView.findViewById(R.id.bt_combine);
        cancel = mRootView.findViewById(R.id.bt_cancel);
    }

    @Override
    protected void initView() {
        cancel.setOnClickListener(this);
        step.setOnClickListener(this);
        combine.setOnClickListener(this);
        if (!RongConfigCenter.conversationConfig().rc_enable_send_combine_message) {
            combine.setVisibility(View.GONE);
        }
    }

    @Override
    public void bindData() {
        //no data need to bind
    }

    @Override
    protected int getContentView() {
        return R.layout.rc_dialog_bottom;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        View.OnClickListener listener = null;
        if (i == R.id.bt_by_step) {
            listener = confirmListener;
        } else if (i == R.id.bt_combine) {
            listener = middleListener;
        } else if (i == R.id.bt_cancel) {
            listener = cancelListener;
        }
        if (listener != null) {
            listener.onClick(v);
        }

    }

    public View.OnClickListener getConfirmListener() {
        return confirmListener;
    }

    void setConfirmListener(View.OnClickListener confirmListener) {
        this.confirmListener = confirmListener;
    }

    public View.OnClickListener getCancelListener() {
        return cancelListener;
    }

    void setCancelListener(View.OnClickListener cancelListener) {
        this.cancelListener = cancelListener;
    }

    public View.OnClickListener getMiddleListener() {
        return middleListener;
    }

    void setMiddleListener(View.OnClickListener middleListener) {
        this.middleListener = middleListener;
    }
}
