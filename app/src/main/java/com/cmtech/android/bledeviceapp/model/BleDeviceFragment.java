package com.cmtech.android.bledeviceapp.model;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Toast;

import com.cmtech.android.bledevicecore.model.BleDevice;
import com.cmtech.android.bledeviceapp.activity.MainActivity;

/**
 * Created by bme on 2018/2/27.
 */

public abstract class BleDeviceFragment extends Fragment{
    // MainActivity
    protected MainActivity activity;

    // 对应的控制器接口
    protected BleDeviceController controller;

    // 对应的设备接口
    protected BleDevice device;

    public BleDeviceFragment() {

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(!(context instanceof MainActivity)) {
            throw new IllegalStateException("context不是MainActivity");
        }

        // 获得Activity
        activity = (MainActivity) context;

        // 获取controller
        controller = activity.getController(this);

        // 获取device
        if(controller != null) {
            device = (BleDevice) controller.getDevice();
        }

        if(device == null || controller == null) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 打开设备
        openDevice();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // 更新连接状态
        updateDeviceState();
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        controller.closeDevice();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    // 获取设备
    public BleDevice getDevice() {
        return device;
    }

    // 连接设备
    public void openDevice() {
        controller.openDevice();
    }

    // 断开设备
    public void disconnectDevice() {
        controller.disconnectDevice();
    }

    // 关闭设备
    // 为什么这里不是调用controller.closeDevice()，而是调用activity.closeDevice(fragment)???
    // 因为关闭一个带Fragment的设备，除了要关闭设备本身以外，还要销毁它的Fragment，并将设备的控制器从控制器列表中删除
    // 这些动作需要调用activity.closeDevice才能完成
    // 关闭设备的动作会在销毁Fragment时触发onDestroy()，那里会调用controller.closeDevice()来关闭设备
    public void closeDevice() {
        activity.closeDevice(this);
    }

    // 切换设备状态
    public void switchState() {
        controller.switchState();
    }

    // 配置设备
    public void configureDevice(int requestCode) {
        if(controller != null) {
            controller.configureDevice(activity, requestCode);
        }
    }

    // 更新设备连接状态
    public void updateDeviceState(final BleDevice device) {
        // isAdded()用来判断Fragment是否与Activity关联，如果关联了，才能更新状态信息
        if(device == this.device && isAdded()) {
            updateDeviceState();
        }
    }

    private void updateDeviceState() {


    }

    //////////////////////////////////////////////////////////////////////////

}
