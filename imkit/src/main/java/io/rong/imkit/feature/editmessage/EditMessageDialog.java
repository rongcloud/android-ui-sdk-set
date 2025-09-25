package io.rong.imkit.feature.editmessage;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.rong.imkit.R;

/** 通用对话框 */
public class EditMessageDialog extends AlertDialog {

    public EditMessageDialog(@NonNull Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public EditMessageDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    protected EditMessageDialog(
            @NonNull Context context,
            boolean cancelable,
            @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public static EditMessageDialog newInstance(Context context) {
        return new EditMessageDialog(context);
    }

    private static class ControllerParams {
        public boolean isCancelable;
        public CharSequence contentMessage;
        public OnClickListener listener;
        public int positiveText;
        public int negativeText;
        public int titleText;
        public int contentText;
    }

    private ControllerParams params = new ControllerParams();

    @Override
    public void onStart() {
        super.onStart();
        View view = View.inflate(getContext(), R.layout.rc_edit_message_dialog, null);
        Button negative = view.findViewById(R.id.dialog_btn_negative);
        Button positive = view.findViewById(R.id.dialog_btn_positive);
        TextView content = view.findViewById(R.id.dialog_tv_content);
        TextView title = view.findViewById(R.id.dialog_tv_title);
        negative.setOnClickListener(
                v -> {
                    dismiss();
                    if (params.listener != null) {
                        params.listener.onNegativeClick(v, null);
                    }
                });
        positive.setOnClickListener(
                v -> {
                    dismiss();
                    if (params.listener != null) {
                        params.listener.onPositiveClick(v, null);
                    }
                });

        if (params.contentText > 0) {
            content.setText(params.contentText);
        } else if (!TextUtils.isEmpty(params.contentMessage)) {
            content.setText(Html.fromHtml(params.contentMessage.toString()));
        }

        if (params.positiveText > 0) {
            positive.setText(params.positiveText);
        }

        if (params.negativeText > 0) {
            negative.setText(params.negativeText);
        }

        if (params.titleText > 0) {
            title.setText(params.titleText);
            title.setVisibility(View.VISIBLE);
        }
        setContentView(view);

        setCancelable(params.isCancelable);
        if (getWindow() != null) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            getWindow().setAttributes(layoutParams);
        }
    }

    public EditMessageDialog setContentMessage(CharSequence content) {
        params.contentMessage = content;
        return this;
    }

    public EditMessageDialog setContentMessage(int contentText) {
        params.contentText = contentText;
        return this;
    }

    public EditMessageDialog isCancelable(boolean cancelable) {
        params.isCancelable = cancelable;
        return this;
    }

    public EditMessageDialog setButtonText(int positiveText, int negativeText) {
        params.positiveText = positiveText;
        params.negativeText = negativeText;
        return this;
    }

    public EditMessageDialog setTitleText(int titleText) {
        params.titleText = titleText;
        return this;
    }

    public EditMessageDialog setOnClickListener(OnClickListener listener) {
        params.listener = listener;
        return this;
    }

    public interface OnClickListener {
        void onPositiveClick(View v, Bundle bundle);

        default void onNegativeClick(View v, Bundle bundle) {}
    }
}
