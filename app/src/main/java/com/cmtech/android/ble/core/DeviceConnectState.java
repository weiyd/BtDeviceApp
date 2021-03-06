package com.cmtech.android.ble.core;

import com.cmtech.android.bledeviceapp.R;
import com.cmtech.android.bledeviceapp.global.MyApplication;

/**
  *
  * ClassName:      DeviceState
  * Description:    device connect state
  * Author:         chenm
  * CreateDate:     2018/4/21 下午4:47
  * UpdateUser:     chenm
  * UpdateDate:     2019/4/19 下午4:47
  * UpdateRemark:   更新说明
  * Version:        1.0
 */

public class DeviceConnectState {
    private static final int CLOSED_CODE = 0x00; // closed
    private static final int CONNECT_CODE = 0x01; // connected
    private static final int FAILURE_CODE = 0x02;// connect failure
    private static final int DISCONNECT_CODE = 0x03; // disconnect
    private static final int CONNECTING_CODE = 0x04; // connecting
    private static final int DISCONNECTING_CODE = 0x05; // disconnecting

    public static final DeviceConnectState CLOSED = new DeviceConnectState(CLOSED_CODE, MyApplication.getStr(R.string.closed), R.mipmap.ic_disconnect_32px);
    public static final DeviceConnectState CONNECT = new DeviceConnectState(CONNECT_CODE, MyApplication.getStr(R.string.connected), R.mipmap.ic_connected_32px);
    public static final DeviceConnectState FAILURE = new DeviceConnectState(FAILURE_CODE, MyApplication.getStr(R.string.connect_failure), R.mipmap.ic_disconnect_32px);
    public static final DeviceConnectState DISCONNECT = new DeviceConnectState(DISCONNECT_CODE, MyApplication.getStr(R.string.disconnected), R.mipmap.ic_disconnect_32px);
    public static final DeviceConnectState CONNECTING = new DeviceConnectState(CONNECTING_CODE, MyApplication.getStr(R.string.connecting), R.drawable.connecting);
    public static final DeviceConnectState DISCONNECTING = new DeviceConnectState(DISCONNECTING_CODE, MyApplication.getStr(R.string.disconnecting), R.drawable.connecting);

    private final int code; // code
    private String description; // state description
    private int icon; // state icon

    private DeviceConnectState(int code, String description, int icon) {
        this.code = code;
        this.description = description;
        this.icon = icon;
    }

    public int getCode() {
        return code;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public int getIcon() {
        return icon;
    }
    public void setIcon(int icon) {
        this.icon = icon;
    }

    @Override
    public String toString() {
        return description;
    }
}
