package com.cmtech.android.bledevice.ecgmonitor.model.ecgProcess.ecghrprocess;

/**
 * IEcgHrObserver: 心率值观察者接口
 * Created by Chenm, 2018-12-26
 */

public interface IEcgHrObserver {
    void updateHr(int hr);
}
