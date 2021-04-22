package io.rong.imkit.model;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class State {
    @IntDef({NORMAL, ERROR, PROGRESS, CANCEL, PAUSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Value {
    }

    public static final int NORMAL = 0;
    public static final int ERROR = 1;
    public static final int PROGRESS = 2;
    public static final int CANCEL = 3;
    public static final int PAUSE = 4;
}
