package com.application.quickuse.wlan;

/**
 * Created by guangkai on 16-10-28.
 */

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainWLANActivity extends AppCompatActivity {

    private String TAG = "MainWLANActivity";
    private String WLAN_OFF = "OFF";
    private String WLAN_ON = "ON";
    private String OPEN_WLAN = "[ESS]";
    private String WiFi_Password;
    private String SSID_SECTION = "NULL";
    private String WLAN_ENCRYPTION = "NULL";

    private TextView status_wlan;
    private TextView wifiConnectStatus;
    private TextView wifiWlanEncryption;

    private WifiConfiguration mWifiConfig;

    private WifiInfo mCurrentWifiNetwork;

    private Switch mSwitch_wlan;
    private WifiAdmin mWifiAdmin;

    private boolean THE_LAYOUT_ISVISIBLE = true;

    private int networkId;
    private int[] encryptimageId;

    //定义一个WifiManager对象
    private WifiManager mWifiManager;
    private IntentFilter mWifiStateFilter;

    private Map<String, Object> listmap;
    private List<Map<String, Object>> listItems;
    private ListView listview;
    private List<ScanResult> mScanResultlist;

    private SimpleAdapter listadapter;


    public int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate enter");

        //加载主布局文件
        setContentView(R.layout.main_activity_wlan);

        mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiAdmin = new WifiAdmin(MainWLANActivity.this);

        //初始化变量
        initTheVariable();

        //刚打开Wifi时,设置显示的wifi的状态
        /*本段代码的功能在onStart中已经实现,不需要重复操作,如果onStart代码功能被去掉此处将会被放开使用
        if (WLAN_STATUS_ON == mWifiAdmin.checkState()) {
            Log.d(TAG, "Wifi Default Status Show ON");
            status_wlan.setText(WLAN_ON);

            showWifiList();
        } else if (WLAN_STATUS_OFF == mWifiAdmin.checkState()) {
            Log.d(TAG, "Wifi Default Status Show OFF");
            status_wlan.setText(WLAN_OFF);
        }
        */

        //设置Switch Button的监听器
        mSwitch_wlan.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                if (isChecked) {
                    //打开WLAN
                    Log.d(TAG, "onCheckedChanged: Wifi isChecked : " + isChecked);
                    if (mWifiManager.setWifiEnabled(true)) {

                        //注册广播服务
                        Log.d(TAG, "onCheckedChanged registerReceiver");
                        registerReceiver(mWifiStateReceiver, mWifiStateFilter);

                        status_wlan.setText(WLAN_ON);

                        //开始扫描
                        mWifiAdmin.startScan();

                        //显示Ｗｉｆｉ列表
                        showWifiList();
                    } else {
                        Log.d(TAG, "onCheckedChanged: Open Wifi false");
                        status_wlan.setText(WLAN_OFF);
                        mSwitch_wlan.setChecked(false);
                    }

                } else {
                    if (WifiManager.WIFI_STATE_ENABLED == mWifiManager.getWifiState()) {
                        //取消广播注册
                        Log.d(TAG, "onCheckedChanged unregisterReceiver");
                        unregisterReceiver(mWifiStateReceiver);
                    }

                    // 关闭WLAN
                    Log.d(TAG, "onCheckedChanged: Wifi isChecked : " + isChecked);
                    if (mWifiManager.setWifiEnabled(false)) {

                        status_wlan.setText(WLAN_OFF);

                        //清除关闭Wifi后的AP列表
                        //listadapter.clear();
                        listview.setAdapter(null);
                    } else {

                        Log.d(TAG, "onCheckedChanged: Close Wifi false");
                        status_wlan.setText(WLAN_ON);

                        //注册广播服务
                        Log.d(TAG, "onCheckedChanged closeWifi registerReceiver");
                        registerReceiver(mWifiStateReceiver, mWifiStateFilter);

                        mSwitch_wlan.setChecked(true);
                    }

                }
            }
        });

        //设置TextView的监听器
        status_wlan.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (WifiManager.WIFI_STATE_DISABLED == mWifiManager.getWifiState()) {
                    Log.d(TAG, "TextView Set ON");
                    status_wlan.setText(WLAN_ON);
                    mSwitch_wlan.setChecked(true);
                } else if (WifiManager.WIFI_STATE_ENABLED == mWifiManager.getWifiState()) {

                    Log.d(TAG, "TextView Set OFF");
                    status_wlan.setText(WLAN_OFF);
                    mSwitch_wlan.setChecked(false);
                }

            }
        });


        //设置点击wifi(ListView)监听器
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                HashMap item = (HashMap) arg0.getItemAtPosition(arg2);
                //get每一行的数据的ssid
                SSID_SECTION = String.valueOf(item.get("ssid").toString());
                Log.i(TAG, "SSID is " + SSID_SECTION);

                WLAN_ENCRYPTION = String.valueOf(item.get("wlan_encryption").toString());
                Log.i(TAG, "ENCRYPTION is " + WLAN_ENCRYPTION);


                ShowWifiDialog();
            }
        });

        Log.d(TAG, "onCreate out");
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart enter");

        if (WifiManager.WIFI_STATE_ENABLED == mWifiManager.getWifiState()) {
            Log.d(TAG, "TextView keep ON");
            status_wlan.setText(WLAN_ON);

            //保持WLAN开启状态
            //mSwitch_wlan.toggle();//不用此方法的原因是当切换Activity焦点时将自动改变wifi状态true-->false,false-->true
            mSwitch_wlan.setChecked(true);

        } else if (WifiManager.WIFI_STATE_DISABLED == mWifiManager.getWifiState()) {

            Log.d(TAG, "TextView keep OFF");
            status_wlan.setText(WLAN_OFF);

            //保持WLAN关闭状态
            //mSwitch_wlan.toggle();//不用此方法的原因是当切换Activity焦点时将自动改变wifi状态true-->false,false-->true
            mSwitch_wlan.setChecked(false);

        }
        Log.d(TAG, "onStart out");
    }

    public void initTheVariable() {

        Log.d(TAG, "init enter");
        //定义广播消息
        mWifiStateFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        /*暂时不需要
        mWifiStateFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        */

        status_wlan = (TextView) findViewById(R.id.status_wlan);
        mSwitch_wlan = (Switch) findViewById(R.id.switch_wlan);

        //显示WiFiList
        listview = (ListView) findViewById(R.id.listView);

        encryptimageId = new int[]{
                R.drawable.stat_sys_wifi_signal_0,
                R.drawable.stat_sys_wifi_signal_1,
                R.drawable.stat_sys_wifi_signal_2,
                R.drawable.stat_sys_wifi_signal_3,
                R.drawable.stat_sys_wifi_signal_4,
                R.drawable.ic_wifi_signal_0_dark,
                R.drawable.ic_wifi_signal_1_dark,
                R.drawable.ic_wifi_signal_2_dark,
                R.drawable.ic_wifi_signal_3_dark,
                R.drawable.ic_wifi_signal_4_dark};

        listItems = new ArrayList<Map<String, Object>>();

        listadapter = new SimpleAdapter(this, listItems,
                R.layout.wifi_items, new String[]{"ssid", "rssiimage", "wlan_encryption", "wlan_Rssi"}, new int[]{
                R.id.ssid, R.id.rssiimage, R.id.wlan_encryption, R.id.wlan_Rssi});

        //设置wlan_connect_status控件属性隐藏<很遗憾暂未实现>
        //final LinearLayout inflater= (LinearLayout) getLayoutInflater().inflate(R.layout.wifi_items,null);
        //wifiWlanEncryption = (TextView) inflater.findViewById(R.id.wlan_encryption);
        //wifiWlanEncryption.setVisibility(View.GONE);

        Log.d(TAG, "init out");
    }

    //将Wifi列表显示出来
    public void showWifiList() {

        Log.d(TAG, "showWifiList enter");

        //每次显示前讲listItem清空,不然会出现累加的方式显示AP
        listItems.clear();

        //判断Wifi是否具有运行时权限,如果没有,通过onRequestPermissionsResult获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
        } else {
            //获取Wifi扫描结果
            //do something, permission was previously granted; or legacy device
            mScanResultlist = mWifiManager.getScanResults();
        }

        //使用此方式删除一些信号强度低于-60的AP
        for (Iterator<ScanResult> it = mScanResultlist.iterator(); it.hasNext(); ) {
            ScanResult mScanResult = it.next();
            if (mScanResult.level < -60) {
                it.remove();
            }
        }

        //将获取到的Wifi按照信号由强到弱进行排序
        sortByLevel(mScanResultlist);

        WifiInfo mCurrentWifi = mWifiManager.getConnectionInfo();

        //循环的将要显示的Wifi信号强度图片与信号强度进行匹配
        for (ScanResult mScanResult : mScanResultlist) {

            //每一个Ｗｉｆｉ都需要一个ｌｉｓｔｍap用来装载每一个AP的具体信息
            listmap = new HashMap<String, Object>();

            //设置信号强度图标image
            if (OPEN_WLAN.equals(mScanResult.capabilities)) {

                if (-100 < mScanResult.level && -70 >= mScanResult.level) {
                    listmap.put("rssiimage", encryptimageId[5]);
                } else if (-70 < mScanResult.level && -60 >= mScanResult.level) {
                    listmap.put("rssiimage", encryptimageId[6]);
                } else if (-60 < mScanResult.level && -50 >= mScanResult.level) {
                    listmap.put("rssiimage", encryptimageId[7]);
                } else if (-50 < mScanResult.level && -40 >= mScanResult.level) {
                    listmap.put("rssiimage", encryptimageId[8]);
                } else if (-40 < mScanResult.level) {
                    listmap.put("rssiimage", encryptimageId[9]);
                } else {
                    listmap.put("rssiimage", encryptimageId[9]);
                }

            } else {
                if (-100 < mScanResult.level && -70 >= mScanResult.level) {
                    listmap.put("rssiimage", encryptimageId[0]);
                } else if (-70 < mScanResult.level && -60 >= mScanResult.level) {
                    listmap.put("rssiimage", encryptimageId[1]);
                } else if (-60 < mScanResult.level && -50 >= mScanResult.level) {
                    listmap.put("rssiimage", encryptimageId[2]);
                } else if (-50 < mScanResult.level && -40 >= mScanResult.level) {
                    listmap.put("rssiimage", encryptimageId[3]);
                } else if (-40 < mScanResult.level) {
                    listmap.put("rssiimage", encryptimageId[4]);
                } else {
                    listmap.put("rssiimage", encryptimageId[4]);
                }
            }

            //设置要显示的AP的SSID
            listmap.put("ssid", mScanResult.SSID);

            listmap.put("wlan_encryption", "加密方式: " + mScanResult.capabilities);

            listmap.put("wlan_Rssi", "信号强度: " + mScanResult.level);
            //将AP一个一个的添加到List中
            listItems.add(listmap);

            /*将已经连接的AP放到List的最顶端*/
            if (mScanResult.BSSID.equals(mCurrentWifi.getBSSID())) {
                listItems.add(0, listmap);
            }
        }

        Log.d(TAG, "Add the wifi list to the adapter");
        listview.setAdapter(listadapter);
    }


    //获取Wifi运行时权限
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.length > 0) {
            // Do something with granted permission
            mScanResultlist = mWifiManager.getScanResults();
        }
    }


    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //收到扫描成功的消息,对扫描结果进行处理
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.d(TAG, "BroadcastReceiver SCAN_RESULTS_AVAILABLE_ACTION");
                showWifiList();
            }
            /*暂时不需要,和上边的消息处理对应
            else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                handleWifiStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN));
            } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                handleNetworkStateChanged(
                        (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
            }else if (intent.getAction().equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                // TODO: handle supplicant connection change later
            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                handleSupplicantStateChanged(
                       (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE),
                       intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR),
                        intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0));
            } else if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {
                handleSignalChanged(intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0));
            } else if (intent.getAction().equals(WifiManager.NETWORK_IDS_CHANGED_ACTION)) {
                // TODO: handle network id change info later
            } else {
                Log.e(TAG, "Received an unknown Wifi Intent");
            }
            */
        }
    };


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume enter");


        if (WifiManager.WIFI_STATE_ENABLED == mWifiManager.getWifiState()) {
            //注册广播服务
            Log.d(TAG, "onResume registerReceiver");
            registerReceiver(mWifiStateReceiver, mWifiStateFilter);

        } else if (WifiManager.WIFI_STATE_DISABLED == mWifiManager.getWifiState()) {
            Log.d(TAG, "onResume WLAN_STATUS_OFF");
        }

        Log.d(TAG, "onResume out");
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause enter");

        if (WifiManager.WIFI_STATE_ENABLED == mWifiManager.getWifiState()) {
            //取消广播注册
            Log.d(TAG, "onPause unregisterReceiver");
            unregisterReceiver(mWifiStateReceiver);
        } else if (WifiManager.WIFI_STATE_DISABLED == mWifiManager.getWifiState()) {
            Log.d(TAG, "onPause WLAN_STATUS_OFF");
        }

        Log.d(TAG, "onPause out");
    }

    @Override
    protected void onDestroy() {
        // TODO 自动生成的方法存根

        super.onDestroy();
    }

    /**
     * 将搜索到的wifi根据信号强度从强到时弱进行排序
     *
     * @param list 存放周围wifi热点对象的列表
     */
    private void sortByLevel(List<ScanResult> list) {

        Log.d(TAG, "sortByLevel According to the signal strength, Sort the Wifi from strong to weak");
        Collections.sort(list, new Comparator<ScanResult>() {

            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                //Log.d(TAG, "sortByLevel Comparative signal strength");
                return rhs.level - lhs.level;
            }
        });
    }


    private void ShowWifiDialog() {
        Log.d(TAG, "ShowDialog enter");

        mWifiConfig = new WifiConfiguration();

        networkId = mWifiAdmin.GetWifiConfig(SSID_SECTION, mWifiConfig);

        mCurrentWifiNetwork = mWifiAdmin.getConnectionInfo();

        mCurrentWifiNetwork.getSupplicantState();


        Log.d(TAG, "The Connected wifi ssid : " + mCurrentWifiNetwork.getSSID());

        Log.d(TAG, "networkId : " + networkId);
        //判断当前AP是否被连接过
        if (-1 == networkId) {
            Log.d(TAG, "The " + SSID_SECTION + " will been connect");
            ConnectWifi();

        } else {
            if (("\"" + SSID_SECTION + "\"").equals(mCurrentWifiNetwork.getSSID())) {
                DialogInterface.OnClickListener dialogOnclicListener = new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case Dialog.BUTTON_POSITIVE:

                                Log.d(TAG, "BUTTON_POSITIVE dismiss");
                                dialog.dismiss();

                                break;
                            case Dialog.BUTTON_NEUTRAL:

                                Log.d(TAG, "BUTTON_NEGATIVE forgetWifi");
                                DisConnectWifi();
                                forgetWifi();

                                break;
                        }
                    }
                };
                //dialog参数设置
                //先得到构造器
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //设置标题
                builder.setTitle(SSID_SECTION);

                //设置内容
                builder.setMessage(SSID_SECTION);

                //设置图标，图片id即可
                builder.setIcon(R.mipmap.ic_launcher);

                builder.setPositiveButton("取消", dialogOnclicListener);
                builder.setNeutralButton("断开连接", dialogOnclicListener);
                builder.create().show();
            } else {
                DialogInterface.OnClickListener dialogOnclicListener = new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case Dialog.BUTTON_POSITIVE:

                                Log.d(TAG, "ConnectWifi");
                                ConnectWifi();

                                break;
                            case Dialog.BUTTON_NEGATIVE:

                                Log.d(TAG, "forgetWifi");
                                forgetWifi();

                                break;
                            case Dialog.BUTTON_NEUTRAL:
                                Log.d(TAG, "dismiss");
                                dialog.dismiss();

                                break;
                        }
                    }
                };
                //dialog参数设置
                //先得到构造器
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                //设置标题
                builder.setTitle(SSID_SECTION);

                //设置内容
                builder.setMessage(SSID_SECTION);

                //设置图标，图片id即可
                builder.setIcon(R.mipmap.ic_launcher);

                builder.setPositiveButton("连接", dialogOnclicListener);
                builder.setNegativeButton("删除", dialogOnclicListener);
                builder.setNeutralButton("取消", dialogOnclicListener);
                builder.create().show();
            }
        }

        Log.d(TAG, "ShowDialog out");
    }

    //连接到指定Wifi
    /*
    private void ConnectWifi(){

        Log.d(TAG, "ConnectWifi enter");

        WifiPswDialog pswDialog = new WifiPswDialog(
                MainWLANActivity.this,
                new WifiPswDialog.OnCustomDialogListener() {
                    @Override
                    public void back(String str) {
                        //获取输入的密码
                        WiFi_Password = str;

                        if (WiFi_Password != null) {

                            int netId = mWifiAdmin.AddWifiConfig(mScanResultlist,SSID_SECTION,WiFi_Password);
                            Log.i(TAG, "WifiPswDialog " + String.valueOf(netId));
                            if (netId != -1) {

                                // 添加了配置信息，要重新得到配置信息
                                try {
                                    mWifiAdmin.getConfiguration();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                if (mWifiAdmin.ConnectWifi(netId)) {

                                    Log.d(TAG, "WiFi connect success");
                                }else{

                                    Log.e(TAG, "WiFi connect fail");
                                }

                            } else {
                                // 网络连接错误
                                Log.e(TAG, "netId error");

                            }
                        } else {
                            Log.d(TAG, "WiFi_Password is null");
                        }
                    }
                },SSID_SECTION);
        pswDialog.show();

        Log.d(TAG, "ConnectWifi out");
    }
    */

    private void ConnectWifi() {

        Log.d(TAG, "ConnectWifi enter");

        if (!(OPEN_WLAN.equals(WLAN_ENCRYPTION))) {
            if (networkId == -1) {
                WifiPswDialog pswDialog = new WifiPswDialog(
                        MainWLANActivity.this,
                        new WifiPswDialog.OnCustomDialogListener() {
                            @Override
                            public void back(String str) {
                                //获取输入的密码
                                WiFi_Password = str;

                                WifiConfiguration configuration = null;
                                if (WiFi_Password != null) {

                                    if (WLAN_ENCRYPTION.contains("WPA")) {

                                        Log.d(TAG, "configuration wpa");
                                        configuration = mWifiAdmin.createWifiInfo(SSID_SECTION, WiFi_Password, 3);
                                    } else if (WLAN_ENCRYPTION.contains("WEP")) {

                                        Log.d(TAG, "configuration wep");
                                        configuration = mWifiAdmin.createWifiInfo(SSID_SECTION, WiFi_Password, 2);
                                    }

                                    if (null == configuration) {
                                        Log.d(TAG, "configuration is null");
                                        return;
                                    }

                                    Log.d(TAG, "addNetwork call");
                                    int hasAddNetworkId = mWifiManager.addNetwork(configuration);

                                    //判断网络是否添加成功
                                    if (hasAddNetworkId != -1) {
                                        Toast.makeText(MainWLANActivity.this, R.string.add_network_success, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainWLANActivity.this, R.string.add_network_failed, Toast.LENGTH_SHORT).show();
                                    }

                                    //连接到已经添加的网络
                                    mWifiManager.enableNetwork(hasAddNetworkId, true);

                                } else {
                                    Log.d(TAG, "WiFi_Password is null");
                                }
                            }
                        }, SSID_SECTION);
                pswDialog.show();
            } else {
                Log.d(TAG, "mWifiManager enableNetwork");
                mWifiManager.enableNetwork(networkId, true);
            }
        } else {
            WifiConfiguration OpenConfiguration = null;

            Log.d(TAG, "OPEN WIFI OpenConfiguration");
            OpenConfiguration = mWifiAdmin.createWifiInfo(SSID_SECTION, "", 1);

            if (null == OpenConfiguration) {
                Log.d(TAG, "OPEN WIFI configuration is null");
                return;
            }

            Log.d(TAG, "OPEN WIFI addNetwork call");
            int hasAddNetworkId = mWifiManager.addNetwork(OpenConfiguration);

            //判断网络是否添加成功
            if (hasAddNetworkId != -1) {
                Toast.makeText(MainWLANActivity.this, R.string.add_network_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainWLANActivity.this, R.string.add_network_failed, Toast.LENGTH_SHORT).show();
            }

            //连接到已经添加的网络
            mWifiManager.enableNetwork(hasAddNetworkId, true);
        }

        Log.d(TAG, "ConnectWifi out");
    }

    private void connectToNetwork() {
        if (networkId == -1) {
            WifiConfiguration configuration = null;

            if (WLAN_ENCRYPTION.contains("WPA")) {

                Log.d(TAG, "configuration wpa");
                configuration = mWifiAdmin.createWifiInfo(SSID_SECTION, WiFi_Password, 3);
            } else if (WLAN_ENCRYPTION.contains("WEP")) {

                Log.d(TAG, "configuration wep");
                configuration = mWifiAdmin.createWifiInfo(SSID_SECTION, WiFi_Password, 2);
            } else if (WLAN_ENCRYPTION.equals("[ESS]")) {
                Log.d(TAG, "OPEN WIFI OpenConfiguration");
                configuration = mWifiAdmin.createWifiInfo(SSID_SECTION, "", 1);
            } else {
                Log.d(TAG, "Unrecognized encryption");
                return;
            }

            if (null == configuration) {
                Log.d(TAG, "configuration is null");
                return;
            }

            Log.d(TAG, " connectToNetwork addNetwork call");
            int hasAddNetworkId = mWifiManager.addNetwork(configuration);

            Log.d(TAG, " connectToNetwork hasAddNetworkId = " + hasAddNetworkId);
            //判断网络是否添加成功
            if (hasAddNetworkId == -1) {
                Log.d(TAG, "connectToNetwork show network failed");
                Toast.makeText(MainWLANActivity.this, R.string.add_network_failed, Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "connectToNetwork show network success");
                Toast.makeText(MainWLANActivity.this, R.string.add_network_success, Toast.LENGTH_SHORT).show();
            }

            //连接到已经添加的网络
            mWifiManager.enableNetwork(hasAddNetworkId, true);

        } else {
            Log.d(TAG, "mWifiManager enableNetwork");
            mWifiManager.enableNetwork(networkId, true);
        }
    }

    //断开指定Wifi
    private void DisConnectWifi() {

        Log.d(TAG, "DisConnectWifi enter");

        try {

            // 要重新得到配置信息
            mWifiAdmin.getConfiguration();

            int netId = mWifiAdmin.IsConfiguration(SSID_SECTION);
            Log.i(TAG, "DisConnectWifi netId = " + String.valueOf(netId));

            if (-1 != netId) {

                mWifiAdmin.disConnectionWifi(netId);
                Log.d(TAG, "DisConnection Wifi success");

            } else {
                Log.d(TAG, "Not found network id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "DisConnectWifi out");
    }

    //移除指定Wifi
    private void forgetWifi() {

        Log.d(TAG, "forgetWifi enter");

        try {

            // 要重新得到配置信息
            mWifiAdmin.getConfiguration();

            int netId = mWifiAdmin.IsConfiguration(SSID_SECTION);
            Log.i(TAG, "forgetWifi netId = " + String.valueOf(netId));

            if (-1 != netId) {

                mWifiAdmin.forgetNetwork(netId);
                Log.d(TAG, "forgetWifi Wifi success");

            } else {
                Log.d(TAG, "Not found network id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "forgetWifi out");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
            case R.id.menu_Refresh:
                Log.d(TAG, " menu_flash start scan");
                mWifiAdmin.startScan();
                Toast.makeText(MainWLANActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_add_wlan:
                showCustomViewDialog();
                break;
/*             case R.id.menu_quit:

                Toast.makeText(MainActivity.this, ""+"退出", Toast.LENGTH_SHORT).show();
                break;
*/
            default:
                break;
        }
//         Toast.makeText(MainActivity.this, ""+item.getItemId(), Toast.LENGTH_SHORT).show();

        return super.onOptionsItemSelected(item);
    }

    private void showCustomViewDialog() {
        final AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(this);

        /**
         * 设置内容区域为自定义View
         */
        final LinearLayout loginDialog = (LinearLayout) getLayoutInflater().inflate(R.layout.custom_view, null);
        mAlertDialog.setView(loginDialog);

        mAlertDialog.setPositiveButton(R.string.menu_custom_view_dialog_Cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        mAlertDialog.setNegativeButton(R.string.menu_custom_view_dialog_Login, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText mAddNetworkPasswordEditText = (EditText) loginDialog.findViewById(R.id.menu_add_network_password_EditText);
                EditText mAddNetworkSSIDEditText = (EditText) loginDialog.findViewById(R.id.menu_add_network_ssid_EditText);
                Spinner mAddNetworkSecuritySpinner = (Spinner) loginDialog.findViewById(R.id.menu_custom_view_dialog_security_spinner);

                Log.d(TAG, "mAddNetworkPasswordEditText = " + mAddNetworkPasswordEditText.getText().toString()
                        + " mAddNetworkSSIDEditText = " + mAddNetworkSSIDEditText.getText().toString()
                        + " mAddNetworkSecuritySpinner = " + mAddNetworkSecuritySpinner.getSelectedItem().toString());

                if (mAddNetworkSSIDEditText.length() < 8) {
                    Toast.makeText(MainWLANActivity.this, R.string.add_network_failed + " , 请输入8位的SSID", Toast.LENGTH_SHORT).show();
                    return;
                }
                //填写完正确的SSID、密码、加密方式
                Log.d(TAG, "Start add network");
                mWifiConfig = new WifiConfiguration();

                SSID_SECTION = mAddNetworkSSIDEditText.getText().toString();

                networkId = mWifiAdmin.GetWifiConfig(SSID_SECTION, mWifiConfig);

                if (mAddNetworkSecuritySpinner.getSelectedItem().toString().equals("无")) {
                    WLAN_ENCRYPTION = OPEN_WLAN;
                } else {
                    WLAN_ENCRYPTION = mAddNetworkSecuritySpinner.getSelectedItem().toString();
                }

                WiFi_Password = mAddNetworkSSIDEditText.getText().toString();

                connectToNetwork();
            }
        });

        mAlertDialog.setCancelable(true);
        mAlertDialog.create();
        mAlertDialog.show();
    }

}
