package com.netfieldpro.ui.survey;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.netfieldpro.R;

import java.util.ArrayList;
import java.util.List;

public class WarmupNetworksAdapter extends RecyclerView.Adapter<WarmupNetworksAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(WarmupCoordinator.NetworkGroup item);
    }

    private final List<WarmupCoordinator.NetworkGroup> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public WarmupNetworksAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<WarmupCoordinator.NetworkGroup> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_warmup_network, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WarmupCoordinator.NetworkGroup item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCheckBox checkBox;
        private final TextView textSsid;
        private final TextView textInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.check_network);
            textSsid = itemView.findViewById(R.id.text_network_ssid);
            textInfo = itemView.findViewById(R.id.text_network_info);
        }

        void bind(WarmupCoordinator.NetworkGroup item, OnItemClickListener listener) {
            String displayName = item.ssid().isEmpty()
                    ? itemView.getContext().getString(R.string.s06_hidden_ssid)
                    : item.ssid();
            textSsid.setText(displayName);

            String apLabel = item.bssidCount() == 1 ? "1 AP" : item.bssidCount() + " APs";
            String info = apLabel + ", " + item.maxRssiDbm() + " dBm";
            if (item.isConnected()) {
                info += " (" + itemView.getContext().getString(R.string.diag_connected) + ")";
            }
            textInfo.setText(info);

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.isSelected());

            View.OnClickListener clickListener = v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            };
            itemView.setOnClickListener(clickListener);
            checkBox.setOnClickListener(clickListener);
        }
    }
}

