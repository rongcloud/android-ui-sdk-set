package io.rong.imkit.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.List;

/** @author gusd @Date 2021/10/22 */
public class RongEditText extends EditText {

    private List<TextWatcher> mTextWatcherList;

    public RongEditText(Context context) {
        this(context, null);
    }

    public RongEditText(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public RongEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.addTextChangedListener(mTextWatcher);
    }

    @Override
    public void addTextChangedListener(TextWatcher watcher) {
        if (mTextWatcherList == null) {
            mTextWatcherList = new ArrayList<>();
        }
        if (!mTextWatcherList.contains(watcher)) {
            mTextWatcherList.add(watcher);
        }
    }

    @Override
    public void removeTextChangedListener(TextWatcher watcher) {
        if (mTextWatcherList != null) {
            mTextWatcherList.remove(watcher);
        }
    }

    public void setText(CharSequence text, boolean triggerTextChanged) {
        if (!triggerTextChanged) {
            super.removeTextChangedListener(mTextWatcher);
        }
        super.setText(text);
        if (!triggerTextChanged) {
            super.addTextChangedListener(mTextWatcher);
        }
    }

    public void removeAllTextChangedListener() {
        if (mTextWatcherList != null) {
            mTextWatcherList.clear();
        }
    }

    private TextWatcher mTextWatcher =
            new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    if (mTextWatcherList != null) {
                        for (TextWatcher textWatcher : mTextWatcherList) {
                            textWatcher.beforeTextChanged(s, start, count, after);
                        }
                    }
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (mTextWatcherList != null) {
                        for (TextWatcher textWatcher : mTextWatcherList) {
                            textWatcher.onTextChanged(s, start, before, count);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (mTextWatcherList != null) {
                        for (TextWatcher textWatcher : mTextWatcherList) {
                            textWatcher.afterTextChanged(s);
                        }
                    }
                }
            };
}
