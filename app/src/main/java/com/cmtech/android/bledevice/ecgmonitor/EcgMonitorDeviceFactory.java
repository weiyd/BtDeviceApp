package com.cmtech.android.bledevice.ecgmonitor;

import com.cmtech.android.bledevicecore.model.BleDevice;
import com.cmtech.android.bledevicecore.model.BleDeviceAbstractFactory;
import com.cmtech.android.bledevicecore.model.BleDeviceBasicInfo;
import com.cmtech.android.bledevicecore.model.BleDeviceController;
import com.cmtech.android.bledevicecore.model.BleDeviceFragment;

public class EcgMonitorDeviceFactory extends BleDeviceAbstractFactory {
    @Override
    public BleDevice createBleDevice(BleDeviceBasicInfo basicInfo) {
        return new EcgMonitorDevice(basicInfo);
    }

    @Override
    public BleDeviceController createController(BleDevice device) {
        return new EcgMonitorController(device);
    }

    @Override
    public BleDeviceFragment createFragment() {
        return new EcgMonitorFragment();
    }
}
