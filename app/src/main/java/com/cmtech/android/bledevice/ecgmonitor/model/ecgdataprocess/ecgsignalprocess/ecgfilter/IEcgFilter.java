package com.cmtech.android.bledevice.ecgmonitor.model.ecgdataprocess.ecgsignalprocess.ecgfilter;

/**
 * IEcgFilter: Ecg信号滤波器接口
 * Created by Chenm, 2018-11-29
 */

public interface IEcgFilter{

    double filter(double ecgSignal); // 滤波处理

    void setSampleRate(int sampleRate); // 设置采样频率
}