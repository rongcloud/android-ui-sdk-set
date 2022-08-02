package io.rong.imkit.widget.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import io.rong.imkit.R;


public class PromptPopupDialog extends AlertDialog {

    private Context mContext;
    private OnPromptButtonClickedListener mPromptButtonClickedListener;
    private String mTitle;
    private String mPositiveButton;
    private String mMessage;
    private int mLayoutResId;

    public static PromptPopupDialog newInstance(final Context context, String title, String message) {
        return new PromptPopupDialog(context, title, message);
    }

    public static PromptPopupDialog newInstance(final Context context, String message) {
        return new PromptPopupDialog(context, message);
    }

    public static PromptPopupDialog newInstance(final Context context, String title, String message, String positiveButton) {
        return new PromptPopupDialog(context, title, message, positiveButton);
    }

    public PromptPopupDialog(final Context context, String title, String message, String positiveButton) {
        this(context, title, message);
        mPositiveButton = positiveButton;
    }

    public PromptPopupDialog(final Context context, String title, String message) {
        super(context);
        mLayoutResId = R.layout.rc_dialog_popup_prompt;
        mContext = context;
        mTitle = title;
        mMessage = message;
    }

    public PromptPopupDialog(final Context context, String message) {
        super(context);
        mContext = context;
        mMessage = message;
    }

    @Override
    protected void onStart() {
        super.onStart();
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(mLayoutResId, null);
        TextView txtViewTitle = view.findViewById(R.id.popup_dialog_title);
        TextView txtViewMessage = view.findViewById(R.id.popup_dialog_message);
        TextView txtViewOK = view.findViewById(R.id.popup_dialog_button_ok);
        TextView txtViewCancel = view.findViewById(R.id.popup_dialog_button_cancel);
        txtViewOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPromptButtonClickedListener != null) {
                    mPromptButtonClickedListener.onPositiveButtonClicked();
                }
                dismiss();
            }
        });
        txtViewCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        if (!TextUtils.isEmpty(mTitle)) {
            txtViewTitle.setText(mTitle);
            txtViewTitle.setVisibility(View.VISIBLE);
        }
        if (!TextUtils.isEmpty(mPositiveButton)) {
            txtViewOK.setText(mPositiveButton);
        }

        txtViewMessage.setText(mMessage);

        setContentView(view);
        if (getWindow() == null) {
            return;
        }
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.width = gePopupWidth();
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(layoutParams);
    }

    public PromptPopupDialog setPromptButtonClickedListener(OnPromptButtonClickedListener buttonClickedListener) {
        this.mPromptButtonClickedListener = buttonClickedListener;
        return this;
    }

    public PromptPopupDialog setLayoutRes(int resId) {
        this.mLayoutResId = resId;
        return this;
    }

    public interface OnPromptButtonClickedListener {
        void onPositiveButtonClicked();
    }

    private int gePopupWidth() {
        int distanceToBorder = (int) mContext.getResources().getDimension(R.dimen.rc_dialog_margin_to_edge);
        return getScreenWidth() - 2 * (distanceToBorder);
    }

    private int getScreenWidth() {
        return ((WindowManager) (mContext.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getWidth();
    }

}
