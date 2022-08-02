package io.rong.sticker.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.rong.sticker.R;
import io.rong.sticker.model.Sticker;

/**
 * Created by luoyanlong on 2018/08/15.
 */
public class StickerGridItemView extends LinearLayout {

    private static final int ROW_NUM = 2; // 行数

    private Sticker sticker;
    private ImageView imageView;
    private TextView textView;

    public StickerGridItemView(Context context) {
        super(context);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getParent() != null) {
            GridView parent = (GridView) getParent();
            int width = parent.getWidth() / parent.getNumColumns();
            int height = parent.getHeight() / ROW_NUM;
            setMeasuredDimension(width, height);
        }
    }

    private void init() {
        inflate(getContext(), R.layout.rc_sticker, this);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        imageView = findViewById(R.id.iv);
        textView = findViewById(R.id.tv);
    }

    public Sticker getSticker() {
        return sticker;
    }

    public void setSticker(Sticker sticker) {
        this.sticker = sticker;
        updateView();
    }

    private void updateView() {
        Bitmap bitmap = BitmapFactory.decodeFile(sticker.getLocalThumbUrl());
        imageView.setImageBitmap(bitmap);
        textView.setText(sticker.getDigest());
    }
}
