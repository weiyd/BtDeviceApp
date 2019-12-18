package com.cmtech.android.bledevice.ecg.device;

import com.cmtech.android.ble.core.DeviceRegisterInfo;
import com.cmtech.android.ble.core.IDevice;
import com.cmtech.android.bledevice.ecg.fragment.EcgFragment;
import com.cmtech.android.bledeviceapp.R;
import com.cmtech.android.bledeviceapp.activity.DeviceFragment;
import com.cmtech.android.bledeviceapp.model.DeviceFactory;
import com.cmtech.android.bledeviceapp.model.DeviceType;

// 根据设备类型BleDeviceType，通过反射创建工厂类实例
public class EcgFactory extends DeviceFactory {
    private static final String ECGMONITOR_UUID = "aa40"; // 设备支持的服务UUID短串
    private static final String ECGMONITOR_DEFAULT_NAME = "心电带"; // 缺省设备名
    private static final int ECGMONITOR_DEFAULT_IMAGE_ID = R.drawable.ic_ecgmonitor_default_image; // 缺省图标ID
    private static final String ECGMONITOR_FACTORY = EcgFactory.class.getName();//"com.cmtech.android.bledevice.ecgmonitor.device.EcgFactory"; // 工厂类名

    public static final DeviceType ECGMONITOR_DEVICE_TYPE = new DeviceType(ECGMONITOR_UUID, ECGMONITOR_DEFAULT_IMAGE_ID, ECGMONITOR_DEFAULT_NAME, ECGMONITOR_FACTORY);

    private EcgFactory(DeviceRegisterInfo registerInfo) {
        super(registerInfo);
    }

    @Override
    public IDevice createDevice() {
        return new EcgDevice(registerInfo);
    }

    @Override
    public DeviceFragment createFragment() {
        return DeviceFragment.create(registerInfo.getMacAddress(), EcgFragment.class);
    }
}