package com.cmtech.android.bledeviceapp.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cmtech.android.bledevice.SupportedDeviceType;
import com.cmtech.android.bledevice.ecgmonitor.ecgfile.EcgFileReplayActivity;
import com.cmtech.android.bledevice.ecgmonitor.ecgfile.EcgFileExplorerActivity;
import com.cmtech.android.bledeviceapp.MyApplication;
import com.cmtech.android.bledeviceapp.R;
import com.cmtech.android.bledeviceapp.adapter.BleDeviceListAdapter;
import com.cmtech.android.bledeviceapp.model.FragmentAndTabLayoutManager;
import com.cmtech.android.bledeviceapp.model.UserAccountManager;
import com.cmtech.android.bledevicecore.model.BleDevice;
import com.cmtech.android.bledevicecore.model.BleDeviceAbstractFactory;
import com.cmtech.android.bledevicecore.model.BleDeviceBasicInfo;
import com.cmtech.android.bledevicecore.model.BleDeviceController;
import com.cmtech.android.bledevicecore.model.BleDeviceFragment;
import com.cmtech.android.bledevicecore.model.BleDeviceUtil;
import com.cmtech.android.bledevicecore.model.IBleDeviceActivity;
import com.cmtech.android.bledevicecore.model.IBleDeviceStateObserver;
import com.vise.log.ViseLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *  MainActivity: 主界面
 *  Created by bme on 2018/2/19.
 */
public class MainActivity extends AppCompatActivity implements IBleDeviceStateObserver, IBleDeviceActivity {
    private final static int REQUESTCODE_REGISTERDEVICE = 1;     // 登记设备返回码
    private final static int REQUESTCODE_MODIFYDEVICE = 2;       // 修改设备返回码
    private final static int REQUESTCODE_MODIFYUSERINFO = 3;

    // 已登记的设备列表
    private final List<BleDevice> deviceList = new ArrayList<>();

    // 已打开的设备控制器列表
    private final List<BleDeviceController> openedControllerList = new LinkedList<>();

    // 显示已登记设备列表的Adapter和RecyclerView
    private BleDeviceListAdapter deviceListAdapter;
    private RecyclerView rvDeviceList;

    // 工具条
    private Toolbar toolbar;

    // 侧滑界面
    private DrawerLayout drawerLayout;

    // 欢迎界面
    private LinearLayout welcomeLayout;

    // 包含设备Fragment和Tablayout的界面
    private LinearLayout mainLayout;

    // 主界面的TabLayout和Fragment管理器
    private FragmentAndTabLayoutManager fragAndTabManager;

    // 工具条上的连接菜单和关闭菜单
    private MenuItem menuConnect;
    private MenuItem menuClose;

    // 显示账户名,用户名和头像
    private TextView tvAccountName;
    private TextView tvUserName;
    private ImageView ivAccountImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 创建ToolBar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 设置设备信息RecycleView
        rvDeviceList = findViewById(R.id.rvDevices);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvDeviceList.setLayoutManager(layoutManager);
        rvDeviceList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        deviceListAdapter = new BleDeviceListAdapter(deviceList, this);
        rvDeviceList.setAdapter(deviceListAdapter);

