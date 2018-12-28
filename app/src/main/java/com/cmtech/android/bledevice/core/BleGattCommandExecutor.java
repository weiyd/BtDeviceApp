package com.cmtech.android.bledevice.core;

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
 * BleGattCommandExecutor: Ble Gatt命令执行器
 * Created by bme on 2018/3/2.
 *
 */

public class BleGattCommandExecutor{
    private final static int CMD_ERROR_RETRY_TIMES = 3;      // Gatt命令执行错误可重复的次数

    private final DeviceMirror deviceMirror; // 命令执行的设备镜像
    private final Queue<BleGattCommand> commandList = new LinkedList<>(); // 要执行的命令队列
    private BleGattCommand currentCommand; // 当前在执行的命令
    private volatile boolean currentCommandDone = true; // 标记当前命令是否执行完毕
    private Thread executeThread; // 执行命令的线程
    private int cmdErrorTimes = 0; // 命令执行错误的次数


    // IBleCallback的装饰类，在一般的回调任务完成后，执行串行命令所需动作
    private class BleSerialCommandCallback implements IBleCallback {
        private IBleCallback bleCallback;

        BleSerialCommandCallback(IBleCallback bleCallback) {
            this.bleCallback = bleCallback;
        }

        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
            synchronized(BleGattCommandExecutor.this) {
                // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
                // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
                if(currentCommand != null && deviceMirror != null) {
                    deviceMirror.removeBleCallback(currentCommand.getGattInfoKey());
                }

                // 先做一般Gatt成功操作
                if(bleCallback != null)
                    bleCallback.onSuccess(data, bluetoothGattChannel, bluetoothLeDevice);

                // 标记命令执行完毕
                currentCommandDone = true;
                // 命令错误次数归零
                cmdErrorTimes = 0;
                // 通知执行线程执行下一条
                BleGattCommandExecutor.this.notifyAll();

                ViseLog.i("Command return " + HexUtil.encodeHexStr(data));
            }
        }

        @Override
        public void onFailure(BleException exception) {
            synchronized(BleGattCommandExecutor.this) {
                // 清除当前命令的数据操作IBleCallback，否则会出现多次执行该回调.
                // 有可能是ViseBle内部问题，也有可能本身蓝牙就会这样
                if(currentCommand != null && deviceMirror != null) {
                    deviceMirror.removeBleCallback(currentCommand.getGattInfoKey());
                }

                // 有错误，且次数小于指定次数，重新执行当前命令
                if(cmdErrorTimes < CMD_ERROR_RETRY_TIMES && currentCommand != null) {
                    // 再次执行当前命令
                    currentCommand.execute();
                    cmdErrorTimes++;
                    ViseLog.i("Command Retry: " + cmdErrorTimes);
                } else {
                    // 错误次数大于指定次数
                    cmdErrorTimes = 0;
                    // 停止命令执行器
                    stop();
                    // 断开连接
                    if(deviceMirror != null) deviceMirror.disconnect();

                    if(bleCallback != null)
                        bleCallback.onFailure(exception);
                }
                ViseLog.i(currentCommand + " is wrong: " + exception);
            }
        }
    }

    // 构造器：指定设备镜像
    BleGattCommandExecutor(DeviceMirror deviceMirror) {
        this.deviceMirror = deviceMirror;
    }

    /**
     * 将"读element"操作加入串行执行器
     * @param element 数据单元
     * @param dataOpCallback 读回调
     * @return 是否添加成功
     */
    public void addReadCommand(BleGattElement element, IBleCallback dataOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_READ)
                .setDataOpCallback(new BleSerialCommandCallback(dataOpCallback)).build();
        if(command != null)
            addCommandToList(command);
    }

    /**
     * 将"写element"操作加入串行执行器
     * @param element 数据单元
     * @param data 数据
     * @param dataOpCallback 写回调
     * @return 是否添加成功
     */
    public void addWriteCommand(BleGattElement element, byte[] data, IBleCallback dataOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_WRITE)
                .setData(data)
                .setDataOpCallback(new BleSerialCommandCallback(dataOpCallback)).build();
        if(command != null)
            addCommandToList(command);
    }

    // 写单字节数据
    public void addWriteCommand(BleGattElement element, byte data, IBleCallback dataOpCallback) {
        addWriteCommand(element, new byte[]{data}, dataOpCallback);
    }

    /**
     * 将"数据单元Notify"操作加入串行执行器
     * @param element 数据单元
     * @param enable 使能或失能
     * @param dataOpCallback 写回调
     * @param notifyOpCallback Notify数据回调
     * @return 是否添加成功
     */
    public void addNotifyCommand(BleGattElement element, boolean enable
            , IBleCallback dataOpCallback, IBleCallback notifyOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_NOTIFY)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataOpCallback(new BleSerialCommandCallback(dataOpCallback))
                .setNotifyOpCallback(notifyOpCallback).build();
        if(command != null)
            addCommandToList(command);
    }

    /**
     * 生成"数据单元Indicate"命令，并加入串行执行器
     * @param element 数据单元
     * @param enable 使能或失能
     * @param dataOpCallback 写回调
     * @param indicateOpCallback Notify数据回调
     * @return 是否添加成功
     */
    public void addIndicateCommand(BleGattElement element, boolean enable
            , IBleCallback dataOpCallback, IBleCallback indicateOpCallback) {
        BleGattCommand.Builder builder = new BleGattCommand.Builder();
        BleGattCommand command = builder.setDeviceMirror(deviceMirror)
                .setBluetoothElement(element)
                .setPropertyType(PropertyType.PROPERTY_INDICATE)
                .setData((enable) ? new byte[]{0x01} : new byte[]{0x00})
                .setDataOpCallback(new BleSerialCommandCallback(dataOpCallback))
                .setNotifyOpCallback(indicateOpCallback).build();
        if(command != null)
            addCommandToList(command);
    }

    // 添加Instant命令
    public void addInstantCommand(IBleCallback dataOpCallback) {
        BleGattCommand command = new BleGattCommand.Builder().setDataOpCallback(dataOpCallback).setInstantCommand(true).build();
        if(command != null)
            addCommandToList(command);
    }

    private synchronized void addCommandToList(BleGattCommand command) {
        if(!commandList.offer(command)) {
            throw new IllegalStateException();
        }
        // 添加成功，通知执行线程
        notifyAll();
        return;
    }

    // 开始执行命令
    public synchronized void start() {
        if(executeThread != null && executeThread.isAlive()) return;

        executeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ViseLog.i("Start Command Execution Thread: "+Thread.currentThread().getName());
                try {
                    while (!executeThread.isInterrupted()) {
                        executeNextCommand();
                    }
                }catch (InterruptedException e) {
                    ViseLog.i("The serial executor is interrupted!!!!!!");
                } finally {
                    commandList.clear();
                    currentCommand = null;
                    currentCommandDone = false;
                    cmdErrorTimes = 0;
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

        // 取出一条命令执行
        currentCommand = commandList.poll();
        currentCommand.execute();

        ViseLog.i(currentCommand);

        // 设置未完成标记
        if(!currentCommand.isInstantCommand())
            currentCommandDone = false;
    }

    // 停止执行命令
    public void stop() {
        if(isAlive()) {
            executeThread.interrupt();
        }
    }

    // 命令执行器是否存活
    public synchronized boolean isAlive() {
        return ((executeThread != null) && executeThread.isAlive());
    }
}