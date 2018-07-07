package com.example.moapp.moapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.net.LinkProperties;
import android.net.Network;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "yumike";
    private static final Boolean LOG_ON_UI = false;

    private TextView mTextMessage;

    private Context mCtx;
    private static ConnectivityManager mCM;
    private static ConnectivityManager.NetworkCallback mNetworkCallback;
    private static ConnectivityManager.NetworkCallback mNetworkCallback2;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myLog(TAG, "onCreate");
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myLog(TAG, "onResume");

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mCM = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mNetworkCallback = new NetworkCallbackImpl();
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        NetworkRequest request = builder.build();
        mCM.registerNetworkCallback(request, mNetworkCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        myLog(TAG, "onStop");
        //mCM.unregisterNetworkCallback(mNetworkCallback);
    }

    public void myLog(String tag, String msg) {
        Log.i(tag, msg);
        if (LOG_ON_UI) {
            if (mTextMessage != null) {
                mTextMessage.append(msg + "\n");
            }
        }
    }


    public void ShowNetworkInformation() {
        Network[] networks = mCM.getAllNetworks();
        for (Network nw : networks) {
            NetworkInfo ni = mCM.getNetworkInfo(nw);
            //myLog(TAG, ni.toString());
        }
    }

    private class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            myLog(TAG, "onAvailable: " + network.toString());
            ShowNetworkInformation();
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            myLog(TAG, "onLost: " + network.toString());
            ShowNetworkInformation();
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            myLog(TAG, "onCapabilitiesChanged: " + networkCapabilities.toString());
            ShowNetworkInformation();
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties);
            myLog(TAG, "onLinkPropertiesChanged: " + linkProperties.toString());
            ShowNetworkInformation();
        }
    }
}
