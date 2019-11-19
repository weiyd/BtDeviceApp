package com.cmtech.android.bledevice.ecgmonitor.interfac;

import com.cmtech.android.bledevice.ecgmonitor.device.EcgMonitorConfiguration;
import com.cmtech.android.bledevice.ecgmonitor.enumeration.EcgLeadType;
import com.cmtech.android.bledevice.ecgmonitor.process.hr.HrStatisticProcessor;

public interface IEcgDevice extends HrStatisticProcessor.OnHrStatisticInfoUpdatedListener {
    int getSampleRate();
    EcgLeadType getLeadType();
    void setValue1mV(int value1mV);
    int getValue1mV();
    EcgMonitorConfiguration getConfig();
    void updateSignalValue(int ecgSignal);
    void updateHrValue(short hr);
    void notifyHrAbnormal();
}
