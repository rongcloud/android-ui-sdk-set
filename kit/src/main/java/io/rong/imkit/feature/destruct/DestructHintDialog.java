package io.rong.imkit.feature.destruct;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.RongExtensionCacheHelper;
import io.rong.imkit.utils.RongUtils;


/**
 * BaseDialogFragment
 * Created by lvhongzhen on 18/8/21.
 */
public class DestructHintDialog extends DialogFragment implements View.OnClickListener {
    private static final String TAG = "DestructHintDialog";
    protected Dialog mDialog;
    protected View mRootView;
    private static boolean isFirstClick = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog_MinWidth);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.rc_dialog_destruct_hint, container, false);
        initView();
        return mRootView;
    }

    protected void initView() {
        TextView mConfirm = mRootView.findViewById(R.id.tv_confirm);
        mConfirm.setOnClickListener(this);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDialog = getDialog();
        if (mDialog != null) {
            Window dialogWindow = mDialog.getWindow();
            if (dialogWindow == null) {
                return;
            }
            dialogWindow.setBackgroundDrawableResource(android.R.color.transparent);
            DisplayMetrics dm = new DisplayMetrics();
            if (getActivity() != null) {
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            }
            dialogWindow.setLayout((int) (dm.widthPixels * getScreenWidthProportion()), ViewGroup.LayoutParams.WRAP_CONTENT);
            //设置竖直方向偏移量
            WindowManager.LayoutParams attributes = dialogWindow.getAttributes();
            attributes.gravity = getGravity();
            attributes.x = -RongUtils.dip2px(getHorizontalMovement());
            attributes.y = RongUtils.dip2px(getVerticalMovement());
            dialogWindow.setAttributes(attributes);
        }
    }

    protected int getGravity() {
        return Gravity.CENTER;
    }


    /**
     * 屏幕占比
     *
     * @return 占屏幕宽度的多少
     */
    protected float getScreenWidthProportion() {
        return 0.75f;
    }

    /**
     * 设置竖直方向偏移量
     *
     * @return 返回负值是向上偏移 正值是向下偏移 单位dp 例如：想向上偏移20dp，return -20 即可，无需转换
     */
    protected float getVerticalMovement() {
        return 0;
    }

    /**
     * 设置竖直方向偏移量
     *
     * @return 返回负值是向左偏移 正值是向右偏移 单位dp 例如：想向左偏移20dp，return -20 即可，无需转换
     */
    protected float getHorizontalMovement() {
        return 0;
    }

    public void show(FragmentManager manager) {
        try {
            show(manager, "");
            this.setCancelable(false);
            if (mDialog != null) {
                mDialog.setCanceledOnTouchOutside(false);
            }
        } catch (IllegalStateException e) {
            RLog.e(TAG, "show", e);
        }

    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.tv_confirm) {
            RongExtensionCacheHelper.recordDestructClickEvent(v.getContext());
            hideDialog();
        }
    }

    private void hideDialog() {
        dismissAllowingStateLoss();
    }
}
