package com.example.moapp.moapp;

import android.net.LinkAddress;
import android.net.RouteInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
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

import android.app.usage.NetworkStatsManager;
import android.app.usage.NetworkStats;

import android.provider.Settings;
import android.content.Intent;
import android.os.RemoteException;
import android.app.AppOpsManager;
import android.os.Process;

import android.os.HandlerThread;
import android.os.Message;

import java.net.InetAddress;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "yumike";
    private static final Boolean LOG_ON_UI = false;

    private static final int MSG_TYPE_NETWORK = 1;
    private static final int MSG_TYPE_STATISTICS = 2;
    private static final int MSG_TYPE_DNS = 3;

    private TextView mTextMessage;
    private TextView mTextMessage2;
    private Button mBtn;
    private Button mBtn2;
    private Button mBtn3;

    private Context mCtx;
    private static ConnectivityManager mCM;
    private static ConnectivityManager.NetworkCallback mNetworkCallback;
    private static ConnectivityManager.NetworkCallback mNetworkCallback2;
    private static NetworkStatsManager.UsageCallback mNetworkStatsCallback;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private Handler mMainHandler;

    private static TelephonyManager mTM;
    private static NetworkStatsManager mNSM;

    private int cnt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myLog(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.txv1);
        mTextMessage.setMovementMethod(new ScrollingMovementMethod());

        mTextMessage2 = (TextView) findViewById(R.id.txv2);
        mTextMessage2.setMovementMethod(new ScrollingMovementMethod());

        mBtn = (Button) findViewById(R.id.btn1);
        mBtn.setOnClickListener(onClickToGetNetwork);

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setOnClickListener(onClickToGetStatistics);

        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setOnClickListener(onClickToQueryDns);

        mCM = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mTM = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mNSM = (NetworkStatsManager) getSystemService(Context.NETWORK_STATS_SERVICE);

        mNetworkCallback = new NetworkCallbackImpl();
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        NetworkRequest request = builder.build();
        mCM.registerNetworkCallback(request, mNetworkCallback);

        mNetworkStatsCallback = new NetworkStatsCallbackImpl();
        mNSM.registerUsageCallback(ConnectivityManager.TYPE_WIFI, "",
                100, mNetworkStatsCallback);

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
                switch (msg.what) {
                    case MSG_TYPE_NETWORK:
                        mTextMessage.setText(str);
                        break;
                    case MSG_TYPE_STATISTICS:
                        mTextMessage2.setText(str);
                        break;
                    case MSG_TYPE_DNS:
                        break;
                }

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
        updateUI(MSG_TYPE_NETWORK);
    }

    public String dumpNetwork() {
        Network[] networks = mCM.getAllNetworks();
        StringBuilder sb = new StringBuilder();
        for (Network nw : networks) {
            NetworkInfo ni = mCM.getNetworkInfo(nw);
            LinkProperties lp = mCM.getLinkProperties(nw);
            NetworkCapabilities nc = mCM.getNetworkCapabilities(nw);

            sb.append("Network ").append(nw.toString()).append("\n");

            StringBuilder networkInfo = sb.append("NetworkInfo").append("\n").
                    append("  DetailedState: ").append(ni.getDetailedState()).append("\n").
                    append("  Subtype: ").append(ni.getSubtype()).append("\n").
                    append("  SubtypeName: ").append(ni.getSubtypeName()).append("\n");

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
        return sb.toString();
    }

    public void updateUI(int msgType) {
        String str = null;
        switch (msgType) {
            case MSG_TYPE_NETWORK:
                str = dumpNetwork();
                break;
            case MSG_TYPE_STATISTICS:
                break;
            case MSG_TYPE_DNS:
                break;
        }

        Message msg = Message.obtain(mMainHandler);
        msg.obj = str;
        msg.what = msgType;
        msg.sendToTarget();
    }

    public String dumpBucket(NetworkStats.Bucket bucket) {
        StringBuilder sb = new StringBuilder();
        sb.append(",NetworkStatus:").append(bucket.getDefaultNetworkStatus()).
                append(",StartTime:").append(bucket.getStartTimeStamp()).
                append(",EndTime:").append(bucket.getEndTimeStamp()).
                append(",Rx:").append(bucket.getRxBytes()).
                append(",Tx:").append(bucket.getTxBytes()).
                append(",tag:").append(bucket.getTag()).
                append(",uid:").append(bucket.getUid()).append(" ").
                append(",state:").append(bucket.getState());
        return sb.toString();
    }

    public void QuerySummary() {
        myLog(TAG, "Starting QuerySummary");
        try {
            NetworkStats ns = mNSM.querySummary(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis());
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            if (!checkForPermission()) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
            do {
                ns.getNextBucket(bucket);
                myLog(TAG, dumpBucket(bucket));
            } while (ns.hasNextBucket());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void QuerySummaryForDevice() {
        myLog(TAG, "Starting QuerySummaryForDevice");
        try {
            NetworkStats.Bucket bucket = mNSM.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis());
            if (!checkForPermission()) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
            do {
                myLog(TAG, dumpBucket(bucket));
            } while (false);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public void QuerySummaryForUser() {
        myLog(TAG, "Starting QuerySummaryForUser");
        try {
            NetworkStats.Bucket bucket = mNSM.querySummaryForUser(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis());
            if (!checkForPermission()) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
            do {
                myLog(TAG, dumpBucket(bucket));
            } while (false);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void QueryDetails() {
        myLog(TAG, "Starting QueryDetails");
        try {
            NetworkStats ns = mNSM.queryDetails(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis());
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            if (!checkForPermission()) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
            do {
                ns.getNextBucket(bucket);
                myLog(TAG, dumpBucket(bucket));
            } while (ns.hasNextBucket());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void QueryDetailsForUid() {
        myLog(TAG, "Starting QueryDetailsForUid");
        NetworkStats ns = mNSM.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis(), 10105);
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        if (!checkForPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
        do {
            ns.getNextBucket(bucket);
            myLog(TAG, dumpBucket(bucket));
        } while (ns.hasNextBucket());
    }

    public void QueryDetailsForUidTag() {
        myLog(TAG, "Starting QueryDetailsForUidTag");
        NetworkStats ns = mNSM.queryDetailsForUidTag(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis(), 10105, NetworkStats.Bucket.TAG_NONE);
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        if (!checkForPermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
        do {
            ns.getNextBucket(bucket);
            myLog(TAG, dumpBucket(bucket));
        } while (ns.hasNextBucket());
    }

    private boolean checkForPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
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

    private class NetworkStatsCallbackImpl extends NetworkStatsManager.UsageCallback {
        @Override
        public void onThresholdReached(int i, String s) {
            myLog(TAG, "onThresholdReached: " + i + ", " + s);
        }
    }

    private View.OnClickListener onClickToGetNetwork = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ShowNetworkInformation();
            //updateUI();
        }
    };

    private View.OnClickListener onClickToGetStatistics = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            QuerySummary();
            QuerySummaryForDevice();
            QuerySummaryForUser();
            QueryDetails();
            QueryDetailsForUid();
            QueryDetailsForUidTag();
            //updateUI();
        }
    };


    private View.OnClickListener onClickToQueryDns = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        InetAddress[] addresses = InetAddress.getAllByName("google.com");
                        for (InetAddress addr : addresses) {
                            myLog(TAG, addr.toString());
                        }
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    };
}
