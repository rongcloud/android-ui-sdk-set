package io.rong.imkit.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import io.rong.imkit.R;

public class TipLoadingDialog extends Dialog {
    private TextView textView;

    public TipLoadingDialog(Context context) {
        super(context, R.style.Picture_Theme_AlertDialog);
        setCancelable(true);
        setCanceledOnTouchOutside(false);
        Window window = getWindow();
        window.setWindowAnimations(R.style.PictureThemeDialogWindowStyle);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_tip_alert_dialog);
        // 初始化视图
        textView = findViewById(R.id.tv_tips);
        setCancelable(false);
    }

    // 设置加载提示文字
    public void setTips(String text) {
        if (textView != null) {
            textView.setText(text);
        }
    }
}