        // 导航菜单设置
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setCheckedItem(R.id.nav_registerdevice);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_registerdevice:
                        registerNewDevice();
                        return true;
                    case R.id.nav_exploreecgrecord:
                        replayEcg();
                        return true;
                    case R.id.nav_changeuser:
                        changeUser();
                        return true;
                    case R.id.nav_exit:
                        finish();
                        return true;
                }
                return false;
            }
        });

        // 设置导航视图
        View headerView = navView.getHeaderView(0);
        tvAccountName = headerView.findViewById(R.id.accountname);
        tvUserName = headerView.findViewById(R.id.username);
        ivAccountImage = headerView.findViewById(R.id.accountimage);
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, UserInfoActivity.class);
                startActivityForResult(intent, REQUESTCODE_MODIFYUSERINFO);
            }
        });


        drawerLayout = findViewById(R.id.drawer_layout);
        welcomeLayout = findViewById(R.id.welcome_layout);
        mainLayout = findViewById(R.id.main_layout);


        // 创建Fragment管理器
        TabLayout tabLayout = findViewById(R.id.main_tab_layout);
        fragAndTabManager = new FragmentAndTabLayoutManager(getSupportFragmentManager(), tabLayout, R.id.main_fragment_layout);
        fragAndTabManager.setOnFragmentChangedListener(new FragmentAndTabLayoutManager.OnFragmentChangedListener() {
            @Override
            public void onFragmentchanged() {
                updateToolBar(((BleDeviceFragment) fragAndTabManager.getCurrentFragment()).getDevice());
            }
        });

        // 更新导航视图
        updateNavigationViewUsingUserInfo();

        // 更新主界面
        updateMainLayoutVisibility();

        // 初始化设备
        initializeBleDevice();

        // 处理输入Intent
        processIntent(getIntent());
    }



    @Override
    protected void onNewIntent(Intent intent) {
        processIntent(intent);
    }

    // 处理输入Intent
    private void processIntent(Intent intent) {
        if(intent != null) {
            Uri uri = intent.getData();
            if(uri != null) {
                ViseLog.e(intent.getData().getPath());
                String desFileName = intent.getData().getPath();
                replayEcgFile(desFileName);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            // 登记设备返回
            case REQUESTCODE_REGISTERDEVICE:

                if(resultCode == RESULT_OK) {
                    BleDeviceBasicInfo basicInfo = (BleDeviceBasicInfo) data.getSerializableExtra("devicebasicinfo");
                    // 用基本信息创建BleDevice
                    if(createBleDeviceUsingBasicInfo(basicInfo)) {
                        // 创建成功后，将设备基本信息保存到数据库中
                        basicInfo.save();
                    }
                }
                break;


            // 修改设备信息返回
            case REQUESTCODE_MODIFYDEVICE:

                if ( resultCode == RESULT_OK) {
                    BleDeviceBasicInfo basicInfo = (BleDeviceBasicInfo) data.getSerializableExtra("devicebasicinfo");
                    String macAddress = basicInfo.getMacAddress();
                    BleDevice device = null;
                    for(BleDevice ele : deviceList) {
                        if(macAddress.equalsIgnoreCase(ele.getMacAddress())) {
                            device = ele;
                            break;
                        }
                    }

                    if(device != null) {
                        basicInfo.save();
                        device.setBasicInfo(basicInfo);
                        deviceListAdapter.notifyDataSetChanged();
                    }
                }
                break;

            // 修改用户信息
            case REQUESTCODE_MODIFYUSERINFO:

                if(resultCode == RESULT_OK) {
                    boolean isModified = data.getBooleanExtra("ismodified", false);
                    if(isModified)
                        updateNavigationViewUsingUserInfo();
                }
                break;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainactivity_menu, menu);
        menuConnect = menu.findItem(R.id.toolbar_connectswitch);
        menuClose = menu.findItem(R.id.toolbar_close);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMainMenuVisibility();

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        BleDeviceFragment fragment;
        switch (item.getItemId()) {
            case android.R.id.home:
                openDrawer(true);
                break;

            case R.id.toolbar_connectswitch:
                fragment = (BleDeviceFragment) fragAndTabManager.getCurrentFragment();
                if(fragment != null) {
                    fragment.switchState();
                }
                break;

            case R.id.toolbar_close:
                fragment = (BleDeviceFragment) fragAndTabManager.getCurrentFragment();
                if(fragment != null) {
                    fragment.closeDevice();
                } else {
                    finish();
                }
                break;
        }
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        for(BleDevice device : deviceList) {
            if(device != null) {
                device.removeDeviceStateObserver(this);
            }
        }

        BleDeviceUtil.disconnectAllDevice();
        BleDeviceUtil.clearAllDevice();
        //MyApplication.getViseBle().disconnect();
        //MyApplication.getViseBle().clear();
        //android.os.Process.killProcess(android.os.Process.myPid());

        UserAccountManager.getInstance().signOut();
    }


    @Override
    public void onBackPressed() {
        // 如果drawerLayout打开，则关闭drawerLayout；如果是关闭，则打开
        /*if(drawerLayout.isDrawerOpen(GravityCompat.START))
            openDrawer(false);
        else
            finish();*/

        openDrawer(!drawerLayout.isDrawerOpen(GravityCompat.START));
    }


    ////////////////////////////////////////////////////////////
    // IBleDeviceStateObserver设备状态观察者接口函数
    ////////////////////////////////////////////////////////////

    // 更新设备状态
    @Override
    public void updateDeviceState(final BleDevice device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 更新设备列表Adapter
                updateDeviceListAdapter();

                // 更新设备的Fragment
                BleDeviceFragment deviceFrag = getFragment(device);
                if(deviceFrag != null) deviceFrag.updateDeviceState(device);

                // 更新Activity的ToolBar
                BleDeviceFragment currentFrag = (BleDeviceFragment) fragAndTabManager.getCurrentFragment();
                if(currentFrag != null && deviceFrag == currentFrag) {
                    updateToolBar(currentFrag.getDevice());
                }
            }
        });

    }

    // 关闭设备
    @Override
    public void closeDevice(BleDeviceFragment fragment) {
        if(fragment == null) return;

        BleDeviceController controller = getController(fragment);
        if(controller == null) return;

        openedControllerList.remove(controller);
        deleteFragment(fragment);
    }

    // 从deviceControllerList中寻找Fragment对应的控制器
    @Override
    public BleDeviceController getController(BleDeviceFragment fragment) {
        for(BleDeviceController controller : openedControllerList) {
            if(controller.getFragment().equals(fragment)) {
                return controller;
            }
        }
        return null;
    }
    ////////////////////////////////////////////////////////////


    // 启动一个BLE设备：为设备创建控制器和Fragment，并自动连接
    public void openDevice(BleDevice device) {
        if(device == null) return;

        BleDeviceFragment fragment = getFragment(device);
        if(fragment != null) {
            // 已经打开了，只要显示Fragment，并开始连接
            showFragment(fragment);
            //fragment.openDevice();
        } else {
            BleDeviceAbstractFactory factory = BleDeviceAbstractFactory.getBLEDeviceFactory(device);
            if(factory == null) return;

            BleDeviceController deviceController = factory.createController(device);
            openedControllerList.add(deviceController);
            openFragment(deviceController.getFragment(), device.getImagePath(), device.getNickName());
        }
    }


    // 删除一个已登记设备
    public void deleteDeviceFromRegisteredDeviceList(final BleDevice device) {
        if(device == null) return;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确定删除该设备吗？");
        builder.setMessage(device.getMacAddress()+'\n'+device.getNickName());
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                device.getBasicInfo().delete();
                deviceList.remove(device);
                updateDeviceListAdapter();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.show();
    }

    // 修改设备基本信息 
    public void modifyDeviceBasicInfo(final BleDevice device) {
        Intent intent = new Intent(this, DeviceBasicInfoActivity.class);
        intent.putExtra("devicebasicinfo", device.getBasicInfo());

        startActivityForResult(intent, REQUESTCODE_MODIFYDEVICE);
    }

    // 更新主Layout的可视性
    private void updateMainLayoutVisibility() {
        if(fragAndTabManager.size() == 0) {
            welcomeLayout.setVisibility(View.VISIBLE);
            mainLayout.setVisibility(View.INVISIBLE);

            //toolbar.setTitleTextColor(Color.BLACK);
            setTitle("CM物联");
            toolbar.setLogo(null);
        } else {
            welcomeLayout.setVisibility(View.INVISIBLE);
            mainLayout.setVisibility(View.VISIBLE);
        }
    }

    // 从数据库中获取已登记的设备基本信息列表
    private void initializeBleDevice() {
        // 从数据库获取设备信息，并构造相应的BLEDevice
        List<BleDeviceBasicInfo> basicInfoList = BleDeviceBasicInfo.findAllFromPreference();
        if(basicInfoList != null && !basicInfoList.isEmpty()) {
            for(BleDeviceBasicInfo basicInfo : basicInfoList) {
                createBleDeviceUsingBasicInfo(basicInfo);
            }
        }
    }

    // 根据设备基本信息创建一个新的设备，并添加到设备列表中
    private boolean createBleDeviceUsingBasicInfo(BleDeviceBasicInfo basicInfo) {
        // 获取相应的抽象工厂
        BleDeviceAbstractFactory factory = BleDeviceAbstractFactory.getBLEDeviceFactory(basicInfo);
        if(factory == null) return false;
        // 用工厂创建BleDevice
        BleDevice device = factory.createBleDevice(basicInfo);

        if(device != null) {
            // 将设备添加到设备列表
            deviceList.add(device);
            // 添加Activity作为设备状态的观察者
            device.registerDeviceStateObserver(this);
            // 通知观察者
            device.notifyDeviceStateObservers();
        }
        return true;
    }


    // 打开或关闭侧滑菜单
    private void openDrawer(boolean open) {
        if(open) {
            drawerLayout.openDrawer(GravityCompat.START);
        } else {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    // 产生已登记的设备Mac地址字符串列表
    private List<String> getRegisteredDeviceMacAddressList() {
        List<String> deviceMacList = new ArrayList<>();
        for(BleDevice device : deviceList) {
            deviceMacList.add(device.getMacAddress());
        }
        return deviceMacList;
    }

    // 登记新设备
    private void registerNewDevice() {
        startScanDevice();
    }

    // 心电信号回放
    private void replayEcg() {
        Intent intent = new Intent(MainActivity.this, EcgFileExplorerActivity.class);
        startActivity(intent);
    }

    // 指定心电信号文件的回放
    private void replayEcgFile(String fileName) {
        Intent intent = new Intent(MainActivity.this, EcgFileReplayActivity.class);
        intent.putExtra("fileName", fileName);
        startActivity(intent);
    }

    // 切换用户
    private void changeUser() {
        UserAccountManager.getInstance().signOut();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("auto_signin", false);
        editor.apply();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateNavigationViewUsingUserInfo() {
        if(UserAccountManager.getInstance().isSignIn()) {
            tvAccountName.setText(UserAccountManager.getInstance().getUserAccount().getAccountName());
            tvUserName.setText(UserAccountManager.getInstance().getUserAccount().getUserName());
            String imagePath = UserAccountManager.getInstance().getUserAccount().getImagePath();
            if(!"".equals(imagePath))
                Glide.with(MyApplication.getContext()).load(imagePath).centerCrop().into(ivAccountImage);
        }
    }



    // 开始扫描设备
    private void startScanDevice() {
        List<String> deviceMacList = getRegisteredDeviceMacAddressList();
        startScanActivity(deviceMacList);
    }

    // 启动扫描设备Activity
    private void startScanActivity(List<String> registeredDeviceMacList) {
        Intent intent = new Intent(MainActivity.this, ScanDeviceActivity.class);
        intent.putExtra("registered_device_list", (Serializable) registeredDeviceMacList);

        startActivityForResult(intent, REQUESTCODE_REGISTERDEVICE);

    }

    // 从deviceControllerList中寻找Fragment对应的控制器
    private BleDeviceController getController(BleDevice device) {
        for(BleDeviceController controller : openedControllerList) {
            if(device.equals(controller.getDevice())) {
                return controller;
            }
        }
        return null;
    }

    // 获取设备对应的Fragment
    private BleDeviceFragment getFragment(BleDevice device) {
        BleDeviceController controller = getController(device);
        return (controller != null) ? controller.getFragment() : null;
    }

    // 打开Fragment：将Fragment加入Manager，并显示
    private void openFragment(BleDeviceFragment fragment, String tabImagePath, String tabText) {
        drawerLayout.closeDrawer(GravityCompat.START);
        // 添加设备的Fragment到管理器
        fragAndTabManager.addFragment(fragment, tabImagePath, tabText);
        updateMainLayoutVisibility();
        updateMainMenuVisibility();
    }

    // 显示Fragment
    private void showFragment(BleDeviceFragment fragment) {
        openDrawer(false);
        fragAndTabManager.showFragment(fragment);
    }

    // 删除Fragment
    private void deleteFragment(BleDeviceFragment fragment) {
        fragAndTabManager.deleteFragment(fragment);
        updateMainLayoutVisibility();
        updateMainMenuVisibility();
    }

    private void updateMainMenuVisibility() {
        if(fragAndTabManager.size() == 0) {
            menuConnect.setVisible(false);
        } else {
            menuConnect.setVisible(true);
        }
    }

    // 更新设备列表
    private void updateDeviceListAdapter() {
        if(deviceListAdapter != null) deviceListAdapter.notifyDataSetChanged();
    }

    // 更新工具条
    private void updateToolBar(BleDevice device) {
        if(device == null) return;

        // 更新连接菜单
        if(device.canDisconnect()) {
            menuConnect.setEnabled(true);
            menuConnect.setIcon(R.mipmap.ic_connect_connected);
        }
        else if(device.canConnect()) {
            menuConnect.setEnabled(true);
            menuConnect.setIcon(R.mipmap.ic_connect_disconnect);
        }
        else {
            menuConnect.setEnabled(false);
            menuConnect.setIcon(R.drawable.connectingdrawable);
            AnimationDrawable connectingDrawable = (AnimationDrawable) menuConnect.getIcon();
            if(!connectingDrawable.isRunning())
                connectingDrawable.start();
        }

        // 更新关闭菜单
        menuClose.setEnabled(device.canClose());

        // 更新工具条图标
        Drawable drawable;
        String imagePath = device.getImagePath();
        if(imagePath != null && !"".equals(imagePath)) {
            drawable = new BitmapDrawable(MyApplication.getContext().getResources(), device.getImagePath());
        } else {
            drawable = ContextCompat.getDrawable(this, SupportedDeviceType.getDeviceTypeFromUuid(device.getUuidString()).getDefaultImage());
        }
        toolbar.setLogo(drawable);
    }
}
