package pt.ulisboa.tecnico.cmov.ubibike;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

abstract class NetworkHandlerActivity extends JsonHandlerActivity {

    protected Handler mNetworkHandler;
    private BroadcastReceiver mNetworkBroadcastReceiver;
    private IntentFilter mNetworkIntentFilter;

    //Handle network availability
    protected boolean networkIsAvailable() {
        NetworkInfo activeInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return (activeInfo != null && activeInfo.isConnected());
    }

    protected class NetworkThread implements Runnable {

        private Handler _mHandler;

        public NetworkThread(Handler mHandler) {
            super();
            _mHandler = mHandler;
        }

        @Override
        public void run() {
            try {
                //Repeat until app is connected
                while (!networkIsAvailable()) {
                    Thread.sleep(1000);
                }

                //Send reconnect signal
                _mHandler.obtainMessage(NetworkMessageCode.RECONNECT_NETWORK.ordinal()).sendToTarget();
            } catch (InterruptedException exception) {
                _mHandler.obtainMessage(NetworkMessageCode.EXIT.ordinal(), exception.getMessage() + ".").sendToTarget();
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNetworkIntentFilter = new IntentFilter();
        mNetworkIntentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        mNetworkIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        mNetworkIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");

        mNetworkBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onNetworkChange();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Register network broadcast receiver and listener
        registerReceiver(mNetworkBroadcastReceiver, mNetworkIntentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();

        //Unregister network broadcast receiver and listener
        unregisterReceiver(mNetworkBroadcastReceiver);
    }

    protected void onNetworkChange() {
        if(networkIsAvailable()) {
            onDataConnected();
        } else {
            onDataDisconnected();
        }
    }

    protected void enableButtons(boolean option) {
        //By default, nothing happens
    }

    protected void onDataDisconnected() {
        //Disable buttons
        enableButtons(false);

        //Print error
        TextView noNetView = (TextView) findViewById(R.id.noNetView);
        noNetView.setBackgroundResource(android.R.color.holo_red_dark);
        noNetView.setText(R.string.onDataDisconnected);

        //Start network thread
        new Thread(new NetworkThread(mNetworkHandler)).start();
    }

    protected void onDataConnected() {
        //Enable buttons
        enableButtons(true);

        //Remove error
        TextView noNetView = (TextView) findViewById(R.id.noNetView);
        noNetView.setBackgroundResource(android.R.color.transparent);
        noNetView.setText(R.string.onDataConnected);
    }
}
