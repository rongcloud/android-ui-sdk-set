package io.rong.imkit.picture.widget;

import android.app.Dialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import io.rong.imkit.picture.tools.ScreenUtils;


/**
 * Created by lhz on 2020/11/30
 */

public abstract class BaseDialogFragment extends DialogFragment {
    protected View mRootView;
    protected Dialog mDialog;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDialog = getDialog();
        if (mDialog != null) {
            Window dialogWindow = mDialog.getWindow();
            dialogWindow.setBackgroundDrawableResource(getBackgroundDrawableRes());
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            dialogWindow.setLayout((int) (dm.widthPixels * getScreenWidthProportion()), getScreenHeightProportion());
            //设置竖直方向偏移量
            WindowManager.LayoutParams attributes = dialogWindow.getAttributes();
            attributes.gravity = getGravity();
            attributes.x = -ScreenUtils.dip2px(getContext(), getHorizontalMovement());
            attributes.y = ScreenUtils.dip2px(getContext(), getVerticalMovement());
            dialogWindow.setAttributes(attributes);
        }
    }


    protected int getGravity() {
        return Gravity.CENTER;
    }

    /**
     * @return 占屏幕高度的多少
     */
    protected int getScreenHeightProportion() {
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    protected @DrawableRes
    int getBackgroundDrawableRes() {
        return android.R.color.transparent;
    }

    /**
     * 屏幕占比
     *
     * @return 占屏幕宽度的多少
     */
    protected float getScreenWidthProportion() {
        return 0.9f;
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
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(getContentView(), container, false);
        findView();
        initView();
        bindData();
        return mRootView;
    }

    protected abstract void findView();

    protected abstract void initView();

    public abstract void bindData();

    protected abstract @LayoutRes
    int getContentView();
}
