package io.rong.sticker.businesslogic;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.Nullable;

public class StickerPackagesUiHandler {

    private static Handler uiHandler;

    public static void init(Context context) {
        uiHandler = new Handler(context.getMainLooper());
    }

    public static void destroy() {
        uiHandler = null;
    }

    public @Nullable
    static Handler getUiHandler() {
        return uiHandler;
    }

}
