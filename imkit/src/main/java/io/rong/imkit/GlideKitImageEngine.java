package io.rong.imkit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.utils.GlideUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

public class GlideKitImageEngine implements KitImageEngine {
    private Transformation<Bitmap> transformation = new CenterCrop();

    /**
     * 加载图片
     *
     * @param context
     * @param url
     * @param imageView
     */
    @Override
    public void loadImage(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        Glide.with(context)
                .load(url)
                .error(R.drawable.rc_received_thumb_image_broken)
                .into(imageView);
    }

    /**
     * 加载相册目录
     *
     * @param context 上下文
     * @param url 图片路径
     * @param imageView 承载图片ImageView
     */
    @Override
    public void loadFolderImage(
            @NonNull final Context context,
            @NonNull String url,
            @NonNull final ImageView imageView) {
        Glide.with(context)
                .asBitmap()
                .override(180, 180)
                .centerCrop()
                .sizeMultiplier(0.5f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                //                .placeholder(R.drawable.picture_icon_placeholder)
                .load(url)
                .into(
                        new BitmapImageViewTarget(imageView) {
                            @Override
                            protected void setResource(Bitmap resource) {
                                RoundedBitmapDrawable circularBitmapDrawable =
                                        RoundedBitmapDrawableFactory.create(
                                                context.getResources(), resource);
                                circularBitmapDrawable.setCornerRadius(8);
                                imageView.setImageDrawable(circularBitmapDrawable);
                            }
                        });
    }

    /**
     * 加载gif
     *
     * @param context 上下文
     * @param url 图片路径
     * @param imageView 承载图片ImageView
     */
    @Override
    public void loadAsGifImage(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        Glide.with(context).asGif().load(url).into(imageView);
    }

    /**
     * 加载图片列表图片
     *
     * @param context 上下文
     * @param url 图片路径
     * @param imageView 承载图片ImageView
     */
    @Override
    public void loadGridImage(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        Glide.with(context)
                .asBitmap()
                .load(url)
                .override(200, 200)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                //                .placeholder(R.drawable.picture_image_placeholder)
                .into(imageView);
    }

    @Override
    public void loadConversationListPortrait(
            @NonNull Context context,
            @NonNull String url,
            @NonNull ImageView imageView,
            Conversation conversation) {
        @DrawableRes
        int resourceId =
                IMKitThemeManager.getAttrResId(
                        context, R.attr.rc_conversation_list_cell_portrait_msg_img);
        switch (conversation.getConversationType()) {
            case GROUP:
                resourceId =
                        IMKitThemeManager.getAttrResId(
                                context, R.attr.rc_conversation_list_cell_group_portrait_img);
                break;
            case CUSTOMER_SERVICE:
                resourceId =
                        IMKitThemeManager.getAttrResId(
                                context, R.attr.rc_conversation_list_cell_portrait_kefu_img);
                break;
            case CHATROOM:
                resourceId =
                        IMKitThemeManager.getAttrResId(
                                context, R.attr.rc_conversation_list_cell_discussion_portrait_img);
                break;
            default:
                break;
        }
        loadPortrait(context, url, imageView, resourceId);
    }

    @Override
    public void loadConversationPortrait(
            @NonNull Context context,
            @NonNull String url,
            @NonNull ImageView imageView,
            Message message) {
        @DrawableRes
        int resourceId =
                IMKitThemeManager.getAttrResId(
                        context, R.attr.rc_conversation_list_cell_portrait_msg_img);
        switch (message.getConversationType()) {
            case CUSTOMER_SERVICE:
                if (Message.MessageDirection.RECEIVE == message.getMessageDirection()) {
                    resourceId =
                            IMKitThemeManager.getAttrResId(
                                    context, R.attr.rc_conversation_list_cell_portrait_kefu_img);
                }
                break;
            default:
                break;
        }

        loadPortrait(context, url, imageView, resourceId);
    }

    @Override
    public void loadUserPortrait(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        int defaultPortrait =
                IMKitThemeManager.getAttrResId(
                        context, R.attr.rc_conversation_list_cell_portrait_msg_img);
        Glide.with(imageView)
                .load(url)
                .placeholder(defaultPortrait)
                .error(defaultPortrait)
                .apply(RequestOptions.bitmapTransform(getPortraitTransformation()))
                .into(imageView);
    }

    @Override
    public void loadGroupPortrait(
            @NonNull Context context, @NonNull String url, @NonNull ImageView imageView) {
        int defaultGroupPortrait =
                IMKitThemeManager.getAttrResId(
                        context, R.attr.rc_conversation_list_cell_group_portrait_img);
        Glide.with(imageView)
                .load(url)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .error(
                        Glide.with(imageView)
                                .load(defaultGroupPortrait)
                                .apply(RequestOptions.bitmapTransform(new CircleCrop())))
                .into(imageView);
    }

    // 加载头像
    private void loadPortrait(
            @NonNull Context context,
            @NonNull String url,
            @NonNull ImageView imageView,
            @DrawableRes int resourceId) {
        KitMediaInterceptor interceptor = RongConfigCenter.featureConfig().getKitMediaInterceptor();
        boolean isHttpUrl = url.startsWith("http") || url.startsWith("https");
        if (!isHttpUrl || interceptor == null) {
            loadImage(context, url, imageView, resourceId);
            return;
        }

        // 是网络图片地址 + 且设置拦截器，走拦截器逻辑
        interceptor.onGlidePrepareLoad(
                url,
                null,
                map ->
                        imageView.post(
                                () ->
                                        loadImage(
                                                context,
                                                GlideUtils.buildGlideUrl(url, map),
                                                imageView,
                                                resourceId)));
    }

    private void loadImage(Context context, Object model, ImageView imageView, int resourceId) {
        if (context instanceof Activity) {
            if (((Activity) context).isDestroyed() || ((Activity) context).isFinishing()) {
                return;
            }
        }

        Glide.with(imageView)
                .load(model)
                .placeholder(resourceId)
                .error(resourceId)
                .apply(RequestOptions.bitmapTransform(getPortraitTransformation()))
                .into(imageView);
    }

    protected Transformation<Bitmap> getPortraitTransformation() {
        return transformation;
    }
}
