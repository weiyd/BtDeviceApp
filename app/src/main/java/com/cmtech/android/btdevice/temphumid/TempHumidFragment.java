package com.cmtech.android.btdevice.temphumid;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cmtech.android.ble.callback.IBleCallback;
import com.cmtech.android.ble.core.BluetoothGattChannel;
import com.cmtech.android.ble.exception.BleException;
import com.cmtech.android.ble.model.BluetoothLeDevice;
import com.cmtech.android.btdeviceapp.R;
import com.cmtech.android.btdeviceapp.fragment.DeviceFragment;
import com.cmtech.android.btdeviceapp.activity.MainActivity;
import com.cmtech.android.btdeviceapp.model.MyBluetoothDevice;
import com.cmtech.android.btdeviceapp.util.ByteUtil;

import java.util.Arrays;

import static com.cmtech.android.btdevice.temphumid.TempHumidGattSerialExecutor.*;

/**
 * Created by bme on 2018/2/27.
 */

public class TempHumidFragment extends DeviceFragment {
    private static final int MSG_TEMPHUMIDDATA = 0;
    private static final int MSG_TEMPHUMIDCTRL = 1;
    private static final int MSG_TEMPHUMIDPERIOD = 2;

    private TextView tvTempData;
    private TextView tvHumidData;

    private TempHumidGattSerialExecutor serialExecutor;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_TEMPHUMIDDATA) {
                if(msg.obj != null) {
                    byte[] data = (byte[]) msg.obj;
                    byte[] buf = Arrays.copyOfRange(data, 0, 4);
                    float humid = ByteUtil.getFloat(buf);
                    tvHumidData.setText(""+(int)humid);
                    buf = Arrays.copyOfRange(data, 4, 8);
                    float temp = ByteUtil.getFloat(buf);
                    tvTempData.setText(String.format("%.1f", temp));
                }
            } else if (msg.what == MSG_TEMPHUMIDCTRL) {
                //tvCharacteristics.setText("characteristic");
            } else if (msg.what == MSG_TEMPHUMIDPERIOD) {
                //tvDescriptors.setText("descriptor");
            }
        }
    };

    public TempHumidFragment() {

    }

    public static TempHumidFragment newInstance() {
        return new TempHumidFragment();
    }

    @Override
    public void updateDeviceInfo(final MyBluetoothDevice device, int type) {
        if(this.device == device) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    updateConnectState();
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_temphumid, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTempData = (TextView)view.findViewById(R.id.tv_temp_data);
        tvHumidData = (TextView)view.findViewById(R.id.tv_humid_data);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initProcess();
    }

    @Override
    public void initProcess() {
        Log.d("Main Thread", ""+Thread.currentThread().getId());

        serialExecutor = new TempHumidGattSerialExecutor(device.getDeviceMirror());

        Object thermoData = serialExecutor.getGattObject(TEMPHUMIDDATA);
        Object thermoControl = serialExecutor.getGattObject(TEMPHUMIDCTRL);
        Object thermoPeriod = serialExecutor.getGattObject(TEMPHUMIDPERIOD);
        if(thermoData == null || thermoControl == null || thermoPeriod == null) {
            Log.d("TempHumidFragment", "can't find Gatt object of this element on the device.");
            return;
        }



        serialExecutor.addReadCommand(TEMPHUMIDDATA, new IBleCallback() {
            @Override
            public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
                //Log.d("THERMOPERIOD", "first write period: " + HexUtil.encodeHexStr(data));
                handler.sendEmptyMessage(MSG_TEMPHUMIDDATA);
                Log.d("Thread", "Read Callback Thread: "+Thread.currentThread().getId());
            }

            @Override
            public void onFailure(BleException exception) {
                //Log.d("THERMOCONTROL", exception.toString());
            }
        });

        IBleCallback notifyCallback = new IBleCallback() {
            @Override
            public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
                Message msg = new Message();
                msg.what = MSG_TEMPHUMIDDATA;
                msg.obj = data;
                handler.sendMessage(msg);
            }

            @Override
            public void onFailure(BleException exception) {

            }
        };

        serialExecutor.addNotifyCommand(TEMPHUMIDDATACCC, true, new IBleCallback() {
            @Override
            public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
                Log.d("Thread", "Notify Callback Thread: "+Thread.currentThread().getId());
            }

            @Override
            public void onFailure(BleException exception) {

            }
        }, notifyCallback);



        serialExecutor.addWriteCommand(TEMPHUMIDCTRL, new byte[]{0x01}, new IBleCallback() {
            @Override
            public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
                //Log.d("THERMOPERIOD", "second write period: " + HexUtil.encodeHexStr(data));
                handler.sendEmptyMessage(MSG_TEMPHUMIDCTRL);
                Log.d("Thread", "Control Write Callback Thread: "+Thread.currentThread().getId());
            }

            @Override
            public void onFailure(BleException exception) {
                //Log.d("THERMOCONTROL", exception.toString());
            }
        });

        serialExecutor.addWriteCommand(TEMPHUMIDPERIOD, new byte[]{0x0A}, new IBleCallback() {
            @Override
            public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
                //Log.d("THERMOPERIOD", "second write period: " + HexUtil.encodeHexStr(data));
                handler.sendEmptyMessage(MSG_TEMPHUMIDPERIOD);
                Log.d("Thread", "Period Write Callback Thread: "+Thread.currentThread().getId());
            }

            @Override
            public void onFailure(BleException exception) {
                //Log.d("THERMOCONTROL", exception.toString());
            }
        });

        serialExecutor.startExecuteCommand();
    }

    @Override
    public void onDestroy() {
        // 停止命令执行
        serialExecutor.stopExecuteCommand();

        super.onDestroy();
    }

}
