package io.rong.sticker.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import com.felipecsl.gifimageview.library.GifImageView;
import io.rong.imkit.utils.RongUtils;
import io.rong.sticker.R;
import io.rong.sticker.businesslogic.GifImageLoader;
import io.rong.sticker.model.Sticker;

/** Created by luoyanlong on 2018/08/20. */
public class StickerPopupWindow extends PopupWindow {

    private static StickerPopupWindow instance;

    public static synchronized StickerPopupWindow getInstance(Context context) {
        if (instance == null) {
            instance = new StickerPopupWindow(context);
        }
        return instance;
    }

    StickerPopupWindow(Context context) {
        super();
        View view = LayoutInflater.from(context).inflate(R.layout.rc_sticker_popup, null);
        setContentView(view);
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void show(final View view, Sticker sticker, Background bg) {
        final View contentView = getContentView();
        View background = contentView.findViewById(R.id.bg);
        background.setBackgroundResource(bg.resId);
        final GifImageView gifImageView = contentView.findViewById(R.id.gif_view);
        GifImageLoader.getInstance()
                .obtain(
                        sticker,
                        new GifImageLoader.SimpleCallback() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                gifImageView.setBytes(bytes);
                                gifImageView.startAnimation();
                            }

                            @Override
                            public void onFail() {
                                gifImageView.setBytes(null);
                            }
                        });
        int xoff, yoff;
        if (contentView.getHeight() == 0) {
            int widthMeasureSpec =
                    View.MeasureSpec.makeMeasureSpec(
                            RongUtils.getScreenWidth(), View.MeasureSpec.AT_MOST);
            int heightMeasureSpec =
                    View.MeasureSpec.makeMeasureSpec(
                            RongUtils.getScreenHeight(), View.MeasureSpec.AT_MOST);
            contentView.measure(widthMeasureSpec, heightMeasureSpec);
            xoff = getXoff(view, bg, contentView.getMeasuredWidth());
            yoff = -(view.getHeight() + contentView.getMeasuredHeight());
        } else {
            xoff = getXoff(view, bg, contentView.getWidth());
            yoff = -(view.getHeight() + contentView.getHeight());
        }
        showAsDropDown(view, xoff, yoff);
    }

    private int getXoff(View view, Background bg, int bgWidth) {
        switch (bg) {
            case MIDDLE:
                return (view.getWidth() - bgWidth) / 2;
            case LEFT:
                return (view.getWidth() - bgWidth) / 2
                        + view.getContext()
                                .getResources()
                                .getDimensionPixelSize(R.dimen.popup_window_xoff);
            case RIGHT:
                return (view.getWidth() - bgWidth) / 2
                        - view.getContext()
                                .getResources()
                                .getDimensionPixelSize(R.dimen.popup_window_xoff);
        }
        return 0;
    }

    public enum Background {
        LEFT(R.drawable.rc_zuoyulan),
        MIDDLE(R.drawable.rc_zhongyulan),
        RIGHT(R.drawable.rc_youyulan);

        private final int resId;

        Background(int resId) {
            this.resId = resId;
        }
    }
}
