package io.rong.imkit.conversation.extension.component.moreaction;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import io.rong.imkit.R;

/**
 * Created by zwfang on 2018/3/30.
 */

public class ClickImageView extends RelativeLayout {

    private ImageView imageView;

    public ClickImageView(Context context) {
        super(context);
        initView(context);
    }

    public ClickImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        imageView = new ImageView(context);
        int width = context.getResources().getDimensionPixelSize(R.dimen.rc_ext_more_imgage_width);
        int height = context.getResources().getDimensionPixelOffset(R.dimen.rc_ext_more_imgage_height);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(CENTER_IN_PARENT);
        addView(imageView, params);
    }

    public void setImageDrawable(Drawable drawable) {
        imageView.setImageDrawable(drawable);
    }

    public void setEnable(boolean enable) {
        imageView.setEnabled(enable);
    }
}
