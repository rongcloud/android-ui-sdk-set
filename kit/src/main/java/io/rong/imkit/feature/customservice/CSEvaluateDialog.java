package io.rong.imkit.feature.customservice;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.cs.CustomServiceConfig;

public class CSEvaluateDialog extends AlertDialog {
    private static final String TAG = "CSEvaluateDialog";
    private int mStars;
    private boolean mResolved;
    private String mTargetId;
    private CustomServiceConfig.CSEvaSolveStatus mSolveStatus;
    private EvaluateClickListener mClickListener;

    public CSEvaluateDialog(Context context, String targetId) {
        super(context);
        setCanceledOnTouchOutside(false);
        mTargetId = targetId;
    }

    public void setClickListener(EvaluateClickListener listener) {
        mClickListener = listener;
    }

    /**
     * 展示机器人评价
     *
     * @param resolved 是否解决问题
     */
    public void showRobot(boolean resolved) {
        show();
        setContentView(R.layout.rc_cs_alert_robot_evaluation);
        final LinearLayout linearLayout = findViewById(R.id.rc_cs_yes_no);
        if (resolved) {
            linearLayout.getChildAt(0).setSelected(true);
            linearLayout.getChildAt(1).setSelected(false);
        } else {
            linearLayout.getChildAt(0).setSelected(false);
            linearLayout.getChildAt(1).setSelected(true);
        }
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            View child = linearLayout.getChildAt(i);
            child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setSelected(true);
                    int index = linearLayout.indexOfChild(v);
                    if (index == 0) {
                        linearLayout.getChildAt(1).setSelected(false);
                        mResolved = true;
                    } else {
                        mResolved = false;
                        linearLayout.getChildAt(0).setSelected(false);
                    }
                }
            });
        }

        findViewById(R.id.rc_btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null)
                    mClickListener.onEvaluateCanceled();
            }
        });

        findViewById(R.id.rc_btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RongIMClient.getInstance().evaluateCustomService(mTargetId, mResolved, "");
                if (mClickListener != null)
                    mClickListener.onEvaluateSubmit();
            }
        });
    }

    /**
     * 展示星级评价
     *
     * @param dialogId 提交评价时的会话 id
     */
    public void showStar(final String dialogId) {
        show();
        setContentView(R.layout.rc_cs_alert_human_evaluation);
        final LinearLayout linearLayout = findViewById(R.id.rc_cs_stars);
        for (int i = 0; i < linearLayout.getChildCount(); i++) {
            View child = linearLayout.getChildAt(i);
            child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int index = linearLayout.indexOfChild(v);
                    int count = linearLayout.getChildCount();
                    mStars = index + 1;
                    if (!v.isSelected()) {
                        while (index >= 0) {
                            linearLayout.getChildAt(index).setSelected(true);
                            index--;
                        }
                    } else {
                        index++;
                        while (index < count) {
                            linearLayout.getChildAt(index).setSelected(false);
                            index++;
                        }
                    }
                }
            });
        }

        findViewById(R.id.rc_btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null)
                    mClickListener.onEvaluateCanceled();
            }
        });

        findViewById(R.id.rc_btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RongIMClient.getInstance().evaluateCustomService(mTargetId, mStars, null, dialogId);
                if (mClickListener != null)
                    mClickListener.onEvaluateSubmit();
            }
        });
    }

    /**
     * 展示星级和留言评价
     *
     * @param resolvedOption 是否显示已解决问题选项
     */
    public void showStarMessage(boolean resolvedOption) {
        LinearLayout view = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.rc_cs_evaluate, null);
        setView(view);
        show();
        if (getWindow() != null) {
            getWindow().setContentView(R.layout.rc_cs_evaluate);
        } else {
            RLog.e(TAG, "getWindow is null.");
        }

        final RatingBar ratingBar = findViewById(R.id.rc_rating_bar);
        final TextView evaluateLevel = findViewById(R.id.rc_evaluate_level);
        final TextView isResolved = findViewById(R.id.rc_cs_resolved_or_not);
        final LinearLayout progressLayout = findViewById(R.id.rc_resolve_progress);
        final ImageView resolvedBtn = findViewById(R.id.rc_cs_resolved);
        final ImageView resolvingBtn = findViewById(R.id.rc_cs_resolving);
        final ImageView unresolvedBtn = findViewById(R.id.rc_cs_unresolved);
        final EditText edit = findViewById(R.id.rc_cs_evaluate_content);
        final ImageView closeBtn = findViewById(R.id.rc_close_button);
        final TextView submitBtn = findViewById(R.id.rc_submit_button);

        mStars = 5;

        if (resolvedOption) {
            isResolved.setVisibility(View.VISIBLE);
            progressLayout.setVisibility(View.VISIBLE);
            mSolveStatus = CustomServiceConfig.CSEvaSolveStatus.RESOLVED;
        } else {
            isResolved.setVisibility(View.GONE);
            progressLayout.setVisibility(View.GONE);
        }

        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (ratingBar.getId() == R.id.rc_rating_bar) {
                    if (rating >= 5) {
                        mStars = 5;
                        evaluateLevel.setText(R.string.rc_cs_very_satisfactory);
                    } else if (rating >= 4 && rating < 5) {
                        mStars = 4;
                        evaluateLevel.setText(R.string.rc_cs_satisfactory);
                    } else if (rating >= 3 && rating < 4) {
                        mStars = 3;
                        evaluateLevel.setText(R.string.rc_cs_average);
                    } else if (rating >= 2 && rating < 3) {
                        mStars = 2;
                        evaluateLevel.setText(R.string.rc_cs_unsatisfactory);
                    } else {
                        mStars = 1;
                        evaluateLevel.setText(R.string.rc_cs_very_unsatisfactory);
                    }
                }
            }
        });
        evaluateLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        resolvedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSolveStatus = CustomServiceConfig.CSEvaSolveStatus.RESOLVED;
                resolvedBtn.setImageDrawable(v.getResources().getDrawable(R.drawable.rc_cs_resolved_hover));
                resolvingBtn.setImageDrawable(v.getResources().getDrawable(R.drawable.rc_cs_follow));
                unresolvedBtn.setImageDrawable(v.getResources().getDrawable(R.drawable.rc_cs_unresolved));
            }
        });
        resolvingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSolveStatus = CustomServiceConfig.CSEvaSolveStatus.RESOLVING;
                resolvedBtn.setImageDrawable(v.getResources().getDrawable(R.drawable.rc_cs_resolved));
                resolvingBtn.setImageDrawable(v.getResources().getDrawable(R.drawable.rc_cs_follow_hover));
                unresolvedBtn.setImageDrawable(v.getResources().getDrawable(R.drawable.rc_cs_unresolved));
            }
        });
        unresolvedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSolveStatus = CustomServiceConfig.CSEvaSolveStatus.UNRESOLVED;
                resolvedBtn.setImageDrawable(v.getResources().getDrawable(R.drawable.rc_cs_resolved));
                resolvingBtn.setImageDrawable(v.getResources().getDrawable(R.drawable.rc_cs_follow));
                unresolvedBtn.setImageDrawable(v.getResources().getDrawable(R.drawable.rc_cs_unresolved_hover));
            }
        });
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) edit.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
                if (mClickListener != null)
                    mClickListener.onEvaluateCanceled();
            }
        });
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) edit.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
                RongIMClient.getInstance().evaluateCustomService(mTargetId, mStars, mSolveStatus, edit.getText().toString(), null);
                if (mClickListener != null)
                    mClickListener.onEvaluateSubmit();
            }
        });
    }

    public void destroy() {
        dismiss();
    }

    @Override
    public void setOnCancelListener(OnCancelListener listener) {
        super.setOnCancelListener(listener);
    }

    public interface EvaluateClickListener {
        void onEvaluateSubmit();

        void onEvaluateCanceled();
    }

    public enum EvaluateDialogType {
        ROBOT,
        STAR,
        STAR_MESSAGE
    }
}
