package com.cmtech.android.bledevicecore.model;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.callback.IConnectCallback;
import com.cmtech.android.ble.callback.scan.IScanCallback;
import com.cmtech.android.ble.callback.scan.SingleFilterScanCallback;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.core.DeviceMirrorPool;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.TimeoutException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.ble.model.BluetoothLeDeviceStore;
import com.cmtech.android.bledeviceapp.MyApplication;
import com.cmtech.android.bledevicecore.devicestate.BleDeviceCloseState;
import com.cmtech.android.bledevicecore.devicestate.BleDeviceConnectedState;
import com.cmtech.android.bledevicecore.devicestate.BleDeviceConnectingState;
import com.cmtech.android.bledevicecore.devicestate.BleDeviceDisconnectState;
import com.cmtech.android.bledevicecore.devicestate.BleDeviceDisconnectingState;
import com.cmtech.android.bledevicecore.devicestate.BleDeviceScanState;
import com.cmtech.android.bledevicecore.devicestate.IBleDeviceState;
import com.vise.log.ViseLog;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by bme on 2018/2/19.
 */

public abstract class BleDevice implements Serializable{
    // 设备镜像池
    private final static DeviceMirrorPool deviceMirrorPool = MyApplication.getViseBle().getDeviceMirrorPool();

    // 连接断开后重连之间的延时，毫秒
    private final static int RECONNECT_DELAY = 10000;

    // 设备基本信息
    private BleDeviceBasicInfo basicInfo;
    public BleDeviceBasicInfo getBasicInfo() {
        return basicInfo;
    }
    public void setBasicInfo(BleDeviceBasicInfo basicInfo) {
        this.basicInfo = basicInfo;
    }

    // 当前已重连次数
    private int curReconnectTimes = 0;

    // ViseBle包内部设备对象
    private BluetoothLeDevice bluetoothLeDevice = null;
    public BluetoothLeDevice getBluetoothLeDevice() {
        return bluetoothLeDevice;
    }

    // GATT命令串行执行器
    private BleGattCommandExecutor commandExecutor;

    // 设备状态观察者列表
    private final List<IBleDeviceStateObserver> deviceStateObserverList = new LinkedList<>();

    // 是否正在关闭。当断开连接时，根据这个标志判断是否应该退回到close状态
    private boolean isClosing = false;
    public boolean isClosing() {
        return isClosing;
    }
    public void setClosing(boolean closing) {
        isClosing = closing;
    }

    // 几个设备状态
    private final BleDeviceCloseState closeState = new BleDeviceCloseState(this);                           // 关闭状态
    private final BleDeviceDisconnectState disconnectState = new BleDeviceDisconnectState(this);            // 连接断开状态
    private final BleDeviceScanState scanState = new BleDeviceScanState(this);                              // 扫描状态
    private final BleDeviceConnectingState connectingState = new BleDeviceConnectingState(this);            // 连接中状态
    private final BleDeviceConnectedState connectedState = new BleDeviceConnectedState(this);               // 已连接状态
    private final BleDeviceDisconnectingState disconnectingState = new BleDeviceDisconnectingState(this);   // 断开中状态
    // 获取设备状态常量
    public BleDeviceCloseState getCloseState() {
        return closeState;
    }
    public BleDeviceDisconnectState getDisconnectState() {
        return disconnectState;
    }
    public BleDeviceScanState getScanState() {
        return scanState;
    }
    public BleDeviceConnectingState getConnectingState() {
        return connectingState;
    }
    public BleDeviceConnectedState getConnectedState() {
        return connectedState;
    }
    public BleDeviceDisconnectingState getDisconnectingState() {
        return disconnectingState;
    }

    // 设备状态变量，初始化为关闭状态
    private IBleDeviceState state = closeState;
    // 设置设备状态
    public void setState(IBleDeviceState state) {
        this.state = state;
        notifyDeviceStateObservers();
    }

    // 扫描回调
    private final IScanCallback scanCallback = new IScanCallback() {
        @Override
        public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {

        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
            if (bluetoothLeDeviceStore.getDeviceList().size() > 0) {
                connectCallback.onScanFinish(true);
                MyApplication.getViseBle().connect(bluetoothLeDeviceStore.getDeviceList().get(0), connectCallback);
            } else {
                connectCallback.onScanFinish(false);
            }
        }

        @Override
        public void onScanTimeout() {
            connectCallback.onScanFinish(false);
        }
    };

