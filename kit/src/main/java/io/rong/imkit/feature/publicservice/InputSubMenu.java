package io.rong.imkit.feature.publicservice;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.utils.RongUtils;

public class InputSubMenu {
    private PopupWindow mPopupWindow;
    private ViewGroup container;
    private LayoutInflater mInflater;
    private ISubMenuItemClickListener mOnClickListener;

    public InputSubMenu(Context context, List<String> menus) {
        mInflater = LayoutInflater.from(context);
        container = (ViewGroup) mInflater.inflate(R.layout.rc_ext_sub_menu_container, null);
        mPopupWindow = new PopupWindow(container, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setupSubMenus(container, menus);
    }

    public void showAtLocation(View parent) {
        mPopupWindow.setBackgroundDrawable(new ColorDrawable());
        container.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        int[] location = new int[2];
        int w = container.getMeasuredWidth();
        int h = container.getMeasuredHeight();
        parent.getLocationOnScreen(location);
        int x = location[0] + (parent.getWidth() - w) / 2;
        // 根据设计图，弹窗至少距离屏幕右边缘10dp
        int maxRightX = RongUtils.getScreenWidth() - RongUtils.dip2px(10);
        if ((x + w) > maxRightX) {
            x = maxRightX - w;
        }
        int y = location[1] - h - RongUtils.dip2px(3);

        mPopupWindow.showAtLocation(parent, Gravity.START | Gravity.TOP, x, y);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);
        mPopupWindow.update();
    }

    public void setOnItemClickListener(ISubMenuItemClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    private void setupSubMenus(ViewGroup viewGroup, List<String> menus) {
        for (int i = 0; i < menus.size(); i++) {
            View view = mInflater.inflate(R.layout.rc_ext_sub_menu_item, null);
            TextView tv = view.findViewById(R.id.rc_sub_menu_title);
            View divider = view.findViewById(R.id.rc_sub_menu_divider_line);
            String title = menus.get(i);
            tv.setText(title);
            if (i < menus.size() - 1) {
                divider.setVisibility(View.VISIBLE);
            }
            view.setTag(i);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = (int) v.getTag();
                    mOnClickListener.onClick(index);
                    mPopupWindow.dismiss();
                }
            });
            viewGroup.addView(view);
        }
    }
}
