package com.cmtech.android.bledevice.thermo.model;

import com.cmtech.android.bledevice.thermo.activity.ThermoFragment;
import com.cmtech.android.bledevice.core.AbstractBleDeviceFactory;
import com.cmtech.android.bledevice.core.BleDevice;
import com.cmtech.android.bledevice.core.BleDeviceFragment;

public class ThermoDeviceFactory extends AbstractBleDeviceFactory {
    @Override
    public BleDevice createBleDevice() {
        ThermoGattOperator gattOperator = new ThermoGattOperator();
        return new ThermoDevice(basicInfo, gattOperator);
    }

    @Override
    public BleDeviceFragment createFragment() {
        return ThermoFragment.newInstance(basicInfo.getMacAddress());
    }
}
