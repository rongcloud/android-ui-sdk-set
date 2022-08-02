package io.rong.imkit.widget.dialog;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.io.Serializable;

import io.rong.imkit.R;

/**
 * 底部显示选择菜单对话框。
 * 通过 {@link Builder} 构建对话框。
 */
public class BottomSelectDialog extends DialogFragment implements View.OnClickListener {
    private static final String ARGUMENT_KEY_SELECTIONS = "selections";
    private static final String ARGUMENT_KEY_SELECTIONS_COLOR = "selections_color";
    private static final String ARGUMENT_KEY_TITLE = "title";

    private String[] mSelections;
    private int[] mSelectionsColor;
    private String mTitle;
    private OnSelectListener mOnSelectListener;

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.rc_dialog_bottom_select, container, false);
        LinearLayout containerLl = contentView.findViewById(R.id.rc_dialog_bottom_container);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mSelections = arguments.getStringArray(ARGUMENT_KEY_SELECTIONS);
            mSelectionsColor = arguments.getIntArray(ARGUMENT_KEY_SELECTIONS_COLOR);
            mTitle = arguments.getString(ARGUMENT_KEY_TITLE);
        }

        View cancelView = contentView.findViewById(R.id.rc_dialog_bottom_item_cancel);
        cancelView.setOnClickListener(this);

        addTitle(containerLl);
        addSelection(containerLl);

        return contentView;
    }

    /**
     * 添加标题
     *
     * @param container
     */
    private void addTitle(LinearLayout container) {
        if (!TextUtils.isEmpty(mTitle)) {
            Resources resources = getResources();
            TextView titleView = new TextView(getContext());
            titleView.setText(mTitle);
            titleView.setTextColor(resources.getColor(R.color.rc_dialog_bottom_text_title_color));
            titleView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                            , LinearLayout.LayoutParams.WRAP_CONTENT);
            int margin = (int) resources.getDimension(R.dimen.rc_dialog_bottom_text_item_margin);
            layoutParams.topMargin = margin;
            layoutParams.bottomMargin = margin;
            layoutParams.leftMargin = margin;
            layoutParams.rightMargin = margin;
            container.addView(titleView, 0, layoutParams);
            addSeparateLine(container, 1);
        }
    }

    /**
     * 添加选项
     *
     * @param container
     */
    private void addSelection(LinearLayout container) {
        if (mSelections != null && mSelections.length > 0) {
            boolean isSetSelectionColor = false;
            boolean hasTitle = false;
            if (mSelectionsColor != null && mSelections.length == mSelectionsColor.length) {
                isSetSelectionColor = true;
            }
            if (!TextUtils.isEmpty(mTitle)) {
                hasTitle = true;
            }
            Resources resources = getResources();
            int length = mSelections.length;
            int indexInContainer = hasTitle ? 2 : 0;// 2 = 1个 title + 1个分割线
            for (int i = 0; i < length; i++) {
                String selectText = mSelections[i];
                TextView titleView = new TextView(getContext());
                titleView.setText(selectText);

                if (isSetSelectionColor && mSelectionsColor[i] != 0) {
                    titleView.setTextColor(resources.getColor(mSelectionsColor[i]));
                } else {
                    titleView.setTextColor(resources.getColor(R.color.rc_dialog_bottom_text_color));
                }
                titleView.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams layoutParams =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                                , LinearLayout.LayoutParams.WRAP_CONTENT);
                int margin = (int) resources.getDimension(R.dimen.rc_dialog_bottom_text_item_margin);
                layoutParams.topMargin = margin;
                layoutParams.bottomMargin = margin;
                layoutParams.leftMargin = margin;
                layoutParams.rightMargin = margin;
                titleView.setTag(i);
                titleView.setOnClickListener(this);
                container.addView(titleView, indexInContainer++, layoutParams);

                if (i != length - 1) {
                    addSeparateLine(container, indexInContainer++);
                }
            }
        }
    }

    /**
     * 添加分割线
     *
     * @param container
     * @param index
     */
    private void addSeparateLine(LinearLayout container, int index) {
        View separateView = new View(getContext());
        Resources resources = getResources();
        int height = Math.max(1, (int) resources.getDimension(R.dimen.rc_dialog_bottom_item_separate_height));
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                        , height);
        separateView.setBackgroundColor(resources.getColor(R.color.rc_dialog_bottom_selection_separate_color));
        container.addView(separateView, index, layoutParams);
    }


    @Override
    public void onStart() {
        super.onStart();

        // 设置宽度为屏宽, 靠近屏幕底部。
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window win = dialog.getWindow();
            // 一定要设置Background，如果不设置，window属性设置无效
            if (win != null) {
                win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                WindowManager.LayoutParams params = win.getAttributes();
                params.gravity = Gravity.BOTTOM;
                // 使用ViewGroup.LayoutParams，以便Dialog 宽度充满整个屏幕
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                win.setAttributes(params);
            }

            FragmentActivity activity = getActivity();
            if (activity != null && activity.getWindowManager() != null) {
                //全屏化对话框
                DisplayMetrics dm = new DisplayMetrics();
                activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rc_dialog_bottom_item_cancel) {
            dismiss();
        } else {
            if (v.getTag() instanceof Integer) {
                int index = (int) v.getTag();
                if (mOnSelectListener != null) {
                    mOnSelectListener.onSelect(index);
                }
                dismiss();
            }
        }
    }

    /**
     * 设置点击选项监听。
     * 当 Activity 发生如转屏的事件导致 Fragment 回收时需要重新设置。
     *
     * @param listener
     */
    public void setOnSelectListener(OnSelectListener listener) {
        mOnSelectListener = listener;
    }

    /**
     * 构造器。
     * 通过 {@link #setSelections(String[])} 来设置选项。
     * 通过 {@link #build()} 创建实例。
     * 创建实例后通过{@link BottomSelectDialog#setOnSelectListener(OnSelectListener)} 设置监听。
     */
    public static class Builder {
        private String[] mSelections;
        private int[] mSelectionsColor;
        private String mTitle;

        /**
         * 设置选项中的文字
         *
         * @param selections
         * @return
         */
        public Builder setSelections(String[] selections) {
            this.mSelections = selections;
            return this;
        }

        /**
         * 设置选项中的文字的颜色资源（需要是 resource color 资源。）。
         * 长度必须与 {@link #setSelections(String[])} 一直才能生效。
         * 当设置值为 0 时，使用默认颜色。
         *
         * @param colors
         * @return
         */
        public Builder setSelectionsColor(int[] colors) {
            this.mSelectionsColor = colors;
            return this;
        }

        /**
         * 设置标题。
         * 标题可不设置。
         *
         * @param title
         * @return
         */
        public Builder setTitle(String title) {
            this.mTitle = title;
            return this;
        }

        public BottomSelectDialog build() {
            BottomSelectDialog bottomSelectDialog = new BottomSelectDialog();
            Bundle bundle = new Bundle();
            bundle.putStringArray(ARGUMENT_KEY_SELECTIONS, mSelections);
            bundle.putIntArray(ARGUMENT_KEY_SELECTIONS_COLOR, mSelectionsColor);
            bundle.putString(ARGUMENT_KEY_TITLE, mTitle);
            bottomSelectDialog.setArguments(bundle);
            return bottomSelectDialog;
        }
    }

    /**
     * 点击选项监听。
     */
    public interface OnSelectListener extends Serializable {
        /**
         * 当选择选项时回调
         *
         * @param index 当前选中的下标，与 {@link Builder#setSelections} 下标对应。
         */
        void onSelect(int index);
    }

}
