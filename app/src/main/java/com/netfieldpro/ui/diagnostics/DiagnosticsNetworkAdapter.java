package com.netfieldpro.ui.diagnostics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import com.netfieldpro.R;
import com.netfieldpro.core.wifi.Band;
import com.netfieldpro.core.wifi.ScanListModel;

public class DiagnosticsNetworkAdapter extends RecyclerView.Adapter<DiagnosticsNetworkAdapter.ViewHolder> {

    private List<ScanListModel.Row> items = Collections.emptyList();

    public void setItems(List<ScanListModel.Row> items) {
        this.items = (items != null) ? items : Collections.emptyList();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_diag_network, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View root;
        private final TextView textSsid;
        private final TextView textRssi;
        private final TextView textMeta;
        private final TextView chipConnected;
        private final TextView textBssid;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.root = itemView.findViewById(R.id.item_diag_network_root);
            this.textSsid = itemView.findViewById(R.id.text_diag_network_ssid);
            this.textRssi = itemView.findViewById(R.id.text_diag_network_rssi);
            this.textMeta = itemView.findViewById(R.id.text_diag_network_meta);
            this.chipConnected = itemView.findViewById(R.id.chip_diag_connected);
            this.textBssid = itemView.findViewById(R.id.text_diag_network_bssid);
        }

        void bind(ScanListModel.Row row) {
            Context ctx = itemView.getContext();
            String ssidDisplay = (row.ssidDisplay() != null)
                    ? row.ssidDisplay()
                    : ctx.getString(R.string.diag_hidden_ssid);

            textSsid.setText(ssidDisplay);
            textRssi.setText(row.rssiDbm() + " dBm");

            String bandStr = bandToString(ctx, row.band());
            textMeta.setText(bandStr + " · ch " + row.channel());

            if (row.isConnected()) {
                chipConnected.setVisibility(View.VISIBLE);
            } else {
                chipConnected.setVisibility(View.GONE);
            }

            textBssid.setText(row.bssid());

            String desc = ctx.getString(R.string.diag_row_desc, ssidDisplay, row.rssiDbm(), bandStr, row.channel());
            root.setContentDescription(desc);
        }

        private String bandToString(Context ctx, Band band) {
            if (band == null) {
                return ctx.getString(R.string.diag_band_unknown);
            }
            return switch (band) {
                case B24 -> ctx.getString(R.string.diag_band_24);
                case B5 -> ctx.getString(R.string.diag_band_5);
                case B6 -> ctx.getString(R.string.diag_band_6);
                case UNKNOWN -> ctx.getString(R.string.diag_band_unknown);
            };
        }
    }
}

