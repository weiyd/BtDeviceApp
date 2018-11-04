package com.cmtech.android.bledeviceapp.model;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

public class UserAccount  extends LitePalSupport implements Serializable{
    // 数据库保存的字段
    // id
    private int id;

    private String accountName = "";

    private String password = "";

    private String userName = "匿名";

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