    // 连接回调
    private final IConnectCallback connectCallback = new IConnectCallback() {
        // 连接成功回调
        @Override
        public void onConnectSuccess(DeviceMirror mirror) {
            synchronized (BleDevice.this) {
                state.onDeviceConnectSuccess(mirror);
            }
        }
        // 连接失败回调
        @Override
        public void onConnectFailure(final BleException exception) {
            synchronized (BleDevice.this) {
                if (exception instanceof TimeoutException)
                    state.onDeviceConnectTimeout();
                else
                    state.onDeviceConnectFailure();
            }
        }
        // 连接断开回调
        @Override
        public void onDisconnect(final boolean isActive) {
            synchronized (BleDevice.this) {
                state.onDeviceDisconnect();
            }
        }
        // 扫描结束回调
        @Override
        public void onScanFinish(boolean result) {
            synchronized (BleDevice.this) {
                if (result) {
                    state.onDeviceScanSuccess();
                }
                else {
                    state.onDeviceScanFailure();
                }
            }
        }
    };
    public IConnectCallback getConnectCallback() {
        return connectCallback;
    }

    // 创建消息Handler
    private final Handler handler = createMessageHandler();
    private Handler createMessageHandler() {
        HandlerThread thread = new HandlerThread("BleDevice Work Thread");
        thread.start();
        return new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                processGattMessage(msg);
            }
        };
    }
    public Handler getHandler() {
        return handler;
    }

    ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    // 构造器
    public BleDevice(BleDeviceBasicInfo basicInfo) {
        this.basicInfo = basicInfo;
        curReconnectTimes = 0;
        isClosing = false;
    }


    // 获取设备基本信息
    public String getMacAddress() {
        return basicInfo.getMacAddress();
    }
    public String getNickName() {
        return basicInfo.getNickName();
    }
    public String getUuidString() {
        return basicInfo.getUuidString();
    }
    public boolean autoConnect() {
        return basicInfo.autoConnect();
    }
    public String getImagePath() {
        return basicInfo.getImagePath();
    }
    public int getReconnectTimes() { return basicInfo.getReconnectTimes(); }

    // 打开设备
    public synchronized void open() {
        handler.removeCallbacksAndMessages(null);
        curReconnectTimes = 0;
        state.open();
        if(autoConnect())
            state.scan();
    }

    // 关闭设备
    public synchronized void close() {
        handler.removeCallbacksAndMessages(null);
        state.close();
    }

    // 转换设备状态
    public synchronized void switchState() {
        handler.removeCallbacksAndMessages(null);
        curReconnectTimes = 0;
        state.switchState();
    }

    /*// 连接设备，先执行扫描设备，扫描到之后会自动连接设备
    public synchronized void connect() {
        state.scan();
    }*/

    /*// 断开设备
    public synchronized void disconnect() {
        state.disconnect();
    }*/

    // 获取设备状态描述信息
    public synchronized String getStateDescription() {
        return state.getStateDescription();
    }

    // 设备是否可连接
    public synchronized boolean canConnect() {
        return state.canConnect();
    }

    // 设备是否可断开
    public synchronized boolean canDisconnect() {
        return state.canDisconnect();
    }

    // 设备是否可关闭
    public synchronized boolean canClose() {
        return state.canClose();
    }

    // 设备是否已连接
    public synchronized boolean isConnected() {
        return (state == connectedState);
    }


    // 添加Gatt操作命令
    // 添加读取命令
    public synchronized boolean addReadCommand(BleGattElement element, IBleDataOpCallback dataOpCallback) {
        IBleCallback callback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        return ((commandExecutor != null) && commandExecutor.addReadCommand(element, callback));
    }

    // 添加写入多字节命令
    public synchronized boolean addWriteCommand(BleGattElement element, byte[] data, IBleDataOpCallback dataOpCallback) {
        IBleCallback callback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        return ((commandExecutor != null) && commandExecutor.addWriteCommand(element, data, callback));
    }

    // 添加写入单字节命令
    public synchronized boolean addWriteCommand(BleGattElement element, byte data, IBleDataOpCallback dataOpCallback) {
        IBleCallback callback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        return ((commandExecutor != null) && commandExecutor.addWriteCommand(element, data, callback));
    }

    // 添加Notify命令
    public synchronized boolean addNotifyCommand(BleGattElement element, boolean enable
            , IBleDataOpCallback dataOpCallback, IBleDataOpCallback notifyOpCallback) {
        IBleCallback dataCallback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        IBleCallback notifyCallback = (notifyOpCallback == null) ? null : new BleDataOpCallbackAdapter(notifyOpCallback);
        return ((commandExecutor != null) && commandExecutor.addNotifyCommand(element, enable, dataCallback, notifyCallback));
    }

    // 添加Indicate命令
    public synchronized boolean addIndicateCommand(BleGattElement element, boolean enable
            , IBleDataOpCallback dataOpCallback, IBleDataOpCallback indicateOpCallback) {
        IBleCallback dataCallback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        IBleCallback indicateCallback = (indicateOpCallback == null) ? null : new BleDataOpCallbackAdapter(indicateOpCallback);
        return ((commandExecutor != null) && commandExecutor.addIndicateCommand(element, enable, dataCallback, indicateCallback));
    }

    // 添加Instant命令
    public synchronized boolean addInstantCommand(IBleDataOpCallback dataOpCallback) {
        IBleCallback dataCallback = (dataOpCallback == null) ? null : new BleDataOpCallbackAdapter(dataOpCallback);
        return ((commandExecutor != null) && commandExecutor.addInstantCommand(dataCallback));
    }

    // 登记设备状态观察者
    public void registerDeviceStateObserver(IBleDeviceStateObserver observer) {
        if(!deviceStateObserverList.contains(observer)) {
            deviceStateObserverList.add(observer);
        }
    }

    // 删除设备状态观察者
    public void removeDeviceStateObserver(IBleDeviceStateObserver observer) {
        int index = deviceStateObserverList.indexOf(observer);
        if(index >= 0) {
            deviceStateObserverList.remove(index);
        }
    }

    // 通知设备状态观察者
    public void notifyDeviceStateObservers() {
        for(final IBleDeviceStateObserver observer : deviceStateObserverList) {
            if(observer != null) {
                observer.updateDeviceState(BleDevice.this);
            }
        }
    }

    /*
     * 子类需要提供的抽象方法
     */
    // 连接成功后执行的操作
    public abstract boolean executeAfterConnectSuccess();
    // 连接错误后执行的操作
    public abstract void executeAfterConnectFailure();
    // 断开连接后执行的操作
    public abstract void executeAfterDisconnect();
    // 处理Gatt命令回调消息函数
    public abstract void processGattMessage(Message msg);


    ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////
    // 创建Gatt命令执行器，并启动执行器
    private boolean createGattCommandExecutor() {
        ViseLog.i("create new command executor.");
        if(isCommandExecutorAlive()) return false;
        DeviceMirror deviceMirror = deviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        if(deviceMirror == null) return false;

        commandExecutor = new BleGattCommandExecutor(deviceMirror);
        commandExecutor.start();
        return true;
    }

    // 停止Gatt命令执行器
    private void stopCommandExecutor() {
        //if(isCommandExecutorAlive()) commandExecutor.stop();
        if(commandExecutor != null) commandExecutor.stop();
    }

    // Gatt命令执行器是否Alive
    private boolean isCommandExecutorAlive() {
        return ((commandExecutor != null) && commandExecutor.isAlive());
    }

    // 获取设备上element对应的Gatt Object
    protected Object getGattObject(BleGattElement element) {
        DeviceMirror deviceMirror = deviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        if(deviceMirror == null || element == null) return null;
        return element.retrieveGattObject(deviceMirror);
    }

    // 发送Gatt命令执行后回调处理消息
    protected void sendGattMessage(int what, Object obj) {
        Message msg = Message.obtain(handler, what, obj);
        msg.sendToTarget();
        //Message msg = new Message();
        //msg.what = what;
        //msg.obj = obj;
        //handler.sendMessage(msg);
    }

    private void reconnect(final int delay) {
        int canReconnectTimes = getReconnectTimes();
        if(curReconnectTimes < canReconnectTimes || canReconnectTimes == -1) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    state.scan();
                }
            }, delay);
            if(curReconnectTimes < canReconnectTimes)
                curReconnectTimes++;
        }
    }


    /*
     * 给IBleDeviceState提供的函数，执行扫描和连接后的操作
     */
    // 开始扫描
    public void startScan() {
        ViseLog.e("startScan");
        new SingleFilterScanCallback(scanCallback).setDeviceMac(getMacAddress()).setScan(true).scan();
    }

    public void processScanFailure() {
        reconnect(RECONNECT_DELAY);
    }

    public void processConnectSuccess(DeviceMirror mirror) {
        //handler.removeCallbacksAndMessages(null);
        curReconnectTimes = 0;

        bluetoothLeDevice = mirror.getBluetoothLeDevice();

        ViseLog.i("onConnectSuccess");

        // 创建Gatt串行命令执行器
        if(!createGattCommandExecutor() || !executeAfterConnectSuccess()) {
            // 创建失败，断开连接
            /*handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    state.disconnect();
                }
            }, 500);
            */
            //stopCommandExecutor();
            state.disconnect();
        }
    }

    public void processConnectFailure() {
        stopCommandExecutor();
        bluetoothLeDevice = null;
        executeAfterConnectFailure();
        reconnect(RECONNECT_DELAY);
    }

    public void processDisconnect() {
        stopCommandExecutor();
        bluetoothLeDevice = null;
        executeAfterDisconnect();
        ViseLog.e("processDisconnect");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BleDevice that = (BleDevice) o;
        String thisAddress = getMacAddress();
        String thatAddress = that.getMacAddress();

        return thisAddress != null ? thisAddress.equals(thatAddress) : thatAddress == null;
    }

    @Override
    public int hashCode() {
        return getMacAddress() != null ? getMacAddress().hashCode() : 0;
    }

}
