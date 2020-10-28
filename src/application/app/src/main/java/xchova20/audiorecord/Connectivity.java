package xchova20.audiorecord;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

/**
 * Connectivity manager.
 * Used for checking and manipulating internet connection status.
 */
public class Connectivity {
    /**
     * Connectivity constructor.
     *
     * @param context application context
     */
    public Connectivity(Context context) {
        this.context = context;
    }

    /**
     * Application context
     */
    private Context context;

    /**
     * Turn the Wifi functionality on.
     *
     * @return true if Wifi is already enabled, false if not
     */
    public boolean turnWifiOn() {
        WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()){
            wifi.setWifiEnabled(true);
            return false;
        }
        return true;
    }

    /**
     * Detection of Wifi being on/off.
     *
     * @return true if Wifi is on, false if not
     */
    public boolean isWifiOn() {
        WifiManager wifi =(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        return wifi.isWifiEnabled();
    }

    /**
     * Check for general internet connectivity (does not need to be from Wifi)
     *
     * @return true if connected to the internet, false if not
     */
    public boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
