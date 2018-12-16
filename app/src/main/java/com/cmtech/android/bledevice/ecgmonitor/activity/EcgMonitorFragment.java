package com.cmtech.android.bledevice.ecgmonitor.activity;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cmtech.android.bledevice.ecgmonitor.model.EcgMonitorConfiguration;
import com.cmtech.android.bledevice.ecgmonitor.model.EcgMonitorDevice;
import com.cmtech.android.bledevice.ecgmonitor.model.EcgMonitorState;
import com.cmtech.android.bledevice.ecgmonitor.model.IEcgMonitorObserver;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgfile.EcgAbnormal;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgfile.EcgLeadType;
import com.cmtech.android.bledevice.view.ScanWaveView;
import com.cmtech.android.bledeviceapp.MyApplication;
import com.cmtech.android.bledeviceapp.R;
import com.cmtech.android.bledeviceapp.util.DateTimeUtil;
import com.cmtech.android.bledevicecore.BleDeviceFragment;
import com.cmtech.dsp.seq.RealSeq;
import com.cmtech.dsp.util.SeqUtil;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.vise.log.ViseLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EcgMonitorFragment: 心电带设备Fragment
 * Created by bme on 2018/3/13.
 */

public class EcgMonitorFragment extends BleDeviceFragment implements IEcgMonitorObserver {
    private static final String TAG = "EcgMonitorFragment";

    private TextView tvEcgSampleRate;
    private TextView tvEcgLeadType;
    private TextView tvEcg1mV;
    private TextView tvEcgHr;
    private TextView tvEcgRecordTime;
    private ScanWaveView ecgView;
    private ImageButton ibSwitchSampleEcg;
    private ImageButton ibRecord;
    private CheckBox cbEcgFilter;
    private FrameLayout flEcgView;
    private RelativeLayout rlHrStatistics;
    private BarChart hrBarHistogram;
    private TextView tvHrTotal;
    private ImageButton ibResetHistogram;
    private ImageButton ibSetup;


    private BarDataSet hrBarDateSet;
    private List<BarEntry> hrBarEntries = new ArrayList<>();
    private List<String> hrBarXStrings = new ArrayList<>();

    // 标记异常留言的Button
    private List<Button> commentBtnList = new ArrayList<>();
    int[] commentBtnId = new int[]{R.id.btn_ecgfeel_0, R.id.btn_ecgfeel_1, R.id.btn_ecgfeel_2};

    private EcgMonitorDevice device;                // 保存设备模型

    private AudioTrack audioTrack;

    public EcgMonitorFragment() {

    }

    public static BleDeviceFragment newInstance(String macAddress) {
        BleDeviceFragment fragment = new EcgMonitorFragment();
        return BleDeviceFragment.pushMacAddressIntoFragment(macAddress, fragment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        device = (EcgMonitorDevice) getDevice();
        //device.registerEcgMonitorObserver(EcgMonitorFragment.this);

        return inflater.inflate(R.layout.fragment_ecgmonitor, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvEcgSampleRate = view.findViewById(R.id.tv_ecg_samplerate);
        tvEcgSampleRate.setText(String.valueOf(device.getSampleRate()));

        tvEcgLeadType = view.findViewById(R.id.tv_ecg_leadtype);
        tvEcgLeadType.setText("L"+device.getLeadType().getDescription());

        tvEcg1mV = view.findViewById(R.id.tv_ecg_1mv);
        tvEcg1mV.setText(String.valueOf(device.getValue1mVBeforeCalibrate()) + '/' + String.valueOf(device.getValue1mVAfterCalibrate()));

        tvEcgHr = view.findViewById(R.id.tv_ecg_hr);
        tvEcgHr.setText(String.valueOf(device.getCurrentHr()));
        tvEcgHr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hrStatistics();
            }
        });

        ecgView = view.findViewById(R.id.rwv_ecgview);
        updateEcgView(device.getxPixelPerData(), device.getyValuePerPixel(), device.getPixelPerGrid());

        tvEcgRecordTime = view.findViewById(R.id.tv_ecg_recordtime);
        tvEcgRecordTime.setText(DateTimeUtil.secToTime(device.getRecordSecond()));

