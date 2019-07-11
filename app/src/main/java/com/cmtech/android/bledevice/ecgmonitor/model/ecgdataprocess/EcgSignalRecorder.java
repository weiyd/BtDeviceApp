package com.cmtech.android.bledevice.ecgmonitor.model.ecgdataprocess;

import com.cmtech.android.bledevice.ecgmonitor.model.EcgMonitorDevice;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgappendix.EcgNormalComment;
import com.cmtech.android.bledevice.ecgmonitor.model.ecgfile.EcgFile;

import java.io.IOException;

/**
 * EcgSignalRecorder: 心电信号记录仪，包含心电信号的文件记录，以及留言信息的管理
 * Created by Chenm, 2018-12-27
 */

public class EcgSignalRecorder {

    private final EcgFile ecgFile;

    private int recordDataNum = 0;

    private int sampleRate; // 采样频率

    private boolean isRecord = false;

    private final EcgNormalComment comment; // 当前信号的留言

    private final EcgMonitorDevice device;

    public EcgSignalRecorder(EcgMonitorDevice device, int sampleRate, EcgFile ecgFile) {
        this.device = device;

        this.sampleRate = sampleRate;

        this.ecgFile = ecgFile;

        comment = EcgNormalComment.createDefaultComment();
    }

    // 获取记录的秒数
    public int getSecond() {
        return recordDataNum /sampleRate;
    }

    // 获取记录的数据个数
    public long getDataNum() {
        return recordDataNum;
    }

    public EcgNormalComment getComment() {
        return comment;
    }

    public boolean isRecord() {
        return isRecord;
    }

    public void setRecord(boolean record) {
        isRecord = record;
    }

    // 记录心电信号
    public synchronized void record(int ecgSignal) throws IOException{
        if(isRecord) {
            ecgFile.writeData(ecgSignal);
            recordDataNum++;
            device.onRecordSecNumUpdated(getSecond());
        }
    }

    public synchronized void close() {
        isRecord = false;
    }

    // 添加留言的内容
    public void addCommentContent(String content) {
        if(isRecord)
            comment.appendContent(content);
    }
}