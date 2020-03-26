package com.cmtech.android.bledeviceapp.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cmtech.android.ble.core.BleDeviceInfo;
import com.cmtech.android.ble.core.BleScanner;
import com.cmtech.android.ble.core.DeviceInfo;
import com.cmtech.android.ble.core.IDevice;
import com.cmtech.android.ble.core.WebDeviceInfo;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.bledevice.hrmonitor.view.HrRecordExplorerActivity;
import com.cmtech.android.bledeviceapp.MyApplication;
import com.cmtech.android.bledeviceapp.R;
import com.cmtech.android.bledeviceapp.adapter.CtrlPanelAdapter;
import com.cmtech.android.bledeviceapp.model.Account;
import com.cmtech.android.bledeviceapp.model.AccountManager;
import com.cmtech.android.bledeviceapp.model.DeviceFactory;
import com.cmtech.android.bledeviceapp.model.DeviceFragTabManager;
import com.cmtech.android.bledeviceapp.model.DeviceManager;
import com.cmtech.android.bledeviceapp.model.DeviceType;
import com.cmtech.android.bledeviceapp.model.FragTabManager;
import com.cmtech.android.bledeviceapp.model.MainToolbarManager;
import com.cmtech.android.bledeviceapp.model.NotifyService;
import com.cmtech.android.bledeviceapp.util.APKVersionCodeUtils;
import com.vise.log.ViseLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;

import static com.cmtech.android.ble.core.DeviceState.CLOSED;
import static com.cmtech.android.ble.core.IDevice.INVALID_BATTERY;
import static com.cmtech.android.bledeviceapp.AppConstant.KM_STORE_URI;
import static com.cmtech.android.bledeviceapp.MyApplication.showMessageUsingShortToast;
import static com.cmtech.android.bledeviceapp.activity.LoginActivity.SUPPORT_PLATFORM;
import static com.cmtech.android.bledeviceapp.activity.RegisterActivity.DEVICE_INFO;

/**
 *  MainActivity: 主界面
 *  Created by bme on 2018/2/19.
 */

public class MainActivity extends AppCompatActivity implements IDevice.OnDeviceListener, FragTabManager.OnFragmentUpdatedListener {
    private static final String TAG = "MainActivity";
    private final static int RC_REGISTER_DEVICE = 1;     // return code for registering new device
    private final static int RC_MODIFY_DEVICE_INFO = 2;       // return code for modifying device info
    private final static int RC_MODIFY_ACCOUNT_INFO = 3;     // return code for modifying account info

