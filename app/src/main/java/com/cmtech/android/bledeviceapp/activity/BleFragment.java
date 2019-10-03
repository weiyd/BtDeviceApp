package com.cmtech.android.bledeviceapp.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.cmtech.android.ble.core.BleDevice;

/**
 * BleFragment：设备的Fragment
 * Created by bme on 2018/2/27.
 */

public abstract class BleFragment extends Fragment{
    private static final String TAG = "BleFragment";
    private static final String ARG_DEVICE_MAC = "device_mac";

    private IBleDeviceActivity activity; //包含BleDeviceFragment的Activity，必须要实现IBleDeviceActivity接口
    private BleDevice device; // 设备


    protected BleFragment() {
    }

    public static BleFragment create(String macAddress, Class<? extends BleFragment> fragClass) {
        BleFragment fragment;
        try {
            fragment = fragClass.newInstance();
            Bundle bundle = new Bundle();
            bundle.putString(ARG_DEVICE_MAC, macAddress);
            fragment.setArguments(bundle);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return fragment;
    }

    public BleDevice getDevice() {
        return device;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof IBleDeviceActivity) {
            activity = (IBleDeviceActivity) context;
        } else {
            throw new IllegalArgumentException("The context must be a instance of IBleDeviceActivity.");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 用macAddress获取BleDevice
        Bundle bundle = getArguments();
        if(bundle == null) throw new IllegalStateException();
        String deviceMac = bundle.getString(ARG_DEVICE_MAC);
        device = activity.findDevice(deviceMac);
        if(device == null) throw new IllegalArgumentException("The device is null.");

        // 更新连接状态
        updateState();
        // 注册设备状态观察者
        device.addListener(activity);
        device.updateState();

        // 打开设备
        device.open();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 移除activity设备状态观察者
        //device.removeListener(activity);
    }

    // 切换状态
    public void switchState() {
        device.switchState();
    }

    // 更新状态
    public void updateState() {
        // isAdded()用来判断Fragment是否与Activity关联，如果关联了，才能更新状态信息
        if(device != null && isAdded()) {

        }
    }

    // 关闭
    public void close() {
        if(activity != null) {
            activity.closeFragment(this);
        }
    }

    // 打开配置Activity
    public abstract void openConfigActivity();

}