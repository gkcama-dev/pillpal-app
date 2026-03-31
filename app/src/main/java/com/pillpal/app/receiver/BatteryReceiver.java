package com.pillpal.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BatteryReceiver extends BroadcastReceiver {

    public interface BatteryListener {
        void onBatteryLow();
    }
    private BatteryListener listener;

    public BatteryReceiver(BatteryListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
            if (listener != null) {
                listener.onBatteryLow();
            }
        }
    }

}
