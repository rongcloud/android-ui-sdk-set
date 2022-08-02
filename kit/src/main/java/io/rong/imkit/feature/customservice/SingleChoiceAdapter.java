package io.rong.imkit.feature.customservice;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;

public class SingleChoiceAdapter<T> extends BaseAdapter {

    private List<T> mObjects = new ArrayList<>();
    private int mCheckBoxResourceID = 0;
    private int mSelectItem = -1;

    private LayoutInflater mInflater;

    public SingleChoiceAdapter(Context context, int checkBoxResourceId) {
        init(context, checkBoxResourceId);
    }

    public SingleChoiceAdapter(Context context, List<T> objects,
                               int checkBoxResourceId) {
        init(context, checkBoxResourceId);
        if (objects != null) {
            mObjects = objects;
        }

    }

    private void init(Context context, int checkBoResourceId) {
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCheckBoxResourceID = checkBoResourceId;
    }

    public void refreshData(List<T> objects) {
        if (objects != null) {
            mObjects = objects;
            setSelectItem(0);
        }
    }

    public void setSelectItem(int selectItem) {
        if (selectItem >= 0 && selectItem < mObjects.size()) {
            mSelectItem = selectItem;
            notifyDataSetChanged();
        }

    }

    public int getSelectItem() {
        return mSelectItem;
    }

    public void clear() {
        mObjects.clear();
        notifyDataSetChanged();
    }

    public int getCount() {
        return mObjects.size();
    }

    public T getItem(int position) {
        return mObjects.get(position);
    }

    public int getPosition(T item) {
        return mObjects.indexOf(item);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.rc_cs_item_single_choice,
                    null);
            viewHolder = new ViewHolder();
            viewHolder.mTextView = convertView
                    .findViewById(R.id.rc_cs_tv_group_name);
            viewHolder.mCheckBox = convertView
                    .findViewById(R.id.rc_cs_group_checkBox);
            convertView.setTag(viewHolder);

            if (mCheckBoxResourceID != 0) {
                viewHolder.mCheckBox.setButtonDrawable(mCheckBoxResourceID);
            }

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.mCheckBox.setChecked(mSelectItem == position);

        T item = getItem(position);
        if (item instanceof CharSequence) {
            viewHolder.mTextView.setText((CharSequence) item);
        } else {
            viewHolder.mTextView.setText(item.toString());
        }

        return convertView;
    }

    public static class ViewHolder {
        public TextView mTextView;
        public CheckBox mCheckBox;
    }

}
