package io.rong.imkit.usermanage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.adapter.vh.ContactSelectableViewHolder;
import io.rong.imkit.usermanage.adapter.vh.ContactTitleViewHolder;
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 联系人列表适配器
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class ContactListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ContactModel> data = new ArrayList<>();
    private OnActionClickListener<ContactModel> onItemClickListener;
    private OnActionClickListener<ContactModel> onItemRemoveClickListener;
    private final boolean showSelectButton;
    private final boolean showItemRightArrow;
    private final boolean showItemRightText;
    private final boolean showItemSelectAutoUpdate;
    private boolean showItemRemoveButton;

    public ContactListAdapter(
            boolean showSelectButton,
            boolean showItemRightArrow,
            boolean showItemRightText,
            boolean showItemSelectAutoUpdate,
            boolean showItemRemoveButton) {
        this.showSelectButton = showSelectButton;
        this.showItemRightArrow = showItemRightArrow;
        this.showItemRightText = showItemRightText;
        this.showItemSelectAutoUpdate = showItemSelectAutoUpdate;
        this.showItemRemoveButton = showItemRemoveButton;
    }

    public void setOnItemClickListener(OnActionClickListener<ContactModel> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setOnItemRemoveClickListener(OnActionClickListener<ContactModel> listener) {
        onItemRemoveClickListener = listener;
    }

    public void setData(List<ContactModel> newData) {
        if (newData == null) {
            newData = new CopyOnWriteArrayList<>();
        }
        this.data = new CopyOnWriteArrayList<>(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(viewType, parent, false);

        if (viewType == R.layout.rc_item_contact_selectable) {
            return new ContactSelectableViewHolder(
                    itemView,
                    onItemClickListener,
                    onItemRemoveClickListener,
                    showSelectButton,
                    showItemRightArrow,
                    showItemRightText,
                    showItemSelectAutoUpdate,
                    showItemRemoveButton);
        } else if (viewType == R.layout.rc_item_contact_title) {
            return new ContactTitleViewHolder(itemView);
        } else {
            throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ContactModel contactModel = data.get(position);
        if (holder instanceof ContactSelectableViewHolder) {
            ((ContactSelectableViewHolder) holder).setShowItemRemoveButton(showItemRemoveButton);
            ((ContactSelectableViewHolder) holder).bind(contactModel);
        } else if (holder instanceof ContactTitleViewHolder) {
            ((ContactTitleViewHolder) holder).bind(contactModel);
        }
    }

    @Override
    public int getItemViewType(int position) {
        ContactModel<?> contactModel = data.get(position);
        return contactModel.getContactType() == ContactModel.ItemType.CONTENT
                ? R.layout.rc_item_contact_selectable
                : R.layout.rc_item_contact_title;
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public int getPositionForSection(char section) {
        for (int i = 0; i < data.size(); i++) {
            ContactModel<?> contactModel = data.get(i);
            if (contactModel.getContactType() == ContactModel.ItemType.TITLE) {
                if (((String) contactModel.getBean()).charAt(0) == section) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void setShowItemRemoveButton(boolean showItemRemoveButton) {
        this.showItemRemoveButton = showItemRemoveButton;
        notifyDataSetChanged();
    }
}
