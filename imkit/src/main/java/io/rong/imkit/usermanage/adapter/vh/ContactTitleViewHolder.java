package io.rong.imkit.usermanage.adapter.vh;

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import io.rong.imkit.model.ContactModel;

public class ContactTitleViewHolder extends RecyclerView.ViewHolder {

    private final TextView catalogLetterTextView;

    public ContactTitleViewHolder(@NonNull View itemView) {
        super(itemView);
        catalogLetterTextView = itemView.findViewById(R.id.tv_catalog_letter);
    }

    public void bind(ContactModel<String> characterTitleInfoContactModel) {
        catalogLetterTextView.setText(characterTitleInfoContactModel.getBean());
    }
}
