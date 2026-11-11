package com.netfieldpro.latency;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.RouteInfo;

import androidx.annotation.NonNull;

import java.net.Inet4Address;
import java.net.InetAddress;

public class WifiNetworkTracker {

    public interface Listener {
        void onWifiNetworkChanged(Network network, String gateway, String dns);
    }

    private final ConnectivityManager connectivityManager;
    private Listener listener;
    private boolean isRegistered = false;

    private Network currentNetwork;
    private String currentGateway;
    private String currentDns;

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            synchronized (WifiNetworkTracker.this) {
                currentNetwork = network;
                updateLinkProperties(network);
                notifyListener();
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            synchronized (WifiNetworkTracker.this) {
                if (network.equals(currentNetwork)) {
                    currentNetwork = null;
                    currentGateway = null;
                    currentDns = null;
                    notifyListener();
                }
            }
        }

        @Override
        public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
            synchronized (WifiNetworkTracker.this) {
                if (network.equals(currentNetwork)) {
                    extractRoutesAndDns(linkProperties);
                    notifyListener();
                }
            }
        }
    };

    public WifiNetworkTracker(Context context) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public synchronized void setListener(Listener listener) {
        this.listener = listener;
    }

    public synchronized void startTracking() {
        if (isRegistered || connectivityManager == null) {
            return;
        }
        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
            isRegistered = true;

            Network active = connectivityManager.getActiveNetwork();
            if (active != null) {
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    currentNetwork = active;
                    updateLinkProperties(active);
                }
            }
            notifyListener();
        } catch (Exception e) {
            // Handled gracefully in environments without full network capabilities
        }
    }

    public synchronized void stopTracking() {
        if (!isRegistered || connectivityManager == null) {
            return;
        }
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Exception ignored) {
        }
        isRegistered = false;
        currentNetwork = null;
        currentGateway = null;
        currentDns = null;
    }

    public synchronized Network getCurrentNetwork() {
        return currentNetwork;
    }

    public synchronized String getGateway() {
        return currentGateway;
    }

    public synchronized String getDns() {
        return currentDns;
    }

    public synchronized boolean isConnected() {
        return currentNetwork != null && currentGateway != null;
    }

    private void updateLinkProperties(Network network) {
        if (connectivityManager == null || network == null) {
            return;
        }
        LinkProperties lp = connectivityManager.getLinkProperties(network);
        if (lp != null) {
            extractRoutesAndDns(lp);
        }
    }

    private void extractRoutesAndDns(LinkProperties lp) {
        currentGateway = null;
        currentDns = null;

        for (RouteInfo route : lp.getRoutes()) {
            if (route.isDefaultRoute()) {
                InetAddress gw = route.getGateway();
                if (gw instanceof Inet4Address) {
                    currentGateway = gw.getHostAddress();
                    break;
                }
            }
        }

        for (InetAddress dns : lp.getDnsServers()) {
            if (dns instanceof Inet4Address) {
                currentDns = dns.getHostAddress();
                break;
            }
        }
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onWifiNetworkChanged(currentNetwork, currentGateway, currentDns);
        }
    }
}
