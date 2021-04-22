package io.rong.imkit.widget.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import io.rong.imkit.R;


public class OptionsPopupDialog extends AlertDialog {

    private Context mContext;
    private String[] arrays;
    private OnOptionsItemClickedListener mItemClickedListener;

    public static OptionsPopupDialog newInstance(final Context context, String[] arrays) {
        return new OptionsPopupDialog(context, arrays);
    }

    public OptionsPopupDialog(final Context context, String[] arrays) {
        super(context);
        mContext = context;
        this.arrays = arrays;
    }

    @Override
    protected void onStart() {
        super.onStart();
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.rc_dialog_popup_options, null);
        ListView mListView = view.findViewById(R.id.rc_list_dialog_popup_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, R.layout.rc_dialog_popup_options_item,
                R.id.rc_dialog_popup_item_name, arrays);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mItemClickedListener != null) {
                    mItemClickedListener.onOptionsItemClicked(position);
                    dismiss();
                }
            }
        });
        setContentView(view);
        if (getWindow() == null) {
            return;
        }
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.width = getPopupWidth();
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(layoutParams);
    }

    public OptionsPopupDialog setOptionsPopupDialogListener(OnOptionsItemClickedListener itemListener) {
        this.mItemClickedListener = itemListener;
        return this;
    }

    public interface OnOptionsItemClickedListener {
        void onOptionsItemClicked(int which);
    }

    private int getPopupWidth() {
        int distanceToBorder = (int) mContext.getResources().getDimension(R.dimen.rc_dialog_margin_to_edge);
        return getScreenWidth() - 2 * (distanceToBorder);
    }

    private int getScreenWidth() {
        return ((WindowManager) (mContext.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getWidth();
    }

    @Override
    public void show() {
        if (mContext instanceof Activity) {
            Activity activity = (Activity) mContext;
            if (activity.isFinishing()) {
                return;
            }
        }
        super.show();
    }
}
