package com.cmtech.android.bledeviceapp.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cmtech.android.bledeviceapp.R;
import com.cmtech.android.bledeviceapp.model.AccountManager;
import com.vise.log.ViseLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.smssdk.SMSSDK;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

/**
  *
  * ClassName:      SplashActivity
  * Description:    登录Activity
  * Author:         chenm
  * CreateDate:     2018/10/27 09:18
  * UpdateUser:     chenm
  * UpdateDate:     2019-04-24 09:18
  * UpdateRemark:   更新说明
  * Version:        1.0
 */
public class SplashActivity extends AppCompatActivity {
    private static final int MSG_COUNT_DOWN = 1; // 倒计时消息
    private static final int COUNT_DOWN_SECOND = 3; // 倒计时秒数

    private TextView tvCountDownTime;
    private Thread countThread; // 倒计时线程

    private final Handler countHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what == MSG_COUNT_DOWN) {
                int nSecond = msg.arg1;
                SplashActivity.this.tvCountDownTime.setText(String.valueOf(nSecond) + "秒");

                if(nSecond == 0) {
                    Intent intent = new Intent(SplashActivity.this, HuaweiLoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 检查权限
        checkPermissions();
    }
    // 检查权限
    private void checkPermissions() {
        List<String> permission = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(SplashActivity.this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permission.add(ACCESS_COARSE_LOCATION);
            }
        }
        if(ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(permission.size() != 0)
            ActivityCompat.requestPermissions(SplashActivity.this, permission.toArray(new String[0]), 1);
        else
            initialize();
    }

    private void initialize() {
        TextView tvWelcome = findViewById(R.id.tv_welcometext);
        String welcomeText = String.format(getResources().getString(R.string.welcome_text_format), getResources().getString(R.string.app_name));
        tvWelcome.setText(welcomeText);

        tvCountDownTime = findViewById(R.id.tv_count_down_time);
        tvCountDownTime.setText(String.valueOf(COUNT_DOWN_SECOND) + "秒");

        startCountDown();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                for(int result : grantResults) {
                    if(result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "没有必要的权限，程序无法正常运行", Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                    }
                }
                break;
        }

        initialize();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            stopCountDown();
        } catch (InterruptedException ignored) {
        }
    }

    private void startCountDown() {
        countThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int nSecond = COUNT_DOWN_SECOND;
                try {
                    while (--nSecond >= 0) {
                        Thread.sleep(1000);
                        Message.obtain(countHandler, MSG_COUNT_DOWN, nSecond, 0).sendToTarget();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        countThread.start();
    }

    private void stopCountDown() throws InterruptedException{
        if (countThread != null && countThread.isAlive()) {
            countThread.interrupt();
            countThread.join();
        }
    }

}