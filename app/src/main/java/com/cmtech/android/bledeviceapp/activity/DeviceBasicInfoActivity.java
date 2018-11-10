package com.cmtech.android.bledeviceapp.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.cmtech.android.bledevice.SupportedDeviceType;
import com.cmtech.android.bledeviceapp.MyApplication;
import com.cmtech.android.bledeviceapp.R;
import com.cmtech.android.bledevicecore.model.BleDeviceBasicInfo;
import com.vise.utils.file.FileUtil;
import com.vise.utils.view.BitmapUtil;

import java.io.File;

/**
 *  DeviceBasicInfoActivity: 设备基本信息Activity，可用于修改BleDeviceBasicInfo字段
 *  Created by bme on 2018/6/27.
 */

public class DeviceBasicInfoActivity extends AppCompatActivity {
    public static final File IMAGEDIR = MyApplication.getContext().getExternalFilesDir("images");

    private Button btnCancel;
    private Button btnOk;
    private Button btnDefault;
    private EditText etName;
    private ImageView ivImage;
    private CheckBox cbIsAutoconnect;
    private EditText etReconnectTimes;

    // 设备基本信息
    private BleDeviceBasicInfo basicInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_device_basicinfo);

        Intent intent = getIntent();
        if(intent != null) {
            basicInfo = (BleDeviceBasicInfo) intent.getSerializableExtra("devicebasicinfo");
        }

        if(!IMAGEDIR.exists()) {
            IMAGEDIR.mkdir();
        }

        if(basicInfo == null || !IMAGEDIR.exists()) {
            finish();
        }

        // 设置标题为设备地址
        setTitle("设备："+basicInfo.getMacAddress());

        // 设置设备名
        etName = findViewById(R.id.cfg_device_nickname);
        String deviceName = basicInfo.getNickName();
        if("".equals(deviceName)) {
            deviceName = SupportedDeviceType.getDeviceTypeFromUuid(basicInfo.getUuidString()).getDefaultNickname();
        }
        etName.setText(deviceName);

        // 设置设备图像
        ivImage = findViewById(R.id.cfg_device_image);
        String imagePath = basicInfo.getImagePath();
        if("".equals(imagePath)) {
            Glide.with(this).load(SupportedDeviceType.getDeviceTypeFromUuid(basicInfo.getUuidString()).getDefaultImage()).into(ivImage);
        } else {
            Drawable drawable = new BitmapDrawable(MyApplication.getContext().getResources(), imagePath);
            ivImage.setImageDrawable(drawable);
        }
        ivImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAlbum();
            }
        });

        // 设置重连次数
        etReconnectTimes = findViewById(R.id.cfg_device_reconnecttimes);
        etReconnectTimes.setText(""+basicInfo.getReconnectTimes());

        // 设置打开后是否自动重连
        cbIsAutoconnect = findViewById(R.id.cfg_device_isautoconnect);
        cbIsAutoconnect.setChecked(basicInfo.autoConnect());


        btnCancel = findViewById(R.id.register_device_cancel_btn);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });


        btnOk = findViewById(R.id.register_device_ok_btn);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                basicInfo.setNickName(etName.getText().toString());

                // 把图像缩小，保存为macAddress.jpg文件
                String imagePath = basicInfo.getImagePath();
                if(!"".equals(imagePath)) {
                    Bitmap bitmap = BitmapUtil.getSmallBitmap(imagePath, 100, 100);
                    File toFile = FileUtil.getFile(IMAGEDIR, basicInfo.getMacAddress() + ".jpg");
                    BitmapUtil.saveBitmap(bitmap, toFile);
                    basicInfo.setImagePath(toFile.getAbsolutePath());
                }

                basicInfo.setAutoConnect(cbIsAutoconnect.isChecked());

                basicInfo.setReconnectTimes(Integer.parseInt(etReconnectTimes.getText().toString()));

                Intent intent = new Intent();
                intent.putExtra("devicebasicinfo", basicInfo);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        btnDefault = findViewById(R.id.btn_registerdevice_default);
        btnDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restoreDefaultSetup();
            }
        });

    }

    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if(resultCode == RESULT_OK) {
                    if(Build.VERSION.SDK_INT >= 19) {
                        basicInfo.setImagePath(handleImageOnKitKat(data));
                    } else {
                        basicInfo.setImagePath(handleImageBeforeKitKat(data));
                    }
                }
                break;
            default:
                break;
        }
    }

    @TargetApi(19)
    private String handleImageOnKitKat(Intent data) {
        String imagePath = "";
        Uri uri = data.getData();
        if(DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }

        } else if("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }
        displayImage(imagePath);
        return imagePath;
    }

    private String handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
        return imagePath;
    }

    private String getImagePath(Uri uri, String selection) {
        String path = "";
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(String imagePath) {
        Glide.with(MyApplication.getContext()).load(imagePath).centerCrop().into(ivImage);
    }

    private void restoreDefaultSetup() {
        String deviceName = SupportedDeviceType.getDeviceTypeFromUuid(basicInfo.getUuidString()).getDefaultNickname();
        basicInfo.setNickName(deviceName);
        etName.setText(deviceName);

        basicInfo.setImagePath("");
        Glide.with(this).load(SupportedDeviceType.getDeviceTypeFromUuid(basicInfo.getUuidString()).getDefaultImage()).into(ivImage);
    }
}
