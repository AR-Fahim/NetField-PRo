package com.netfieldpro.wifi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.net.wifi.ScanResult;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.netfieldpro.core.wifi.ScanBatch;
import com.netfieldpro.core.wifi.Tier;
import com.netfieldpro.core.wifi.WifiObservation;

@RunWith(AndroidJUnit4.class)
public class ScanResultMapperTest {

    private ScanResult createScanResult(String bssid, String ssid, int freq, int level, long timestamp) {
        ScanResult sr = new ScanResult();
        sr.BSSID = bssid;
        sr.SSID = ssid;
        sr.frequency = freq;
        sr.level = level;
        sr.timestamp = timestamp;
        return sr;
    }

    @Test
    public void testMAP1_scanResultMapsToExpectedObservation() {
        ScanResult sr = createScanResult("AA:BB:CC:DD:EE:01", "eduroam", 5180, -58, 123456L);
        WifiObservation obs = ScanResultMapper.toObservation(sr);

        assertNotNull(obs);
        assertEquals("aa:bb:cc:dd:ee:01", obs.bssid());
        assertEquals("eduroam", obs.ssid());
        assertEquals(5180, obs.freqMHz());
        assertEquals(-58, obs.rssiDbm());
        assertEquals(123456000L, obs.timestampNs());
    }

    @Test
    public void testMAP2_invalidBssidIsDroppedAndCounted() {
        ScanResult validSr = createScanResult("AA:BB:CC:DD:EE:01", "eduroam", 5180, -58, 123456L);
        ScanResult invalidSr = createScanResult("02:00:00:00:00:00", "eduroam", 5180, -58, 123456L);

        long arrival = 123456000L + 1000L;
        ScanBatch batch = ScanResultMapper.map(List.of(validSr, invalidSr), arrival, Tier.CALLBACK, true);

        assertEquals(1, batch.rows().size());
        assertEquals("aa:bb:cc:dd:ee:01", batch.rows().get(0).bssid());
        assertEquals(1, batch.droppedInvalid());
    }
}
