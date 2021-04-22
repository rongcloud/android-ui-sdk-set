package io.rong.imkit.feature.reference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.LayoutDirection;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.text.TextUtilsCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.List;
import java.util.Locale;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.activity.FilePreviewActivity;
import io.rong.imkit.activity.PicturePagerActivity;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.component.emoticon.AndroidEmoji;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.utils.StringUtils;
import io.rong.imkit.utils.TextViewUtils;
import io.rong.imkit.widget.ILinkClickListener;
import io.rong.imkit.widget.LinkTextViewMovementMethod;
import io.rong.imkit.widget.ReferenceDialog;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.FileMessage;
import io.rong.message.ImageMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.TextMessage;

import static io.rong.imkit.conversation.messgelist.provider.SightMessageItemProvider.dip2pix;

public class ReferenceMessageItemProvider extends BaseMessageItemProvider<ReferenceMessage> {

    private static final String TAG = "ReferenceMessageItemProvider";

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_reference_message, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder, ViewHolder parentHolder, ReferenceMessage referenceMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        TextView referenceSendContent = holder.getView(R.id.rc_msg_tv_reference_send_content);
        if (referenceMessage.getUserId() != null) {
            holder.setText(R.id.rc_msg_tv_reference_name, getDisplayName(uiMessage, referenceMessage.getUserId()) + " : ");
        }
        if (referenceMessage.getEditSendText() != null) {
            setTextContent(referenceSendContent, uiMessage, referenceMessage.getEditSendText(), true);
            setMovementMethod(uiMessage, referenceSendContent);
        }
        if (referenceMessage.getReferenceContent() == null) {
            return;
        }
        if (referenceMessage.getReferenceContent() instanceof TextMessage) {
            setTextType(holder.getConvertView(), holder, parentHolder, position, referenceMessage, uiMessage);
            holder.setVisible(R.id.rc_msg_tv_reference_content, true);
            holder.setVisible(R.id.rc_msg_iv_reference, false);
            holder.setVisible(R.id.rc_msg_tv_reference_file_name, false);
        } else if (referenceMessage.getReferenceContent() instanceof ImageMessage) {
            setImageType(holder.getConvertView(), holder, parentHolder, position, referenceMessage, uiMessage);
            holder.setVisible(R.id.rc_msg_tv_reference_content, false);
            holder.setVisible(R.id.rc_msg_iv_reference, true);
            holder.setVisible(R.id.rc_msg_tv_reference_file_name, false);
        } else if (referenceMessage.getReferenceContent() instanceof FileMessage) {
            setFileType(holder.getConvertView(), holder, parentHolder, position, referenceMessage, uiMessage);
            holder.setVisible(R.id.rc_msg_tv_reference_content, false);
            holder.setVisible(R.id.rc_msg_iv_reference, false);
            holder.setVisible(R.id.rc_msg_tv_reference_file_name, true);
        } else if (referenceMessage.getReferenceContent() instanceof RichContentMessage) {
            setRichType(holder.getConvertView(), holder, parentHolder, position, referenceMessage, uiMessage);
            holder.setVisible(R.id.rc_msg_tv_reference_content, true);
            TextView referenceContent = holder.getView(R.id.rc_msg_tv_reference_content);
            referenceContent.setMaxLines(3);
            referenceContent.setEllipsize(TextUtils.TruncateAt.END);
            holder.setVisible(R.id.rc_msg_iv_reference, false);
            holder.setVisible(R.id.rc_msg_tv_reference_file_name, false);
        } else if (referenceMessage.getReferenceContent() instanceof ReferenceMessage) {
            setReferenceType(holder.getConvertView(), holder, parentHolder, position, referenceMessage, uiMessage);
            holder.setVisible(R.id.rc_msg_tv_reference_content, true);
            holder.setVisible(R.id.rc_msg_iv_reference, false);
            holder.setVisible(R.id.rc_msg_tv_reference_file_name, false);
        } else {
            holder.setVisible(R.id.rc_msg_tv_reference_content, true);
            holder.setText(R.id.rc_msg_tv_reference_content, holder.getContext().getString(R.string.rc_message_unknown));
            holder.setVisible(R.id.rc_msg_iv_reference, false);
            holder.setVisible(R.id.rc_msg_tv_reference_file_name, false);
        }
    }

    @Override
    protected boolean onItemClick(ViewHolder holder, ReferenceMessage referenceMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        return false;
    }

    @Override
    protected boolean onItemLongClick(ViewHolder holder, ReferenceMessage referenceMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof ReferenceMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, ReferenceMessage referenceMessage) {
        if (referenceMessage != null && !TextUtils.isEmpty(referenceMessage.getEditSendText())) {
            return new SpannableString(referenceMessage.getEditSendText());
        } else {
            return null;
        }

    }

    private String getDisplayName(UiMessage uiMessage, String referenceUserId) {
        if (uiMessage.getMessage().getSenderUserId() != null) {
            if (uiMessage.getMessage().getConversationType().equals(Conversation.ConversationType.GROUP)) {
                GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(uiMessage.getMessage().getTargetId(), referenceUserId);
                if (groupUserInfo != null) {
                    return groupUserInfo.getNickname();
                }
            }
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(referenceUserId);
            if (userInfo != null) {
                return userInfo.getName();
            }
        }
        return "";
    }

    private SpannableStringBuilder createSpan(String content) {
        SpannableStringBuilder spannable = new SpannableStringBuilder(content);
        AndroidEmoji.ensure(spannable);
        return spannable;
    }

    private void setTextContent(final TextView textView, final UiMessage data, String content, boolean isSendContent) {
        textView.setTag(data.getMessageId());
        content = StringUtils.getStringNoBlank(content);
        if (isSendContent) {
            if (data.getContentSpannable() == null) {
                SpannableStringBuilder spannable = TextViewUtils.getSpannable(content, new TextViewUtils.RegularCallBack() {
                    @Override
                    public void finish(SpannableStringBuilder spannable) {
                        data.setContentSpannable(spannable);
                        if (textView.getTag().equals(data.getMessageId())) {
                            textView.post(new Runnable() {
                                @Override
                                public void run() {
                                    textView.setText(data.getContentSpannable());
                                }
                            });
                        }
                    }
                });
                data.setContentSpannable(spannable);
            }
            textView.setText(data.getContentSpannable());
        } else {
            if (data.getReferenceContentSpannable() == null) {
                SpannableStringBuilder spannable = TextViewUtils.getSpannable(content, new TextViewUtils.RegularCallBack() {
                    @Override
                    public void finish(SpannableStringBuilder spannable) {
                        data.setReferenceContentSpannable(spannable);
                        if (textView.getTag().equals(data.getMessageId())) {
                            textView.post(new Runnable() {
                                @Override
                                public void run() {
                                    textView.setText(data.getReferenceContentSpannable());
                                }
                            });
                        }
                    }
                });
                data.setReferenceContentSpannable(spannable);
            }
            textView.setText(data.getReferenceContentSpannable());
        }
    }

    private void setMovementMethod(final UiMessage uiMessage, final TextView textView) {
        textView.setMovementMethod(new LinkTextViewMovementMethod(new ILinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                boolean result = false;
                if (RongConfigCenter.conversationConfig().getConversationClickListener() != null) {
                    result = RongConfigCenter.conversationConfig().getConversationClickListener().onMessageLinkClick(textView.getContext(), link, uiMessage.getMessage());
                }
                if (result)
                    return true;
                String str = link.toLowerCase();
                if (str.startsWith("http") || str.startsWith("https")) {
                    RouteUtils.routeToWebActivity(textView.getContext(), link);
                    result = true;
                }

                return result;
            }
        }));
    }

    private void setTextType(final View view, ViewHolder holder, ViewHolder parentHolder, final int position, final ReferenceMessage referenceMessage, final UiMessage uiMessage) {
        if (referenceMessage == null || referenceMessage.getReferenceContent() == null) {
            return;
        }
        TextView textView = holder.getView(R.id.rc_msg_tv_reference_content);
        textView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListenerByEllipsize(textView, 1));
        TextMessage content = (TextMessage) referenceMessage.getReferenceContent();
        setTextContent(textView, uiMessage, content.getContent(), false);
        setReferenceContentAction(view, holder, parentHolder, position, referenceMessage, uiMessage);
        textClickAction(view, textView, uiMessage.getReferenceContentSpannable());
    }

    private void textClickAction(final View view, TextView textView, final SpannableStringBuilder span) {
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopWindow(view.getContext(), span);
                hideInputKeyboard(view);
            }
        });
    }

    private void hideInputKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setReferenceContentAction(final View view, ViewHolder holder, final ViewHolder parentHolder, final int position, final ReferenceMessage referenceMessage, final UiMessage uiMessage) {
        holder.setOnLongClickListener(R.id.rc_msg_tv_reference_content, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return parentHolder.getView(R.id.rc_content).performLongClick();
            }
        });

        holder.setOnLongClickListener(R.id.rc_msg_tv_reference_send_content, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return parentHolder.getView(R.id.rc_content).performLongClick();
            }
        });
        TextView referenceContent = holder.getView(R.id.rc_msg_tv_reference_content);
        setMovementMethod(uiMessage, referenceContent);

    }

    private void setImageType(final View view, ViewHolder holder, final ViewHolder parentHolder, final int position, final ReferenceMessage referenceMessage, final UiMessage uiMessage) {
        if (referenceMessage == null || referenceMessage.getReferenceContent() == null) {
            return;
        }
        final ImageView imageView = holder.getView(R.id.rc_msg_iv_reference);
        ImageMessage content = (ImageMessage) referenceMessage.getReferenceContent();
        Uri imageUri = null;
        if (content.getThumUri() != null) {
            imageUri = content.getThumUri();
        } else if (content.getLocalPath() != null) {
            imageUri = content.getLocalPath();
        } else if (content.getMediaUrl() != null) {
            imageUri = content.getMediaUrl();
        }
        if (imageUri != null) {
            RequestOptions options = RequestOptions.bitmapTransform(new RoundedCorners(dip2pix(IMCenter.getInstance().getContext(), 3))).override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
            Glide.with(view).load(imageUri).apply(options).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
                    layoutParams.width = resource.getIntrinsicWidth();
                    layoutParams.height = resource.getIntrinsicHeight();
                    imageView.setLayoutParams(layoutParams);
                    return false;
                }
            }).into(imageView);
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent(view.getContext(), PicturePagerActivity.class);
                    intent.setPackage(view.getContext().getPackageName());
                    intent.putExtra("message", uiMessage.getMessage());
                    view.getContext().startActivity(intent);
                } catch (Exception e) {
                    RLog.e(TAG, "setImageType", e);
                }
            }
        });

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return parentHolder.getView(R.id.rc_content).performLongClick();
            }
        });

        holder.setOnLongClickListener(R.id.rc_msg_tv_reference_send_content, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return parentHolder.getView(R.id.rc_content).performLongClick();
            }
        });
    }

    private void setFileType(final View view, final ViewHolder holder, final ViewHolder parentHolder, final int position, final ReferenceMessage referenceMessage, final UiMessage uiMessage) {
        if (referenceMessage == null || referenceMessage.getReferenceContent() == null) {
            return;
        }
        final FileMessage content = (FileMessage) referenceMessage.getReferenceContent();
        String string = view.getContext().getString(R.string.rc_search_file_prefix) +
                ' ' + content.getName();

        final SpannableStringBuilder ssb = new SpannableStringBuilder(string);
        ssb.setSpan(new ForegroundColorSpan(view.getContext().getResources().getColor(R.color.rc_reference_text_link_color)),
                0,
                string.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        AndroidEmoji.ensure(ssb);
        holder.setText(R.id.rc_msg_tv_reference_file_name, ssb);
        holder.setOnClickListener(R.id.rc_msg_tv_reference_file_name, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent();
                    intent.setClass(view.getContext(), FilePreviewActivity.class);
                    intent.setPackage(view.getContext().getPackageName());
                    ReferenceMessage referenceMessage = (ReferenceMessage) uiMessage.getMessage().getContent();
                    FileMessage fileMessage = (FileMessage) referenceMessage.getReferenceContent();
                    intent.putExtra("FileMessage", fileMessage);
                    intent.putExtra("Message", uiMessage.getMessage());
                    intent.putExtra("Progress", uiMessage.getProgress());
                    view.getContext().startActivity(intent);
                } catch (Exception e) {
                    RLog.e(TAG, "exception: " + e);
                }
            }
        });
        holder.setOnLongClickListener(R.id.rc_msg_tv_reference_file_name, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return parentHolder.getView(R.id.rc_content).performLongClick();
            }
        });
        holder.setOnLongClickListener(R.id.rc_msg_tv_reference_send_content, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return parentHolder.getView(R.id.rc_content).performLongClick();
            }
        });
    }

    private void setRichType(final View view, ViewHolder holder, final ViewHolder parentHolder, final int position, final ReferenceMessage referenceMessage, final UiMessage uiMessage) {
        if (referenceMessage == null || referenceMessage.getReferenceContent() == null) {
            return;
        }
        final RichContentMessage content = (RichContentMessage) referenceMessage.getReferenceContent();
        String string = view.getContext().getString(R.string.rc_reference_link) +
                ' ' + content.getTitle();
        final TextView textView = holder.getView(R.id.rc_msg_tv_reference_content);
        textView.setTag(uiMessage.getMessageId());
        if (uiMessage.getReferenceContentSpannable() == null) {
            SpannableStringBuilder spannable = TextViewUtils.getRichSpannable(string, new TextViewUtils.RegularCallBack() {
                @Override
                public void finish(SpannableStringBuilder spannable) {
                    uiMessage.setReferenceContentSpannable(spannable);
                    if (textView.getTag().equals(uiMessage.getMessageId())) {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(uiMessage.getReferenceContentSpannable());
                            }
                        });
                    }
                }
            }, textView.getResources().getColor(R.color.rc_reference_text_link_color));
            uiMessage.setReferenceContentSpannable(spannable);
        }
        textView.setText(uiMessage.getReferenceContentSpannable());
        holder.setOnClickListener(R.id.rc_msg_tv_reference_content, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RouteUtils.routeToWebActivity(view.getContext(), content.getUrl());
            }
        });
        holder.setOnLongClickListener(R.id.rc_msg_tv_reference_content, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return parentHolder.getView(R.id.rc_content).performLongClick();
            }
        });
        holder.setOnLongClickListener(R.id.rc_msg_tv_reference_send_content, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return parentHolder.getView(R.id.rc_content).performLongClick();
            }
        });
    }

    private void setReferenceType(final View view, ViewHolder holder, ViewHolder parentHolder, final int position, final ReferenceMessage referenceMessage, final UiMessage uiMessage) {
        if (referenceMessage == null || referenceMessage.getReferenceContent() == null) {
            return;
        }
        ReferenceMessage content = (ReferenceMessage) referenceMessage.getReferenceContent();
        setTextContent(holder.<TextView>getView(R.id.rc_msg_tv_reference_content), uiMessage, content.getEditSendText(), false)
        ;
        setReferenceContentAction(view, holder, parentHolder, position, referenceMessage, uiMessage);
    }

    private void showPopWindow(Context context, SpannableStringBuilder showText) {
        //View inflate = View.inflate(context, R.layout.rc_reference_popupwindow, null);
        if (context instanceof FragmentActivity) {
            new ReferenceDialog(showText).show(((FragmentActivity) context).getSupportFragmentManager());
        }

        //final PopupWindow popupWindow = new PopupWindow(inflate, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        //lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
//
//        //sdk > 21 解决 标题栏没有办法遮罩的问题
//        popupWindow.setClippingEnabled(false);
//        popupWindow.setFocusable(true);
//        popupWindow.setOutsideTouchable(true);
//        popupWindow.showAtLocation(inflate, Gravity.NO_GRAVITY, 0, 0);
//        fullScreenImmersive(inflate);
    }

    private void fullScreenImmersive(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
            view.setSystemUiVisibility(uiOptions);
        }
    }

    public class OnGlobalLayoutListenerByEllipsize implements ViewTreeObserver.OnGlobalLayoutListener {

        private TextView textView;
        private int maxLines;

        public OnGlobalLayoutListenerByEllipsize(TextView textView, int maxLines) {
            if (maxLines <= 0) {
                throw new IllegalArgumentException("MaxLines cannot be less than or equal to 0");
            }
            this.textView = textView;
            this.maxLines = maxLines;
            this.textView.setMaxLines(this.maxLines + 1);
            this.textView.setSingleLine(false);
        }

        @Override
        public void onGlobalLayout() {
            if (textView.getLineCount() > maxLines) {
                int line = textView.getLayout().getLineEnd(maxLines - 1);
                //定义成 CharSequence 类型，是为了兼容 emoji 表情，如果使用 String 类型则会造成 emoji 无法显示
                CharSequence truncate = "...";
                CharSequence text = textView.getText();
                try {
                    text = text.subSequence(0, line - 2);
                } catch (Exception e) {
                    truncate = "";
                    text = textView.getText();
                }
                if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == LayoutDirection.RTL) {
                    textView.setText(truncate.toString() + text);
                } else {
                    textView.setText(text + truncate.toString());
                }
            }
        }
    }
}
