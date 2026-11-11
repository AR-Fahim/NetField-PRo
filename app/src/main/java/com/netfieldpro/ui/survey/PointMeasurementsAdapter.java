package com.netfieldpro.ui.survey;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.netfieldpro.R;
import com.netfieldpro.core.util.FixedDecimal;
import com.netfieldpro.data.db.entity.MeasurementRow;

import java.util.ArrayList;
import java.util.List;

public class PointMeasurementsAdapter extends RecyclerView.Adapter<PointMeasurementsAdapter.ViewHolder> {

    private final List<MeasurementRow> items = new ArrayList<>();

    public void setItems(List<MeasurementRow> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_point_bssid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeasurementRow m = items.get(position);
        holder.textSsid.setText(m.ssid != null && !m.ssid.isEmpty() ? m.ssid : "(hidden)");
        holder.textBssid.setText(m.bssidOrId);
        String rssiText = FixedDecimal.format(m.rssiMedian, 1) + " dBm";
        holder.textRssi.setText(rssiText);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textSsid;
        final TextView textBssid;
        final TextView textRssi;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textSsid = itemView.findViewById(R.id.text_item_ssid);
            textBssid = itemView.findViewById(R.id.text_item_bssid);
            textRssi = itemView.findViewById(R.id.text_item_rssi);
        }
    }
}

