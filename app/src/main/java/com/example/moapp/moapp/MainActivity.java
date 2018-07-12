package com.example.moapp.moapp;

import android.net.LinkAddress;
import android.net.RouteInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.text.method.ScrollingMovementMethod;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.net.LinkProperties;
import android.net.Network;

import android.os.HandlerThread;
import android.os.Message;

import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "yumike";
    private static final Boolean LOG_ON_UI = false;

    private TextView mTextMessage;
    private Button mBtn;

    private Context mCtx;
    private static ConnectivityManager mCM;
    private static ConnectivityManager.NetworkCallback mNetworkCallback;
    private static ConnectivityManager.NetworkCallback mNetworkCallback2;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private Handler mMainHandler;

    private int cnt = 0;

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


        mTextMessage = (TextView) findViewById(R.id.txv1);
        mTextMessage.setMovementMethod(new ScrollingMovementMethod());
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        //navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mBtn = (Button) findViewById(R.id.btn1);
        mBtn.setOnClickListener(onClickToGetNetwork);

        mCM = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mNetworkCallback = new NetworkCallbackImpl();
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        NetworkRequest request = builder.build();
        mCM.registerNetworkCallback(request, mNetworkCallback);

        mHandlerThread = new HandlerThread("myHandlerThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                String str = (String) msg.obj;
                myLog(TAG, "msg.obj " + str);
                ShowNetworkInformation();
                mHandler.sendEmptyMessageDelayed(1, 3000);
                Message msg2 = Message.obtain(msg) ;
                //msg2.setTarget(mMainHandler);
                msg2.obj = String.valueOf(cnt++);
                mMainHandler.sendMessage(msg2);
            }
        };

        Message msg = Message.obtain(mHandler);
        msg.obj = String.valueOf(cnt);
        //msg.sendToTarget();

        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                String str = (String) msg.obj;
                myLog(TAG, "setText " + str);
                mTextMessage.setText(str);
            }
        };

    }

    @Override
    protected void onResume() {
        super.onResume();
        myLog(TAG, "onResume");

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
            LinkProperties lp = mCM.getLinkProperties(nw);
            NetworkCapabilities nc = mCM.getNetworkCapabilities(nw);
            myLog(TAG, nw != null? nw.toString() : "null");
            myLog(TAG, ni != null? ni.toString() : "null");
            myLog(TAG, lp != null? lp.toString() : "null");
            myLog(TAG, nc != null? nc.toString() : "null");
        }
        updateUI();
    }

    public void updateUI() {
        Network[] networks = mCM.getAllNetworks();
        StringBuilder sb = new StringBuilder();

        for (Network nw : networks) {
            NetworkInfo ni = mCM.getNetworkInfo(nw);
            LinkProperties lp = mCM.getLinkProperties(nw);
            NetworkCapabilities nc = mCM.getNetworkCapabilities(nw);

            sb.append("Network ").append(nw.toString()).append("\n");

            StringBuilder networkInfo = sb.append("NetworkInfo").append("\n").
                    append("  DetailedState: ").append(ni.getDetailedState()).append("\n");

            /*
            sb.append("NetworkCapabilities").append("\n").
                    append(nc.toString()).append("\n");
            */

            String linkAddresses = "";
            for (LinkAddress addr : lp.getLinkAddresses()) linkAddresses += "  LinkAddresses: " + addr.toString() + "\n";

            String routes = "";
            for (RouteInfo route : lp.getRoutes()) routes += "  Routes: " + route.toString() + "\n";

            String dns = "";
            for (InetAddress addr : lp.getDnsServers()) dns += "  DnsServers: " + addr.toString() + "\n";


            sb.append("LinkProperties").append("\n").
                    append("  Interface: ").append(lp.getInterfaceName()).append("\n").
                    append(linkAddresses).
                    append(routes).
                    append(dns);

            sb.append("\n");
        }

        Message msg = Message.obtain(mMainHandler);
        msg.obj = sb.toString();
        msg.sendToTarget();
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

    private View.OnClickListener onClickToGetNetwork = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ShowNetworkInformation();
            //updateUI();
        }
    };

}