    private final static SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());

    LocalDevicesFragment localDevicesFragment;
    //WebDevicesFragment webDevicesFragment;
    private NotifyService notiService; // 通知服务,用于初始化BleDeviceManager，并管理后台通知
    private DeviceFragTabManager fragTabManager; // BleFragment和TabLayout管理器
    private MainToolbarManager tbManager; // 工具条管理器
    private DrawerLayout drawerLayout; // 侧滑界面
    private LinearLayout noDeviceLayout; // 无设备打开时的界面
    private RelativeLayout hasDeviceLayout; // 有设备打开时的界面，即包含设备Fragment和Tablayout的主界面
    private FloatingActionButton fabConnect; // 切换连接状态的FAB
    private TextView tvAccountName; // 账户名称控件
    private ImageView ivAccountImage; // 头像头像控件
    private Button btnLogout;
    private boolean stopNotiService = false; // 是否停止通知服务

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            notiService = ((NotifyService.BleNotifyServiceBinder)iBinder).getService();
            // 成功绑定后初始化UI，否则请求退出
            if(notiService != null) {
                initUI();
            } else {
                requestFinish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if(notiService != null) {
                Intent intent = new Intent(MainActivity.this, NotifyService.class);
                stopService(intent);
                notiService = null;
            }
            finish();
        }
    };

    public NotifyService getNotiService() {
        return notiService;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 确定账户已经登录
        if(!AccountManager.getInstance().isSignIn()) {
            Toast.makeText(this, "account sign in fail.", Toast.LENGTH_SHORT).show();
            finish();
        }
        ViseLog.e(AccountManager.getInstance().getAccount());

        // 启动并绑定通知服务
        Intent serviceIntent = new Intent(this, NotifyService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        if(BleScanner.isBleDisabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
        }
    }

    // 主界面初始化
    private void initUI() {
        // 初始化工具条管理器
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TextView tvBattery = findViewById(R.id.tv_device_battery);
        tbManager = new MainToolbarManager(this, toolbar, tvBattery);
        tbManager.setNavigationIcon(AccountManager.getInstance().getAccount().getImagePath());

        // init device control panel
        ViewPager pager = findViewById(R.id.vp_device_panel);
        TabLayout layout = findViewById(R.id.tl_device_panel);
        localDevicesFragment = new LocalDevicesFragment();
        //webDevicesFragment = new WebDevicesFragment();
        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(localDevicesFragment);
        List<String> titleList = new ArrayList<>();
        titleList.add("本地设备");
        CtrlPanelAdapter fragAdapter = new CtrlPanelAdapter(getSupportFragmentManager(), fragmentList, titleList);
        pager.setAdapter(fragAdapter);
        pager.setOffscreenPageLimit(1);
        layout.setupWithViewPager(pager);

        // init navigation view
        initNavigation();
        updateNavigation();

        // 设置FAB，FloatingActionButton
        fabConnect = findViewById(R.id.fab_connect);
        fabConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceFragment fragment = (DeviceFragment) fragTabManager.getCurrentFragment();
                if(fragment != null) {
                    fragment.switchState();
                }
            }
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        noDeviceLayout = findViewById(R.id.layout_when_nodeivce_opened);
        hasDeviceLayout = findViewById(R.id.layout_when_device_opened);

        // 初始化BleFragTabManager
        TabLayout tabLayout = findViewById(R.id.tablayout_device);
        fragTabManager = new DeviceFragTabManager(getSupportFragmentManager(), tabLayout, R.id.layout_main_fragment);
        fragTabManager.setOnFragmentUpdatedListener(this);

        // init main layout
        initMainLayout();

        // 为已经打开的设备创建并打开Fragment
        List<IDevice> openedDevices = DeviceManager.getOpenedDevice();
        for(IDevice device : openedDevices) {
            if(device.getState() != CLOSED) {
                createAndOpenFragment(device);
            }
        }
    }

    private void initNavigation() {
        NavigationView navView = findViewById(R.id.nav_view);
        View headerView = navView.getHeaderView(0);
        tvAccountName = headerView.findViewById(R.id.tv_account_name);
        ivAccountImage = headerView.findViewById(R.id.iv_account_image);
        btnLogout = headerView.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutAccount();
            }
        });

        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_scan_device: // 扫描设备
                        List<String> addresses = DeviceManager.getDeviceAddressList();
                        Intent scanIntent = new Intent(MainActivity.this, ScanActivity.class);
                        scanIntent.putExtra("device_address_list", (Serializable) addresses);
                        startActivityForResult(scanIntent, RC_REGISTER_DEVICE);
                        return true;
                    case R.id.nav_query_record: // 查阅记录
                        /*PopupMenu popupMenu = new PopupMenu(MainActivity.this, item.getActionView());
                        popupMenu.inflate(R.menu.menu_query_record);
                        List<DeviceType> types = DeviceType.getSupportedDeviceTypes();
                        popupMenu.getMenu().findItem(R.id.nav_hr_record).setVisible(true);
                        popupMenu.getMenu().findItem(R.id.nav_ecg_record).setVisible(types.contains(ECGMONITOR_DEVICE_TYPE));
                        popupMenu.getMenu().findItem(R.id.nav_temphumid_record).setVisible(types.contains(TEMPHUMID_DEVICE_TYPE));
                        popupMenu.getMenu().findItem(R.id.nav_thermo_record).setVisible(types.contains(THERMO_DEVICE_TYPE));
                        @SuppressLint("RestrictedApi")
                        MenuPopupHelper popupHelper = new MenuPopupHelper(MainActivity.this, (MenuBuilder) popupMenu.getMenu(), item.getActionView());
                        popupHelper.setForceShowIcon(true);
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                switch (menuItem.getItemId()) {
                                    case R.id.nav_hr_record:
                                        Intent hrIntent = new Intent(MainActivity.this, HrRecordExplorerActivity.class);
                                        startActivity(hrIntent);
                                        return true;
                                    case R.id.nav_ecg_record:
                                        Intent recordIntent = new Intent(MainActivity.this, EcgRecordExplorerActivity.class);
                                        startActivity(recordIntent);
                                        return true;
                                    case R.id.nav_temphumid_record:
                                    case R.id.nav_thermo_record:
                                        Toast.makeText(MainActivity.this, "当前无记录。", Toast.LENGTH_SHORT).show();
                                        return true;
                                }
                                return false;
                            }
                        });
                        popupHelper.show();*/
                        Intent hrIntent = new Intent(MainActivity.this, HrRecordExplorerActivity.class);
                        startActivity(hrIntent);
                        return true;
                    case R.id.nav_open_store: // open KM store
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(KM_STORE_URI));
                        startActivity(intent);
                        return true;
                    case R.id.nav_exit: // exit
                        requestFinish();
                        return true;
                }
                return false;
            }
        });
    }

    private void updateMainLayout(IDevice device) {
        if(device == null) {
            tbManager.setTitle(getString(R.string.app_name), getString(R.string.no_device_opened));
            tbManager.setBattery(INVALID_BATTERY);
            updateConnectFAButton(CLOSED.getIcon());
            invalidateOptionsMenu();
            updateMainLayoutVisibility(false);
        } else {
            String title = device.getName();
            DeviceInfo registerInfo = device.getRegisterInfo();
            if(!device.isLocal()) {
                title += ("-" + ((WebDeviceInfo) registerInfo).getBroadcastName());
            }
            tbManager.setTitle(title, device.getAddress());
            tbManager.setBattery(device.getBattery());
            updateConnectFAButton(device.getState().getIcon());
            updateCloseMenuItem(true);
            updateMainLayoutVisibility(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_REGISTER_DEVICE: // 注册设备返回
                if(resultCode == RESULT_OK) {
                    BleDeviceInfo registerInfo = (BleDeviceInfo) data.getSerializableExtra(DEVICE_INFO);
                    registerDevice(registerInfo);
                }
                break;

            case RC_MODIFY_DEVICE_INFO: // 修改注册信息返回
                if ( resultCode == RESULT_OK) {
                    BleDeviceInfo registerInfo = (BleDeviceInfo) data.getSerializableExtra(DEVICE_INFO);
                    IDevice device = DeviceManager.findDevice(registerInfo);
                    if(device != null && registerInfo.saveToPref(pref)) {
                        Toast.makeText(MainActivity.this, "设备信息修改成功", Toast.LENGTH_SHORT).show();
                        device.updateRegisterInfo(registerInfo);
                        updateDeviceList();
                        Drawable drawable;
                        if(TextUtils.isEmpty(device.getImagePath())) {
                            DeviceType deviceType = DeviceType.getFromUuid(device.getUuidString());
                            if(deviceType == null) {
                                throw new IllegalStateException("The device type is not supported.");
                            }
                            drawable = ContextCompat.getDrawable(this, deviceType.getDefaultImageId());
                        } else {
                            drawable = new BitmapDrawable(getResources(), device.getImagePath());
                        }
                        fragTabManager.updateTabInfo(fragTabManager.findFragment(device), drawable, device.getName());
                        if(fragTabManager.isFragmentSelected(device)) {
                            tbManager.setTitle(device.getName(), device.getAddress());
                            tbManager.setBattery(device.getBattery());
                        }
                    } else {
                        showMessageUsingShortToast("设备信息修改失败");
                    }
                }
                break;

            case RC_MODIFY_ACCOUNT_INFO: // 修改用户信息返回
                if(resultCode == RESULT_OK) {
                    updateNavigation();
                    tbManager.setNavigationIcon(AccountManager.getInstance().getAccount().getImagePath());
                } else {
                    boolean logout = (data != null && data.getBooleanExtra("logout", false));
                    if(logout) { // 退出登录
                        logoutAccount();
                    }
                }
                break;
        }
    }

    private void registerDevice(BleDeviceInfo deviceInfo) {
        if(deviceInfo != null) {
            IDevice device = DeviceManager.createDeviceIfNotExist(deviceInfo);
            if(device != null) {
                if(deviceInfo.saveToPref(pref)) {
                    //Toast.makeText(MainActivity.this, "设备注册成功", Toast.LENGTH_SHORT).show();
                    updateDeviceList();
                    device.addListener(notiService);
                } else {
                    Toast.makeText(MainActivity.this, "添加设备失败", Toast.LENGTH_SHORT).show();
                    DeviceManager.deleteDevice(device);
                }
            }
        }
    }

    private void updateDeviceList() {
        localDevicesFragment.update();
        //webDevicesFragment.update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem menuConfig = menu.findItem(R.id.toolbar_config);
        MenuItem menuClose = menu.findItem(R.id.toolbar_close);
        tbManager.setMenuItems(new MenuItem[]{menuConfig, menuClose});
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if(fragTabManager.size() == 0) {
            tbManager.updateMenuItemsVisible(new boolean[]{false, true});
        } else {
            tbManager.updateMenuItemsVisible(new boolean[]{true, true});
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        DeviceFragment fragment;
        switch (item.getItemId()) {
            case android.R.id.home:
                openDrawer(true);
                break;

            case R.id.toolbar_config:
                fragment = (DeviceFragment) fragTabManager.getCurrentFragment();
                if(fragment != null) {
                    fragment.openConfigureActivity();
                }
                break;

            case R.id.toolbar_close:
                fragment = (DeviceFragment) fragTabManager.getCurrentFragment();
                if(fragment != null) {
                    closeFragment(fragment);
                } else {
                    requestFinish();
                }
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        ViseLog.e("MainActivity.onDestroy()");
        super.onDestroy();

        DeviceManager.removeDeviceListener(this);

        unbindService(serviceConnection);
        if(stopNotiService) {
            Intent stopIntent = new Intent(MainActivity.this, NotifyService.class);
            stopService(stopIntent);
        }
    }

    private void requestFinish() {
        if(DeviceManager.hasOpenedDevice()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("退出应用");
            builder.setMessage("有设备打开，退出将关闭这些设备。");
            builder.setPositiveButton("退出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    stopNotiService = true;
                    finish();
                }
            });
            builder.setNegativeButton("最小化", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    stopNotiService = false;
                    openDrawer(false);
                    MainActivity.this.moveTaskToBack(true);
                }
            });
            builder.show();
        } else {
            stopNotiService = true;
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        openDrawer(!drawerLayout.isDrawerOpen(GravityCompat.START));
    }

    // 设备状态更新
    @Override
    public void onStateUpdated(final IDevice device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 更新设备列表Adapter
                updateDeviceList();
                // 更新设备的Fragment界面
                DeviceFragment deviceFrag = fragTabManager.findFragment(device);
                if(deviceFrag != null) deviceFrag.updateState();
                if(fragTabManager.isFragmentSelected(device)) {
                    updateConnectFAButton(device.getState().getIcon());
                    updateCloseMenuItem(true);
                }
            }
        });

    }

    // 异常通知
    @Override
    public void onExceptionNotified(IDevice device, BleException ex) {
        showMessageUsingShortToast(device.getAddress() + "-" + ex.getDescription());
    }

    // 电量更新
    @Override
    public void onBatteryUpdated(final IDevice device) {
        if(fragTabManager.isFragmentSelected(device)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tbManager.setBattery(device.getBattery());
                }
            });
        }
    }

    // Fragment更新
    @Override
    public void onFragmentUpdated() {
        DeviceFragment fragment = (DeviceFragment)fragTabManager.getCurrentFragment();
        IDevice device = (fragment == null) ? null : fragment.getDevice();
        updateMainLayout(device);
    }

    // 打开设备
    public void openDevice(IDevice device) {
        if(device == null) return;

        DeviceFragment fragment = fragTabManager.findFragment(device);
        if(fragment != null) {
            openDrawer(false);
            fragTabManager.showFragment(fragment);
        } else {
            if(device.getState() == CLOSED)
                createAndOpenFragment(device);
        }
    }

    private void createAndOpenFragment(IDevice device) {
        if(device == null) return;

        DeviceFactory factory = DeviceFactory.getFactory(device.getRegisterInfo());
        if(factory != null) {
            openDrawer(false);
            Drawable drawable;
            if(TextUtils.isEmpty(device.getImagePath())) {
                DeviceType deviceType = DeviceType.getFromUuid(device.getUuidString());
                if(deviceType == null) {
                    throw new IllegalStateException("The device type is not supported.");
                }
                drawable = ContextCompat.getDrawable(this, deviceType.getDefaultImageId());
            } else {
                drawable = new BitmapDrawable(getResources(), device.getImagePath());
            }
            if(drawable == null) {
                DeviceType deviceType = DeviceType.getFromUuid(device.getUuidString());
                if(deviceType == null) {
                    throw new IllegalStateException("The device type is not supported.");
                }
                drawable = ContextCompat.getDrawable(this, deviceType.getDefaultImageId());
            }
            fragTabManager.openFragment(factory.createFragment(), drawable, device.getName());
            updateMainLayout(device);
        }
    }

    // 关闭Fragment
    private void closeFragment(final DeviceFragment fragment) {
        if(fragment != null)
            fragment.close();
    }

    // 移除Fragment
    public void removeFragment(DeviceFragment fragment) {
        fragTabManager.removeFragment(fragment);
    }

    // 从设备列表中删除设备
    public void removeRegisteredDevice(final IDevice device) {
        if(device == null) return;

        if(fragTabManager.isFragmentOpened(device)) {
            showMessageUsingShortToast("请先关闭该设备。");
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("删除设备").setMessage("确定删除设备：" + device.getAddress()+'\n'+device.getName());
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DeviceInfo registerInfo = device.getRegisterInfo();
                if(registerInfo instanceof BleDeviceInfo && ((BleDeviceInfo)registerInfo).deleteFromPref(pref)) {
                    DeviceManager.deleteDevice(device);
                    updateDeviceList();
                } else {
                    Toast.makeText(MainActivity.this, "无法删除设备。", Toast.LENGTH_SHORT).show();
                }
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).show();
    }

    // 修改设备注册信息 
    public void modifyDeviceInfo(final DeviceInfo deviceInfo) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra(DEVICE_INFO, deviceInfo);
        startActivityForResult(intent, RC_MODIFY_DEVICE_INFO);
    }

    private void initMainLayout() {
        TextView tvVersionName = noDeviceLayout.findViewById(R.id.tv_versionname);
        tvVersionName.setText(String.format("Ver%s", APKVersionCodeUtils.getVerName(this)));
        updateMainLayout(null);
    }

    // 打开或关闭侧滑菜单
    private void openDrawer(boolean open) {
        if(open) {
            drawerLayout.openDrawer(GravityCompat.START);
        } else {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    // logout
    private void logoutAccount() {
        if(DeviceManager.hasOpenedDevice()) {
            Toast.makeText(this, "有设备打开，请先关闭设备。", Toast.LENGTH_SHORT).show();
            return;
        }

        Account account = AccountManager.getInstance().getAccount();
        if(account != null) {
            if(account.getPlatName().equals(QQ.NAME)) {
                Platform plat = ShareSDK.getPlatform(QQ.NAME);
                plat.removeAccount(true);
            } else if(account.getPlatName().equals(Wechat.NAME)) {
                Platform plat = ShareSDK.getPlatform(Wechat.NAME);
                plat.removeAccount(true);
            }
        }
        AccountManager.getInstance().signOut();

        Intent intent = new Intent(MainActivity.this, SplashActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateNavigation() {
        Account account = AccountManager.getInstance().getAccount();
        if(account == null) {
            throw new IllegalStateException();
        }

        if(account.getName() == null || "".equals(account.getName().trim())) {
            tvAccountName.setText("请设置");
        } else {
            tvAccountName.setText(account.getName());
        }

        // load icon by platform name
        ivAccountImage.setImageResource(SUPPORT_PLATFORM.get(account.getPlatName()));
        //Glide.with(MyApplication.getContext()).load(R.id.ib_qq_login).into(ivAccountImage);

        /* load icon from imagePath
        String imagePath = account.getImagePath();
        if(imagePath != null && !"".equals(imagePath))
            Glide.with(MyApplication.getContext()).load(imagePath).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(ivAccountImage);*/
    }

    // 更新MainLayout的可视性
    private void updateMainLayoutVisibility(boolean hasDeviceOpened) {
        if(hasDeviceOpened && hasDeviceLayout.getVisibility() == View.INVISIBLE) {
            noDeviceLayout.setVisibility(View.INVISIBLE);
            hasDeviceLayout.setVisibility(View.VISIBLE);
        } else if(!hasDeviceOpened && hasDeviceLayout.getVisibility() == View.VISIBLE){
            noDeviceLayout.setVisibility(View.VISIBLE);
            hasDeviceLayout.setVisibility(View.INVISIBLE);
        }
    }

    // 更新连接浮动动作按钮
    private void updateConnectFAButton(int icon) {
        fabConnect.clearAnimation();
        fabConnect.setImageResource(icon);
        if(fabConnect.getDrawable() instanceof AnimationDrawable) {
            ((AnimationDrawable) fabConnect.getDrawable()).start();
        }
    }

    private void updateCloseMenuItem(boolean visible) {
        tbManager.updateMenuItemVisible(1, visible);
    }

}
