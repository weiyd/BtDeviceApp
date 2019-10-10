package com.cmtech.android.bledevice.ecgmonitor.model.ecgdataprocess.ecgsignalprocess;

import com.cmtech.android.bledevice.ecgmonitor.model.EcgMonitorDevice;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgdataprocess.ecgsignalprocess.ecgcalibrator.EcgCalibrator65536;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgdataprocess.ecgsignalprocess.ecgcalibrator.IEcgCalibrator;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgdataprocess.ecgsignalprocess.ecgfilter.EcgPreFilterWith35HzNotch;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgdataprocess.ecgsignalprocess.ecgfilter.IEcgFilter;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgdataprocess.ecgsignalprocess.ecghrprocess.HrAbnormalWarner;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgdataprocess.ecgsignalprocess.ecghrprocess.HrProcessor;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgdataprocess.ecgsignalprocess.ecghrprocess.IHrOperator;
import com.cmtech.msp.qrsdetbyhamilton.QrsDetector;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.cmtech.android.bledevice.ecgmonitor.model.EcgMonitorDevice.VALUE_1MV_AFTER_CALIBRATION;


/**
  *
  * ClassName:      EcgSignalProcessor
  * Description:    心电信号处理器，包含心电信号的标定，预滤波，基于QRS波检测的心率计算，以及心率记录和统计分析，心率异常报警
  * Author:         chenm
  * CreateDate:     2018-12-23 08:00
  * UpdateUser:     chenm
  * UpdateDate:     2019-06-15 08:00
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

public class EcgSignalProcessor {
    public static final short INVALID_HR = 0; // 无效心率值
    public static final int HR_FILTER_TIME_IN_SECOND = 10;
    public static final int HR_HISTOGRAM_BAR_NUM = 5;
    private static final String HR_ABNORMAL_WARNER_KEY = "hr_warner"; // 心率异常报警器的String key
    private static final String HR_PROCESSOR_KEY = "hr_processor"; // 心率处理器的String key

    private final EcgMonitorDevice device; // 设备
    private final IEcgCalibrator ecgCalibrator; // 标定处理器
    private final IEcgFilter ecgFilter; // 滤波器
    private QrsDetector qrsDetector; // QRS波检测器，可求心率值
    private final Map<String, IHrOperator> hrOperators; // 心率相关操作Map

    public EcgSignalProcessor(EcgMonitorDevice device) {
        if(device == null) {
            throw new IllegalArgumentException("EcgMonitorDevice is null");
        }

        this.device = device;
        ecgCalibrator = new EcgCalibrator65536(device.getValue1mVBeforeCalibration());
        ecgFilter = new EcgPreFilterWith35HzNotch(device.getSampleRate());
        hrOperators = new ConcurrentHashMap<>();
        HrProcessor hrProcessor = new HrProcessor(HR_FILTER_TIME_IN_SECOND, device);
        hrOperators.put(HR_PROCESSOR_KEY, hrProcessor);
        setHrAbnormalWarner(device.getConfig().isWarnWhenHrAbnormal(), device.getConfig().getHrLowLimit(), device.getConfig().getHrHighLimit());
    }

    public void update() {
        ecgCalibrator.setValue1mVBeforeCalibration(device.getValue1mVBeforeCalibration());
        ecgFilter.updateSampleRate(device.getSampleRate());
        qrsDetector = new QrsDetector(device.getSampleRate(), VALUE_1MV_AFTER_CALIBRATION);
        setHrAbnormalWarner(device.getConfig().isWarnWhenHrAbnormal(), device.getConfig().getHrLowLimit(), device.getConfig().getHrHighLimit());
    }

    public void setHrAbnormalWarner(boolean isWarn, int lowLimit, int highLimit) {
        HrAbnormalWarner hrWarner = (HrAbnormalWarner) hrOperators.get(HR_ABNORMAL_WARNER_KEY);

        if (isWarn) {
            if (hrWarner != null) {
                hrWarner.initialize(lowLimit, highLimit);
            } else {
                hrWarner = new HrAbnormalWarner(device, lowLimit, highLimit);
                hrOperators.put(HR_ABNORMAL_WARNER_KEY, hrWarner);
            }
        } else {
            if (hrWarner != null) {
                hrWarner.close();
            }
            hrOperators.remove(HR_ABNORMAL_WARNER_KEY);
        }
    }

    // 处理Ecg信号
    public void process(int ecgSignal) {
        ecgSignal = (int) ecgFilter.filter(ecgCalibrator.process(ecgSignal)); // 标定,滤波
        device.updateSignalValue(ecgSignal); // 更新信号
        short currentHr = (short) qrsDetector.outputHR(ecgSignal); // 检测Qrs波，获取心率
        // 通知心率值更新
        if(currentHr != INVALID_HR) {
            device.onHrValueUpdated(currentHr);
        }
        // 心率操作
        if (currentHr != INVALID_HR) {
            for (IHrOperator operator : hrOperators.values()) {
                operator.operate(currentHr);
            }
        }
    }

    // 重置心率记录仪
    public void resetHrProcessor() {
        HrProcessor hrProcessor = (HrProcessor) hrOperators.get(HR_PROCESSOR_KEY);

        if(hrProcessor != null) {
            hrProcessor.reset();
        }
    }

    public void updateHrStatisticInfo() {
        HrProcessor hrProcessor = (HrProcessor) hrOperators.get(HR_PROCESSOR_KEY);

        if(hrProcessor != null) {
            hrProcessor.updateHrStatisticInfo();
        }
    }

    public List<Short> getHrList() {
        HrProcessor hrProcessor = (HrProcessor) hrOperators.get(HR_PROCESSOR_KEY);
        if(hrProcessor != null) {
            return hrProcessor.getHrList();
        }
        return null;
    }

    public void close() {
        for(IHrOperator operator : hrOperators.values()) {
            if(operator != null)
                operator.close();
        }
    }

}