        ibSwitchSampleEcg = view.findViewById(R.id.ib_ecgreplay_startandstop);
        updateState(device.getState());
        ibSwitchSampleEcg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                device.switchSampleState();
            }
        });

        for(int i = 0; i < commentBtnId.length; i++) {
            Button button = view.findViewById(commentBtnId[i]);
            final String commentDescription = EcgAbnormal.getDescriptionFromCode(i);
            button.setText(commentDescription);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    device.addComment(device.getRecordSecond(), commentDescription);
                }
            });
            commentBtnList.add(button);
        }

        ibRecord = view.findViewById(R.id.ib_ecg_record);
        // 根据设备的isRecord初始化Record按钮
        updateRecordStatus(device.isRecord());
        ibRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isRecord = !device.isRecord();
                device.setEcgRecord(isRecord);

                /*for(Button button : commentBtnList) {
                    button.setEnabled(isRecord);
                }*/
            }
        });

        cbEcgFilter = view.findViewById(R.id.cb_ecg_filter);
        cbEcgFilter.setChecked(device.isFilter());
        cbEcgFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                device.hookEcgFilter(b);
            }
        });

        flEcgView = view.findViewById(R.id.fl_ecgview);
        rlHrStatistics = view.findViewById(R.id.rl_hrstatistics);
        hrBarHistogram = view.findViewById(R.id.bc_hr_histogram);
        initBarChart(hrBarHistogram);
        updateHrBarData(device.getHrStatistics());
        hrBarDateSet = initBarDataSet("心率值统计", Color.BLUE, Color.BLACK);
        showBarChart();
        tvHrTotal = view.findViewById(R.id.tv_ecghr_totaltimes);

        ibResetHistogram = view.findViewById(R.id.ib_reset_histogram);
        ibResetHistogram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                device.resetHrStatistics();
                updateBarChart(null);
            }
        });

        ibSetup = view.findViewById(R.id.ib_ecgmonitor_setup);
        ibSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), EcgMonitorConfigureActivity.class);
                EcgMonitorConfiguration configuration = new EcgMonitorConfiguration();
                intent.putExtra("configuration", configuration);
                startActivity(intent);
            }
        });

        device.registerEcgMonitorObserver(EcgMonitorFragment.this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(device != null)
            device.removeEcgMonitorObserver();

    }

    @Override
    public void updateState(final EcgMonitorState state) {
        if(state.canStart()) {
            ibSwitchSampleEcg.setImageDrawable(ContextCompat.getDrawable(MyApplication.getContext(), R.mipmap.ic_ecg_play_48px));
            ibSwitchSampleEcg.setClickable(true);
        } else if(state.canStop()) {
            ibSwitchSampleEcg.setImageDrawable(ContextCompat.getDrawable(MyApplication.getContext(), R.mipmap.ic_ecg_pause_48px));
            ibSwitchSampleEcg.setClickable(true);
        } else {
            ibSwitchSampleEcg.setClickable(false);
        }
    }

    @Override
    public void updateSampleRate(final int sampleRate) {
        tvEcgSampleRate.setText(String.valueOf(sampleRate));
    }

    @Override
    public void updateLeadType(final EcgLeadType leadType) {
        tvEcgLeadType.setText("L"+leadType.getDescription());
    }

    @Override
    public void updateCalibrationValue(final int calibrationValueBefore, final int calibrationValueAfter) {
        tvEcg1mV.setText(String.valueOf(calibrationValueBefore) + '/' + String.valueOf(calibrationValueAfter));
    }

    @Override
    public void updateRecordStatus(final boolean isRecord) {
        int imageId;
        if (isRecord)
            imageId = R.mipmap.ic_ecg_record_start;
        else
            imageId = R.mipmap.ic_ecg_record_stop;
        ibRecord.setImageDrawable(ContextCompat.getDrawable(MyApplication.getContext(), imageId));
        for(Button button : commentBtnList) {
            button.setEnabled(isRecord);
        }
    }

    @Override
    public void updateEcgView(final int xPixelPerData, final float yValuePerPixel, final int gridPixels) {
        ecgView.setResolution(xPixelPerData, yValuePerPixel);
        ecgView.setGridPixels(gridPixels);
        ecgView.setZeroLocation(0.5);
        ecgView.initView();
    }

    @Override
    public void updateEcgSignal(final int ecgSignal) {
        ecgView.showData(ecgSignal);
    }

    @Override
    public void updateRecordSecond(final int second) {
        tvEcgRecordTime.setText(DateTimeUtil.secToTime(second));
    }

    @Override
    public void updateEcgHr(final int hr) {
        tvEcgHr.setText(String.valueOf(hr));
    }

    @Override
    public void notifyHrWarn() {
        ViseLog.e("Hr Warn!");

        if(audioTrack == null) {
            initHrWarnAudioTrack();
            audioTrack.play();
        } else {
            switch(audioTrack.getPlayState()) {
                case AudioTrack.PLAYSTATE_PAUSED:
                case AudioTrack.PLAYSTATE_PLAYING:
                    audioTrack.stop();
                    audioTrack.reloadStaticData();
                    audioTrack.play();
                    break;
                case AudioTrack.PLAYSTATE_STOPPED:
                    audioTrack.reloadStaticData();
                    audioTrack.play();
                    break;
            }
        }
    }

    private void initHrWarnAudioTrack() {
        int length = 4000;
        int fs = 1000;
        RealSeq sinSeq = SeqUtil.createSinSeq(127.0, fs, 0, 44100, length);
        byte[] wave = new byte[length];
        for(int i = 0; i < wave.length; i++) {
            wave[i] = (byte)(double)sinSeq.get(i);
        }

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_8BIT, length, AudioTrack.MODE_STATIC);
        audioTrack.write(wave, 0, wave.length);
        audioTrack.write(wave, 0, wave.length);
    }

    private void hrStatistics() {
        if(device.getHrStatistics() != null) {
            int[] hrHistogram = Arrays.copyOf(device.getHrStatistics(), device.getHrStatistics().length);
            if(rlHrStatistics.getVisibility() == View.INVISIBLE) {
                updateBarChart(hrHistogram);
                rlHrStatistics.setVisibility(View.VISIBLE);
                flEcgView.setVisibility(View.INVISIBLE);
            }
            else {
                rlHrStatistics.setVisibility(View.INVISIBLE);
                flEcgView.setVisibility(View.VISIBLE);
            }
        }
    }

    private class StringAxisValueFormatter implements IAxisValueFormatter {
        private List<String> mStrs;
        /**
     * 对字符串类型的坐标轴标记进行格式化
     * @param strs
     */
        public StringAxisValueFormatter(List<String> strs){
            this.mStrs =strs;
        }

        @Override
        public String getFormattedValue(float value, AxisBase axisBase) {
            int index = (int) value;
            if (index < 0 || index >= mStrs.size()) {
                return "";
            } else {
                return mStrs.get(index);
            }
        }
    }

    private void initBarChart(BarChart barChart) {
        /***图表设置***/
        //背景颜色
        barChart.setBackgroundColor(Color.WHITE);
        //不显示图表网格
        barChart.setDrawGridBackground(false);
        //背景阴影
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(false);
        //显示边框
        barChart.setDrawBorders(true);
        //设置动画效果
        barChart.animateY(1000, Easing.Linear);
        barChart.animateX(1000, Easing.Linear);

        /***XY轴的设置***/
        //X轴设置显示位置在底部
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
        xAxis.setDrawAxisLine(false);
        xAxis.setValueFormatter(new StringAxisValueFormatter(hrBarXStrings));

        YAxis leftAxis = barChart.getAxisLeft();
        YAxis rightAxis = barChart.getAxisRight();
        //保证Y轴从0开始，不然会上移一点
        leftAxis.setAxisMinimum(0f);
        rightAxis.setAxisMinimum(0f);
        leftAxis.setDrawAxisLine(false);
        rightAxis.setDrawAxisLine(false);

        /***折线图例 标签 设置***/
        Legend legend = barChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(11f);
        //显示位置
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        //是否绘制在图表里面
        legend.setDrawInside(false);

        Description description = new Description();
        description.setEnabled(false);
        barChart.setDescription(description);

        barChart.setDrawValueAboveBar(true);
    }

    private void updateHrBarData(int[] hrData) {
        hrBarXStrings.clear();
        hrBarEntries.clear();
        if(hrData == null || hrData.length < 4) {
            hrBarXStrings.add("无有效心率");
            hrBarEntries.add(new BarEntry(0, 0.0f));
        } else {
            int j = 0;
            hrBarXStrings.add("30以下");
            hrBarEntries.add(new BarEntry(j++, (float) (hrData[0] + hrData[1] + hrData[2])));
            for (int i = 3; i < hrData.length - 1; i++) {
                if (hrData[i] > 5) {
                    String key = String.valueOf(i * 10) + '-' + String.valueOf((i + 1) * 10);
                    hrBarXStrings.add(key);
                    hrBarEntries.add(new BarEntry(j++, (float) hrData[i]));
                }
            }
            hrBarXStrings.add("200以上");
            hrBarEntries.add(new BarEntry(j, (float) hrData[hrData.length - 1]));
        }
    }

    /**
     * 柱状图始化设置 一个BarDataSet 代表一列柱状图
     *
     * @param barColor      柱状图颜色
     */
    private BarDataSet initBarDataSet(String legendString, int barColor, int dataColor) {
        // 每一个BarDataSet代表一类柱状图
        BarDataSet barDataSet = new BarDataSet(hrBarEntries, legendString);

        barDataSet.setColor(barColor);
        barDataSet.setFormLineWidth(1f);
        barDataSet.setFormSize(15.f);
        //不显示柱状图顶部值
        barDataSet.setDrawValues(true);
        barDataSet.setValueTextSize(14f);
        barDataSet.setValueTextColor(dataColor);
        barDataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return String.valueOf((int) value);
            }
        });
        return barDataSet;
    }

    public void showBarChart() {
        BarData data = new BarData(hrBarDateSet);
        hrBarHistogram.setData(data);
        hrBarHistogram.invalidate();

    }

    public void updateBarChart(int[] hrHistogram) {
        updateHrBarData(hrHistogram);
        hrBarDateSet.setValues(hrBarEntries);
        showBarChart();
        int sum = 0;

        if(hrHistogram != null) {
            for (int num : hrHistogram) {
                sum += num;
            }
        }
        tvHrTotal.setText(String.valueOf(sum));
    }
}
