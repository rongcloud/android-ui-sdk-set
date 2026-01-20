package io.rong.imkit.usermanage.adapter;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import io.rong.imkit.R;
import io.rong.imkit.base.adapter.CommonAdapter;
import io.rong.imkit.base.adapter.ViewHolder;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.model.UiFriendApplicationInfo;
import io.rong.imlib.model.FriendApplicationStatus;
import io.rong.imlib.model.FriendApplicationType;
import java.util.List;

public class ApplyFriendAdapter extends CommonAdapter<UiFriendApplicationInfo> {

    private OnBtnClickListener onBtnClickListener;

    public ApplyFriendAdapter() {
        super(R.layout.rc_item_apply_friend);
    }

    public void setOnBtnClickListener(OnBtnClickListener onBtnClickListener) {
        this.onBtnClickListener = onBtnClickListener;
    }

    @Override
    public void bindData(ViewHolder holder, UiFriendApplicationInfo item, int position) {
        holder.setText(R.id.tv_title, item.getInfo().getName());
        //        holder.setText(R.id.tv_content, item.getInfo().getRemark());
        TextView tv = holder.<TextView>getView(R.id.tv_content);
        if (position == 0) {
            holder.setVisible(R.id.tv_time, true);
            holder.setText(R.id.tv_time, item.getShowTime());
        } else {
            int showTime = item.getShowTime();
            UiFriendApplicationInfo preInfo = mData.get(position - 1);
            if (preInfo.getShowTime() == showTime) {
                holder.setVisible(R.id.tv_time, false);
                holder.setText(R.id.tv_time, item.getShowTime());
            } else {
                holder.setVisible(R.id.tv_time, true);
                holder.setText(R.id.tv_time, item.getShowTime());
            }
        }

        tv.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                String desc =
                                        TextUtils.isEmpty(item.getInfo().getExtra())
                                                ? holder.itemView
                                                        .getContext()
                                                        .getString(R.string.rc_request_add_friend)
                                                : item.getInfo().getExtra();
                                TextPaint paint = tv.getPaint();
                                float moreText =
                                        tv.getTextSize()
                                                * tv.getContext()
                                                        .getString(R.string.rc_expand)
                                                        .length();
                                float availableTextWidth = tv.getWidth() - moreText;
                                CharSequence ellipsizeStr =
                                        TextUtils.ellipsize(
                                                desc,
                                                paint,
                                                availableTextWidth,
                                                TextUtils.TruncateAt.END);
                                // TextView 实际显示的文本长度  < 应该显示文本的长度(收缩状态)
                                if (ellipsizeStr.length() < desc.length()) {
                                    openFun(tv, ellipsizeStr, desc); // 显示收缩状态的文本和图标
                                }
                                // TextView 实际显示的文本长度  == 应该显示文本的长度(正常状态)
                                else if (ellipsizeStr.length() == desc.length()) {
                                    tv.setText(desc); // 正常显示Textview
                                }
                                // TextView 实际显示的文本长度  > 应该显示文本的长度(展开状态)
                                else {
                                    closeFun(tv, ellipsizeStr, desc); // 显示展开状态的文本和图标
                                }
                                tv.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }

                            // 显示收缩状态的文本，设置点击图标，并添加点击事件
                            private void openFun(
                                    final TextView tv,
                                    final CharSequence ellipsizeStr,
                                    final String desc) {
                                CharSequence temp =
                                        ellipsizeStr
                                                + tv.getContext().getString(R.string.rc_expand);
                                SpannableString ss = new SpannableString(temp);
                                ClickableSpan cs =
                                        new ClickableSpan() {
                                            @Override
                                            public void onClick(@NonNull View widget) {
                                                closeFun(tv, ellipsizeStr, desc);
                                            }

                                            @Override
                                            public void updateDrawState(@NonNull TextPaint ds) {
                                                super.updateDrawState(ds);
                                                ds.setUnderlineText(false);
                                            }
                                        };

                                ss.setSpan(
                                        cs,
                                        ss.length()
                                                - tv.getContext()
                                                        .getString(R.string.rc_expand)
                                                        .length(),
                                        ss.length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                tv.setText(ss);
                                tv.setMovementMethod(LinkMovementMethod.getInstance());
                            }

                            // 显示展开状态的文本，设置点击图标，并添加点击事件
                            private void closeFun(
                                    final TextView tv,
                                    final CharSequence ellipsizeStr,
                                    final String desc) {
                                tv.setText(desc);
                            }
                        });

        RongConfigCenter.featureConfig()
                .getKitImageEngine()
                .loadUserPortrait(
                        holder.itemView.getContext(),
                        item.getInfo().getPortraitUri(),
                        holder.<ImageView>getView(R.id.iv_head));
        FriendApplicationStatus status = item.getInfo().getApplicationStatus();
        FriendApplicationType applicationType = item.getInfo().getApplicationType();
        if (applicationType == FriendApplicationType.Received) {
            if (status == FriendApplicationStatus.UnHandled) {
                holder.setVisible(R.id.tv_result, false);
                holder.setVisible(R.id.tv_reject, true);
                holder.setVisible(R.id.tv_accept, true);
                holder.setOnClickListener(
                        R.id.tv_reject,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (onBtnClickListener != null) {
                                    onBtnClickListener.onRejectClick(holder, item, position);
                                }
                            }
                        });
                holder.setOnClickListener(
                        R.id.tv_accept,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (onBtnClickListener != null) {
                                    onBtnClickListener.onAcceptClick(holder, item, position);
                                }
                            }
                        });

            } else {
                holder.setVisible(R.id.tv_result, true);
                holder.setVisible(R.id.tv_reject, false);
                holder.setVisible(R.id.tv_accept, false);
                String text;
                if (status == FriendApplicationStatus.Accepted) {
                    text = holder.itemView.getContext().getString(R.string.rc_passed);
                } else if (status == FriendApplicationStatus.Refused) {
                    text = holder.itemView.getContext().getString(R.string.rc_reject);
                } else {
                    text = holder.itemView.getContext().getString(R.string.rc_expired);
                }
                holder.setText(R.id.tv_result, text);
            }
        } else {
            holder.setVisible(R.id.tv_result, true);
            holder.setVisible(R.id.tv_reject, false);
            holder.setVisible(R.id.tv_accept, false);
            String text;
            if (status == FriendApplicationStatus.Accepted) {
                text = holder.itemView.getContext().getString(R.string.rc_added);
            } else if (status == FriendApplicationStatus.Refused) {
                text = holder.itemView.getContext().getString(R.string.rc_rejected);
            } else if (status == FriendApplicationStatus.UnHandled) {
                text = holder.itemView.getContext().getString(R.string.rc_waiting);
            } else {
                text = holder.itemView.getContext().getString(R.string.rc_expired);
            }
            holder.setText(R.id.tv_result, text);
        }
    }

    @Override
    public void setData(List<UiFriendApplicationInfo> data) {
        if (data == null) {
            return;
        }
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    public interface OnBtnClickListener {
        void onAcceptClick(ViewHolder holder, UiFriendApplicationInfo item, int position);

        void onRejectClick(ViewHolder holder, UiFriendApplicationInfo item, int position);
    }
}
