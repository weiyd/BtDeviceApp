package com.cmtech.android.btdeviceapp.model;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.common.PropertyType;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by bme on 2018/3/2.
 * Gatt数据操作命令的串行执行器
 */

public class GattCommandSerialExecutor {
    private final int ERROR_TIMES = 3;      // Gatt命令执行错误可重复的次数

    // 指定的设备镜像
    private final DeviceMirror deviceMirror;

    // 要执行的命令队列
    private final Queue<BleGattCommand> commandList = new LinkedList<>();

    // 当前在执行的命令
    private BleGattCommand currentCommand;

    // 标记当前命令是否执行完毕
    private volatile boolean currentCommandDone = true;

    // 执行命令的线程
    private Thread executeThread;

    // 命令执行错误的次数
    private int errorTimes = 0;


    // IBleCallback的装饰者，在一般的回调任务完成后，执行串行命令所需动作
    private class BleSerialCommandCallback implements IBleCallback {
        IBleCallback bleCallback;

        public BleSerialCommandCallback(IBleCallback bleCallback) {
            this.bleCallback = bleCallback;
        }

        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
            synchronized(GattCommandSerialExecutor.this) {
                // 先做一般操作
                if(bleCallback != null)
                    bleCallback.onSuccess(data, bluetoothGattChannel, bluetoothLeDevice);

                currentCommandSuccess();

                ViseLog.i("The returned data is " + HexUtil.encodeHexStr(data));
            }
        }

        @Override
        public void onFailure(BleException exception) {
            synchronized(GattCommandSerialExecutor.this) {
                if(bleCallback != null)
                    bleCallback.onFailure(exception);

                currentCommandFail();
                ViseLog.i("GattCommandSerialExecutor Wrong: " + exception);
            }
        }
    }

    // 构造器：指定设备镜像
    public GattCommandSerialExecutor(DeviceMirror deviceMirror) {
        this.deviceMirror = deviceMirror;
    }

    /**
     * 将"读数据单元"操作加入串行执行器
     * @param element 数据单元
     * @param dataOpCallback 读回调
     * @return 是否添加成功
     */
    public boolean addReadCommand(BleGattElement element, IBleCallback dataOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_READ)
                .setDataOpCallback(new BleSerialCommandCallback(dataOpCallback)).build();
        if(command == null) return false;
        return addCommandToList(command);
    }

    /**
     * 将"写数据单元"操作加入串行执行器
     * @param element 数据单元
     * @param data 数据
     * @param dataOpCallback 写回调
     * @return 是否添加成功
     */
    public boolean addWriteCommand(BleGattElement element, byte[] data, IBleCallback dataOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_WRITE)
                .setData(data)
                .setDataOpCallback(new BleSerialCommandCallback(dataOpCallback)).build();
        if(command == null) return false;
        return addCommandToList(command);
    }

    // 写单字节数据
    public boolean addWriteCommand(BleGattElement element, byte data, IBleCallback dataOpCallback) {
        return addWriteCommand(element, new byte[]{data}, dataOpCallback);
    }

    /**
     * 将"数据单元Notify"操作加入串行执行器
     * @param element 数据单元
     * @param enable 使能或失能
     * @param dataOpCallback 写回调
     * @param notifyOpCallback Notify数据回调
     * @return 是否添加成功
     */
    public boolean addNotifyCommand(BleGattElement element, boolean enable
            , IBleCallback dataOpCallback, IBleCallback notifyOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_NOTIFY)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataOpCallback(new BleSerialCommandCallback(dataOpCallback))
                .setNotifyOpCallback(notifyOpCallback).build();
        if(command == null) return false;
        return addCommandToList(command);
    }

    /**
     * 生成"数据单元Indicate"命令，并加入串行执行器
     * @param element 数据单元
     * @param enable 使能或失能
     * @param dataOpCallback 写回调
     * @param indicateOpCallback Notify数据回调
     * @return 是否添加成功
     */
    public boolean addIndicateCommand(BleGattElement element, boolean enable
            , IBleCallback dataOpCallback, IBleCallback indicateOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_INDICATE)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataOpCallback(new BleSerialCommandCallback(dataOpCallback))
                .setNotifyOpCallback(indicateOpCallback).build();
        if(command == null) return false;
        return addCommandToList(command);
    }

    private synchronized boolean addCommandToList(BleGattCommand command) {
        boolean flag = commandList.offer(command);

        // 添加成功，通知执行线程
        if(flag) notifyAll();

        return flag;
    }

    private synchronized void currentCommandSuccess() {
        // 标记命令执行完毕
        currentCommandDone = true;
        // 命令错误次数归零
        errorTimes = 0;
        // 通知执行线程执行下一条
        GattCommandSerialExecutor.this.notifyAll();
    }

    private synchronized void currentCommandFail() {
        // 有错误，且次数小于ERROR_TIMES次，重新执行当前命令
        if(errorTimes < ERROR_TIMES) {
            // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
            // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
            if(currentCommand != null) {
                if(deviceMirror != null) deviceMirror.removeBleCallback(currentCommand.getGattInfoKey());
            }

            currentCommand.execute();
            errorTimes++;
            ViseLog.i("Retry current command, times = " + errorTimes);

        } else {
            // 错误次数大于ERROR_TIMES次，断开连接
            errorTimes = 0;
            if (executeThread != null && executeThread.isAlive()) executeThread.interrupt();
            deviceMirror.disconnect();

        }
    }


    // 开始执行命令
    public synchronized void start() {
        if(executeThread != null && executeThread.isAlive()) return;

        executeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ViseLog.i("Execution Thread: "+Thread.currentThread().getName());
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        executeNextCommand();
                    }
                }catch (InterruptedException e) {
                    ViseLog.i("The serial executor is interrupted!!!!!!");
                } finally {
                    commandList.clear();
                }
            }
        });

        executeThread.start();
    }

    private synchronized void executeNextCommand() throws InterruptedException{
        // 如果当前命令还没有执行完毕，或者命令队列为空，就等待
        while(!currentCommandDone || commandList.isEmpty()) {
            wait();
        }
        // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
        // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
        if(currentCommand != null) {
            if(deviceMirror != null) deviceMirror.removeBleCallback(currentCommand.getGattInfoKey());
        }

        // 取出一条命令执行
        currentCommand = commandList.poll();
        if(currentCommand != null) currentCommand.execute();

        ViseLog.i("Executing command: " + currentCommand);

        // 设置未完成标记
        currentCommandDone = false;
    }

    // 停止执行命令
    public synchronized void stop() {
        if(isAlive()) executeThread.interrupt();

        /*try {
            executeThread.interrupt();
            executeThread.join();
            executeThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

    public synchronized boolean isAlive() {
        return ((executeThread != null) && executeThread.isAlive());
    }
}
