package com.cmtech.android.bledevice.temphumid.model;

import com.cmtech.android.bledevice.temphumid.activity.TempHumidFragment;
import com.cmtech.android.bledevicecore.AbstractBleDeviceFactory;
import com.cmtech.android.bledevicecore.BleDevice;
import com.cmtech.android.bledevicecore.BleDeviceFragment;

// 会根据设备类型BleDeviceType，通过反射创建工厂类实例
public class TempHumidDeviceFactory extends AbstractBleDeviceFactory {
    @Override
    public BleDevice createBleDevice() {
        return new TempHumidDevice(basicInfo);
    }

    @Override
    public BleDeviceFragment createFragment() {
        return TempHumidFragment.newInstance(basicInfo.getMacAddress());
    }


}
