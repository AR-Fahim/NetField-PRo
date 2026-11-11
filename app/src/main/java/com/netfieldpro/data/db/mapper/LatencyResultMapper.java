package com.netfieldpro.data.db.mapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.netfieldpro.core.json.CanonicalJsonWriter;
import com.netfieldpro.core.json.JsonArray;
import com.netfieldpro.core.json.JsonParser;
import com.netfieldpro.core.json.JsonString;
import com.netfieldpro.core.json.JsonValue;
import com.netfieldpro.core.model.entity.LatencyResult;
import com.netfieldpro.core.model.enums.LatencyMethod;
import com.netfieldpro.core.model.enums.LatencyScope;
import com.netfieldpro.core.util.FixedDecimal;
import com.netfieldpro.data.db.entity.LatencyResultRow;

public final class LatencyResultMapper {

    private LatencyResultMapper() {
    }

    public static LatencyResultRow toRow(LatencyResult lr) {
        LatencyResultRow row = new LatencyResultRow();
        row.id = lr.id();
        row.pointId = lr.pointId();
        row.method = lr.method().name();
        row.target = lr.target();
        row.boundIface = lr.boundIface();
        row.scope = lr.scope().name();
        row.sent = lr.sent();
        row.recv = lr.recv();
        row.minMs = lr.minMs();
        row.avgMs = lr.avgMs();
        row.maxMs = lr.maxMs();
        row.jitterMs = lr.jitterMs();
        row.lossCount = lr.lossCount();
        row.duringScan = lr.duringScan();
        row.connectedBssid = lr.connectedBssid();
        row.ssid = lr.ssid();

        JsonArray arr = new JsonArray();
        for (Double rtt : lr.rtts()) {
            arr.add(new JsonString(FixedDecimal.format(rtt, 3)));
        }
        row.rtts = new String(CanonicalJsonWriter.toBytes(arr), StandardCharsets.UTF_8);
        return row;
    }

    public static LatencyResult toDomain(LatencyResultRow row) {
        try {
            JsonArray arr = (JsonArray) JsonParser.parse(row.rtts.getBytes(StandardCharsets.UTF_8));
            List<Double> rtts = new ArrayList<>();
            for (JsonValue v : arr.elements()) {
                rtts.add(Double.parseDouble(((JsonString) v).value()));
            }

            return new LatencyResult(
                    row.id, row.pointId, LatencyMethod.valueOf(row.method),
                    row.target, row.boundIface, LatencyScope.valueOf(row.scope),
                    row.sent, row.recv, row.minMs, row.avgMs, row.maxMs,
                    row.jitterMs, row.lossCount, row.duringScan,
                    row.connectedBssid, row.ssid, rtts
            );
        } catch (Exception e) {
            throw new RuntimeException("Error mapping LatencyResultRow to domain", e);
        }
    }
}

