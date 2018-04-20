package com.cmtech.android.btdevice.ecgmonitor;

/**
 * Created by bme on 2018/4/21.
 */

public enum EcgLeadType {
    LEAD_I(0, "I"),
    LEAD_II(1, "II"),
    LEAD_III(2, "III");

    EcgLeadType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    private int code;
    private String description;

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static String getDescriptionFromCode(int code) {
        for(EcgLeadType ele : EcgLeadType.values()) {
            if(ele.code == code) {
                return ele.description;
            }
        }
        return "";
    }

    public static EcgLeadType getFromCode(int code) {
        for(EcgLeadType ele : EcgLeadType.values()) {
            if(ele.code == code) {
                return ele;
            }
        }
        return null;
    }
}
