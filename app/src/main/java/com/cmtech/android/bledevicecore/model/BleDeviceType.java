package com.cmtech.android.bledevicecore.model;

import com.cmtech.android.bledeviceapp.R;

public class BleDeviceType {

    //支持的设备类型信息，以后要保存到数据库中，并从数据库中加载

    // 支持的设备类型的16位UUID的字符串
    private static final String UUID_SIMPLE128GATTPROFILE = "aa10";
    private static final String UUID_HEIGHTSCALE = "aa20";
    private static final String UUID_THERMOMETER = "aa30";
    private static final String UUID_ECGMONITOR = "aa40";
    private static final String UUID_SIGGENERATOR = "aa50";
    private static final String UUID_TEMPHUMID = "aa60";
    private static final String UUID_UNKNOWN = "0000";

    // 支持的设备类型的缺省名称
    private static final String NAME_SIMPLE128GATTPROFILE = "简单GATT例程";
    private static final String NAME_HEIGHTSCALE = "高度计";
    private static final String NAME_THERMOMETER = "体温计";
    private static final String NAME_ECGMONITOR = "心电监护仪";
    private static final String NAME_SIGGENERATOR = "信号发生器";
    private static final String NAME_TEMPHUMID = "温湿度计";
    private static final String NAME_UNKNOWN = "未知设备";


    private static final BleDeviceType DEVTYPE_THERMOMETER =
            new BleDeviceType(UUID_THERMOMETER, R.drawable.thermo_image, NAME_THERMOMETER);
    private static final BleDeviceType DEVTYPE_ECGMONITOR =
            new BleDeviceType(UUID_ECGMONITOR, R.drawable.ecgmonitor_image, NAME_ECGMONITOR);
    private static final BleDeviceType DEVTYPE_TEMPHUMID =
            new BleDeviceType(UUID_TEMPHUMID, R.drawable.temphumid_image, NAME_TEMPHUMID);
    private static final BleDeviceType DEVTYPE_UNKNOWN =
            new BleDeviceType(UUID_UNKNOWN, R.mipmap.ic_unknown_128px, NAME_UNKNOWN);

    private String uuid;        // 设备16位UUID字符串
    private Integer image;      // 缺省图标
    private String name;        // 缺省设备名

    private BleDeviceType(String uuid, Integer image, String name) {
        this.uuid = uuid;
        this.image = image;
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getImage() {
        return image;
    }

    public void setImage(Integer image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static BleDeviceType fromUuid(String uuid) {
        if(DEVTYPE_THERMOMETER.uuid.equalsIgnoreCase(uuid)) return DEVTYPE_THERMOMETER;
        if(DEVTYPE_ECGMONITOR.uuid.equalsIgnoreCase(uuid)) return DEVTYPE_ECGMONITOR;
        if(DEVTYPE_TEMPHUMID.uuid.equalsIgnoreCase(uuid)) return DEVTYPE_TEMPHUMID;
        return DEVTYPE_UNKNOWN;
    }
}
