package io.rong.imkit.widget;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import io.rong.imkit.R;

public class SimpleInputDialog extends DialogFragment implements View.OnClickListener {
    private EditText inputEt;
    private TextView confirmTv;
    private TextView cancelTv;
    private TextView titleTv;

    private String hintText;
    private String confirmText;
    private String cancelText;
    private String titleText;
    private InputDialogListener inputDialogListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // 使用 Activity 的 Context，确保能够访问应用的主题属性
        return new NoLeakDialog(requireContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        // 透明化背景
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            // 设置透明背景
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // 使用传入的 inflater 以确保正确应用主题
        View contentView =
                inflater.inflate(R.layout.rc_common_dialog_simple_input, container, false);

        // 初始化视图
        inputEt = contentView.findViewById(R.id.common_et_dialog_input);
        confirmTv = contentView.findViewById(R.id.common_tv_dialog_confirm);
        cancelTv = contentView.findViewById(R.id.common_tv_dialog_cancel);
        titleTv = contentView.findViewById(R.id.common_tv_title);

        // 设置点击监听
        confirmTv.setOnClickListener(this);
        cancelTv.setOnClickListener(this);

        // 设置提示文本
        if (!TextUtils.isEmpty(hintText)) {
            inputEt.setHint(hintText);
        }

        // 设置确认按钮文本
        if (!TextUtils.isEmpty(confirmText)) {
            confirmTv.setText(confirmText);
        }

        // 设置取消按钮文本
        if (!TextUtils.isEmpty(cancelText)) {
            cancelTv.setText(cancelText);
        }

        // 设置标题文本
        if (!TextUtils.isEmpty(titleText)) {
            titleTv.setText(titleText);
        }

        // 请求无标题栏
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            // 去除系统自带的 margin
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // 设置 dialog 在界面中的属性
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    public void setInputHint(String hint) {
        hintText = hint;
    }

    public void setConfirmText(String confirmText) {
        this.confirmText = confirmText;
    }

    public void setCancelText(String cancelText) {
        this.cancelText = cancelText;
    }

    public void setTitleText(String titleText) {
        this.titleText = titleText;
    }

    public void setInputDialogListener(InputDialogListener listener) {
        this.inputDialogListener = listener;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.common_tv_dialog_confirm) {
            boolean isClose = true;
            if (inputDialogListener != null) {
                isClose = inputDialogListener.onConfirmClicked(inputEt);
            }
            if (isClose) {
                dismiss();
            }
        } else if (id == R.id.common_tv_dialog_cancel) {
            dismiss();
        }
    }

    public interface InputDialogListener {
        /**
         * 当点击确认时回调输入内容
         *
         * @return 返回 false 时，不关闭对话框，返回 true时关闭对话框
         */
        boolean onConfirmClicked(EditText input);
    }
}
