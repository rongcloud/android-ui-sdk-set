package io.rong.imkit.feature.editmessage;

import android.app.AlertDialog;
import android.content.Context;
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

public class EditMessageFailedDialog extends AlertDialog {

    public EditMessageFailedDialog(@NonNull Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public EditMessageFailedDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    protected EditMessageFailedDialog(
            @NonNull Context context,
            boolean cancelable,
            @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public static EditMessageFailedDialog newInstance(Context context) {
        return new EditMessageFailedDialog(context);
    }

    private static class ControllerParams {
        public boolean isCancelable;
        public CharSequence contentMessage;
        public View.OnClickListener listener;
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
        view.findViewById(R.id.dialog_btn_negative).setVisibility(View.GONE);
        view.findViewById(R.id.dialog_v_btn_separate).setVisibility(View.GONE);
        Button positive = view.findViewById(R.id.dialog_btn_positive);
        TextView content = view.findViewById(R.id.dialog_tv_content);
        TextView title = view.findViewById(R.id.dialog_tv_title);
        positive.setOnClickListener(
                v -> {
                    dismiss();
                    if (params.listener != null) {
                        params.listener.onClick(v);
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

    public EditMessageFailedDialog setContentMessage(CharSequence content) {
        params.contentMessage = content;
        return this;
    }

    public EditMessageFailedDialog setContentMessage(int contentText) {
        params.contentText = contentText;
        return this;
    }

    public EditMessageFailedDialog isCancelable(boolean cancelable) {
        params.isCancelable = cancelable;
        return this;
    }

    public EditMessageFailedDialog setButtonText(int positiveText) {
        params.positiveText = positiveText;
        return this;
    }

    public EditMessageFailedDialog setTitleText(int titleText) {
        params.titleText = titleText;
        return this;
    }

    public EditMessageFailedDialog setOnClickListener(View.OnClickListener listener) {
        params.listener = listener;
        return this;
    }
}
