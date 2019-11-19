package com.cmtech.android.bledevice.ecgmonitor.device;

import android.content.Context;

import com.cmtech.android.ble.core.BleDeviceState;
import com.cmtech.android.ble.core.DeviceRegisterInfo;
import com.cmtech.android.ble.core.IDevice;
import com.cmtech.android.bledevice.ecgmonitor.enumeration.EcgLeadType;
import com.cmtech.android.bledevice.ecgmonitor.interfac.IEcgDevice;
import com.cmtech.android.bledevice.ecgmonitor.interfac.OnEcgMonitorListener;
import com.cmtech.android.bledevice.ecgmonitor.process.hr.HrStatisticsInfo;
import com.cmtech.android.bledevice.ecgmonitor.record.EcgRecord;

public abstract class AbstractEcgDevice implements IEcgDevice {
    protected int sampleRate; // 采样率
    protected EcgLeadType leadType; // 导联类型
    protected int value1mV; // 定标之前1mV值
    protected OnEcgMonitorListener listener; // 心电监护仪监听器
    protected EcgRecord ecgRecord; // 心电记录，可记录心电信号数据、用户留言和心率信息
    protected EcgMonitorConfiguration config; // 心电监护仪的配置信息

    private IDevice deviceProxy;

    public AbstractEcgDevice(IDevice deviceProxy) {
        this.deviceProxy = deviceProxy;
    }

    @Override
    public int getSampleRate() { return sampleRate; }
    @Override
    public EcgLeadType getLeadType() {
        return leadType;
    }
    @Override
    public int getValue1mV() { return value1mV; }
    @Override
    public EcgMonitorConfiguration getConfig() {
        return config;
    }
    @Override
    public void setListener(OnEcgMonitorListener listener) {
        this.listener = listener;
    }
    @Override
    public void removeListener() {
        listener = null;
    }
    @Override
    public void notifyHrAbnormal() {
        if(listener != null) {
            listener.onHrAbnormalNotified();
        }
    }
    @Override
    public void onHrStatisticInfoUpdated(HrStatisticsInfo hrInfoObject) {
        if(listener != null) {
            listener.onHrStatisticsInfoUpdated(hrInfoObject);
        }
    }

    @Override
    public DeviceRegisterInfo getRegisterInfo() {
        return null;
    }

    @Override
    public void updateRegisterInfo(DeviceRegisterInfo registerInfo) {

    }

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getUuidString() {
        return null;
    }

    @Override
    public String getImagePath() {
        return null;
    }

    @Override
    public BleDeviceState getState() {
        return null;
    }

    @Override
    public boolean isScanning() {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isDisconnected() {
        return false;
    }

    @Override
    public void setState(BleDeviceState state) {

    }

    @Override
    public void updateState() {

    }

    @Override
    public int getBattery() {
        return 0;
    }

    @Override
    public void setBattery(int battery) {

    }

    @Override
    public void addListener(OnDeviceListener listener) {

    }

    @Override
    public void removeListener(OnDeviceListener listener) {

    }

    @Override
    public void open(Context context) {

    }

    @Override
    public void switchState() {

    }

    @Override
    public void callDisconnect(boolean stopAutoScan) {

    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public void clear() {

    }
}
