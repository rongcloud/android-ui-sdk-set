package io.rong.imkit.widget;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.picture.widget.BaseDialogFragment;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.utils.TextViewUtils;
import io.rong.imkit.widget.dialog.OptionsPopupDialog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;

public class ReferenceDialog extends BaseDialogFragment
        implements RongIMClient.OnRecallMessageListener {
    private TextView referenceShowText;
    private UiMessage mUiMessage;

    public ReferenceDialog(UiMessage uiMessage) {
        this.mUiMessage = uiMessage;
    }

    @Override
    protected void findView() {
        referenceShowText = mRootView.findViewById(R.id.rc_reference_window_text);
        referenceShowText.setOnLongClickListener(
                view -> {
                    showCopyDialog();
                    return false;
                });
        referenceShowText.setMovementMethod(
                new LinkTextViewMovementMethod(
                        new ILinkClickListener() {
                            @Override
                            public boolean onLinkClick(String link) {
                                String str = link.toLowerCase();
                                if (str.startsWith("http") || str.startsWith("https")) {
                                    RouteUtils.routeToWebActivity(getContext(), link);
                                    return true;
                                }

                                return false;
                            }
                        }));
    }

    @Override
    protected void initView() {
        mRootView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        IMCenter.getInstance().addOnRecallMessageListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        IMCenter.getInstance().removeOnRecallMessageListener(this);
    }

    @Override
    public void bindData() {
        SpannableStringBuilder spannable =
                TextViewUtils.getSpannable(
                        mUiMessage.getReferenceContentSpannable().toString(), this::setText);
        setText(spannable);
    }

    protected void setText(SpannableStringBuilder span) {
        ReferenceMessage content = (ReferenceMessage) mUiMessage.getMessage().getContent();
        ReferenceMessage.ReferenceMessageStatus referMsgStatus = content.getReferMsgStatus();
        if (referMsgStatus == ReferenceMessage.ReferenceMessageStatus.MODIFIED) {
            SpannableStringBuilder contentSpannable = new SpannableStringBuilder(span);
            String text = getString(R.string.rc_edit_status_success);
            SpannableStringBuilder spannable = new SpannableStringBuilder("（" + text + "）");
            ForegroundColorSpan colorSpan =
                    new ForegroundColorSpan(
                            getResources().getColor(R.color.rc_edit_success_status));
            spannable.setSpan(colorSpan, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            contentSpannable.append(spannable);
            referenceShowText.setText(contentSpannable);
        }
        // 目前delete、recall不处理
        //        else if (referMsgStatus == ReferenceMessage.ReferenceMessageStatus.DELETE) {
        //            SpannableStringBuilder contentSpannable = new SpannableStringBuilder();
        //            String text = getString(R.string.rc_reference_status_delete);
        //            SpannableStringBuilder spannableString = new SpannableStringBuilder(text);
        //            ForegroundColorSpan colorSpan =
        //                    new
        // ForegroundColorSpan(getResources().getColor(R.color.rc_edit_success_status));
        //            spannableString.setSpan(
        //                    colorSpan, 0, spannableString.length(),
        // Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        //            contentSpannable.append(spannableString);
        //            referenceShowText.setText(contentSpannable);
        //        } else if (referMsgStatus == ReferenceMessage.ReferenceMessageStatus.RECALLED) {
        //            SpannableStringBuilder contentSpannable = new SpannableStringBuilder();
        //            String text = getString(R.string.rc_reference_status_recall);
        //            SpannableStringBuilder spannableString = new SpannableStringBuilder(text);
        //            ForegroundColorSpan colorSpan =
        //                    new
        // ForegroundColorSpan(getResources().getColor(R.color.rc_edit_success_status));
        //            spannableString.setSpan(
        //                    colorSpan, 0, spannableString.length(),
        // Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        //            contentSpannable.append(spannableString);
        //            referenceShowText.setText(contentSpannable);
        //        }
        else {
            referenceShowText.setText(span);
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.rc_reference_popupwindow;
    }

    @Override
    protected float getScreenWidthProportion() {
        return 1f;
    }

    @Override
    protected int getScreenHeightProportion() {
        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    @Override
    protected int getBackgroundDrawableRes() {
        return R.color.app_color_white;
    }

    @Override
    public boolean onMessageRecalled(
            Message message, RecallNotificationMessage recallNotificationMessage) {
        int messageId = mUiMessage.getMessageId();
        if (messageId == message.getMessageId()) {
            new AlertDialog.Builder(getContext(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                    .setMessage(getString(R.string.rc_recall_success))
                    .setPositiveButton(
                            getString(R.string.rc_dialog_ok),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dismissAllowingStateLoss();
                                }
                            })
                    .setCancelable(false)
                    .show();
        }
        return false;
    }

    private void showCopyDialog() {
        if (getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing()) {
            return;
        }
        String[] items = new String[] {getString(R.string.rc_dialog_item_message_copy)};
        OptionsPopupDialog.newInstance(getActivity(), items)
                .setOptionsPopupDialogListener(
                        new OptionsPopupDialog.OnOptionsItemClickedListener() {
                            @Override
                            public void onOptionsItemClicked(int which) {
                                if (which == 0) {
                                    copyText(mUiMessage.getReferenceContentSpannable().toString());
                                }
                            }
                        })
                .show();
    }

    private void copyText(String text) {
        if (getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing()) {
            return;
        }
        ClipboardManager clipboard =
                (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            try {
                clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
            } catch (Exception e) {
            }
        }
    }
}
