package io.rong.imkit.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import io.rong.imkit.R;

/** 右侧的字母索引View */
public class SideBar extends View {

    // 触摸事件
    private OnTouchingLetterChangedListener onTouchingLetterChangedListener;

    // 26个字母
    public static final String[] DEFAULT_B = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
        "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    };
    private String[] b = DEFAULT_B;
    // 选中
    private int choose = -1;

    private Paint paint = new Paint();

    private TextView mTextDialog;

    // 单位转换工具，将 dp 转换为 px
    private int singleHeightPx;
    // 单个字符的高度，单位为dp
    private static final int SINGLE_HEIGHT_DP = 25;

    /**
     * 为 SideBar 显示字母的 TextView
     *
     * @param mTextDialog TextView
     */
    public void setTextView(TextView mTextDialog) {
        this.mTextDialog = mTextDialog;
    }

    public SideBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public SideBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SideBar(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        // 将 20dp 转换为像素
        singleHeightPx =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                SINGLE_HEIGHT_DP,
                                context.getResources().getDisplayMetrics());
    }

    /** 重写 onMeasure 方法，根据字母数量动态设置 SideBar 的高度 */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 动态计算高度：字母数组长度 * 单个字符的高度
        int desiredHeight = b.length * singleHeightPx;

        // 设置测量宽度和高度
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, desiredHeight);
    }

    /** 重写的onDraw的方法 */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        for (int i = 0; i < b.length; i++) {
            paint.setColor(Color.GRAY); // 所有字母的默认颜色 目前为灰色(右侧字体颜色)
            paint.setTypeface(Typeface.DEFAULT); // (右侧字体样式)
            paint.setAntiAlias(true);
            paint.setTextSize(30); // (右侧字体大小)
            // 选中的状态
            if (i == choose) {
                paint.setColor(Color.parseColor("#FFFFFF")); // 选中字母的颜色 目前为白
                paint.setFakeBoldText(true); // 设置是否为粗体文字
            }
            // x坐标等于=中间-字符串宽度的一般
            float xPos = width / 2f - paint.measureText(b[i]) / 2;
            float yPos = singleHeightPx * i * 1.0f + singleHeightPx;
            canvas.drawText(b[i], xPos, yPos, paint);
            paint.reset(); // 重置画笔
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        final int action = event.getAction();
        final float y = event.getY(); // 点击y坐标
        final int oldChoose = choose;

        final OnTouchingLetterChangedListener listener = onTouchingLetterChangedListener;

        final int c = (int) (y / getHeight() * b.length); // 点击y坐标所占高度的比例*b数组的长度就等于点击b中的个数

        if (action == MotionEvent.ACTION_UP) {
            setBackgroundDrawable(new ColorDrawable(0x00000000)); // 设置背景颜色
            choose = -1;
            invalidate();
            if (mTextDialog != null) {
                mTextDialog.setVisibility(View.INVISIBLE);
            }
        } else {
            setBackgroundResource(R.drawable.rc_bg_sidebar); // 点击字母条的背景颜色
            if (oldChoose != c) {
                if (c >= 0 && c < b.length) {
                    if (listener != null) {
                        listener.onTouchingLetterChanged(b[c]);
                    }
                    if (mTextDialog != null) {
                        mTextDialog.setText(b[c]);
                        mTextDialog.setVisibility(View.VISIBLE);
                    }
                    choose = c;
                    invalidate();
                }
            }
        }

        return true;
    }
    /**
     * 向外松开的方法
     *
     * @param onTouchingLetterChangedListener OnTouchingLetterChangedListener
     */
    public void setOnTouchingLetterChangedListener(
            OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
    }

    public interface OnTouchingLetterChangedListener {
        void onTouchingLetterChanged(String s);
    }

    /**
     * 允许从外部设置字母数组
     *
     * @param letters 字母数组
     */
    public void setLetters(String[] letters) {
        if (letters != null && letters.length > 0) {
            this.b = letters;
            requestLayout();
            invalidate(); // 刷新View以显示新的字母数组
        }
    }
}
