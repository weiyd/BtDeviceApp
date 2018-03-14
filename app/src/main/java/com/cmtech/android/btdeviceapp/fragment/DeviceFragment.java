package com.cmtech.android.btdeviceapp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cmtech.android.ble.callback.IConnectCallback;
import com.cmtech.android.ble.core.DeviceMirror;
import com.cmtech.android.ble.core.DeviceMirrorPool;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.exception.TimeoutException;
import com.cmtech.android.btdeviceapp.MyApplication;
import com.cmtech.android.btdeviceapp.R;
import com.cmtech.android.btdeviceapp.model.DeviceState;
import com.cmtech.android.btdeviceapp.model.GattSerialExecutor;
import com.cmtech.android.btdeviceapp.model.IConnectSuccessCallback;
import com.cmtech.android.btdeviceapp.model.MyBluetoothDevice;

/**
 * Created by bme on 2018/2/27.
 */

public abstract class DeviceFragment extends Fragment implements IDeviceFragment {
    // 对应的设备
    protected MyBluetoothDevice device;

    // 观察者接口对象
    protected IDeviceFragmentObserver fragmentObserver;

    // 连接状态tv
    protected TextView tvConnectState;

    protected Button btnChangeConnect;
    protected Button btnClose;


    public DeviceFragment() {

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvConnectState = view.findViewById(R.id.device_connect_state_tv);
        btnChangeConnect = view.findViewById(R.id.device_changeconnect_btn);
        btnClose = view.findViewById(R.id.device_close_btn);

        btnChangeConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(device == null) return;
                switch (device.getDeviceState()) {
                    case CONNECT_SUCCESS:
                    case CONNECT_PROCESS:
                        disconnectDevice();
                        break;

                    default:
                        connectDevice();
                        break;
                }
            }
        });


        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(DeviceFragment.this.getClass().getSimpleName(), "is closed.");

                close();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(!(context instanceof IDeviceFragmentObserver)) {
            throw new IllegalStateException("context没有实现IDeviceFragmentListener接口");
        }

        // 获取listener
        fragmentObserver = (IDeviceFragmentObserver) context;

        // 获取device
        device = fragmentObserver.findDeviceFromFragment(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // 更新连接状态
        updateConnectState();
    }

    @Override
    public void onStart() {
        super.onStart();

        // 执行初始化
        initializeGatt();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        fragmentObserver = null;

        disconnectDevice();

        //device = null;
    }



    /////////////// 下面是作为IMyBluetoothDeviceObserver接口要实现的函数//////////////////////
    @Override
    public void updateDeviceInfo(final MyBluetoothDevice device, final int type) {
        if(device != null && device.getFragment() == this) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    switch (type) {
                        case TYPE_MODIFY_CONNECTSTATE:
                            updateConnectState();
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }
    /////////////// 下面是作为IMyBluetoothDeviceObserver接口要实现的函数结束//////////////////////




    /////////////// 下面是作为IDeviceFragment接口要实现的函数//////////////////////
    @Override
    public void updateConnectState() {
        if(device != null) {
            tvConnectState.setText(device.getDeviceState().getDescription());
            switch (device.getDeviceState()) {
                case CONNECT_SUCCESS:
                case CONNECT_PROCESS:
                    btnChangeConnect.setText("断开");
                    break;
                default:
                    btnChangeConnect.setText("连接");
                    break;
            }
        }
    }

    @Override
    public void connectDevice() {
        if(device == null) return;
        DeviceState state = device.getDeviceState();
        if(state == DeviceState.CONNECT_SUCCESS || state == DeviceState.CONNECT_PROCESS) return;

        device.connect(new IConnectSuccessCallback() {
            @Override
            public void doAfterConnectSuccess(MyBluetoothDevice device) {
                initializeGatt();
            }
        });
    }

    @Override
    public void disconnectDevice() {
        // 断开设备
        if(device != null) device.disconnect();
    }

    @Override
    public void close() {
        // 断开设备
        if(device != null) device.disconnect();

        // 让观察者删除此Fragment
        if(fragmentObserver != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    fragmentObserver.closeFragment(DeviceFragment.this);
                }
            });
        }
    }

    /////////////// 下面是作为IDeviceFragment接口要实现的函数结束//////////////////////




}
