package com.dji.UAVDetection;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class Init extends Application {
    public static final String      FLAG_CONNECTION_CHANGE = "fpv_tutorial_connection_change";
    private             Handler     mHandler;
    private             Application instance;

    public Init() {
    }

    public void setContext(Application application) {
        instance = application;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        //监听SDK注册结果
        DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {
            //监听SDK注册结果
            @Override
            public void onRegister(DJIError djiError) {
                if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                    mHandler.post(() -> Toast.makeText(getApplicationContext(), "SDK注册成功", Toast.LENGTH_SHORT).show());
                    DJISDKManager.getInstance().startConnectionToProduct();
                } else {
                    mHandler.post(() -> Toast.makeText(getApplicationContext(), "SDK注册失败", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onProductDisconnect() {
                notifyStatusChange();
            }

            @Override
            public void onProductConnect(BaseProduct baseProduct) {
                notifyStatusChange();
            }

            @Override
            public void onProductChanged(BaseProduct baseProduct) {
                notifyStatusChange();
            }

            @Override
            public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                if (newComponent != null) {
                    newComponent.setComponentListener(isConnected -> notifyStatusChange());
                }
            }

            @Override
            public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {
            }

            @Override
            public void onDatabaseDownloadProgress(long l, long l1) {
            }

        };
        //注册Android系统6.0以上的应用程序前请检查权限。
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (permissionCheck == 0 && permissionCheck2 == 0)) {
            DJISDKManager.getInstance().registerApp(getApplicationContext(), mDJISDKManagerCallback);
            Toast.makeText(getApplicationContext(), "正在注册，请稍候...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "请检查是否授予权限。", Toast.LENGTH_SHORT).show();
        }
    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private final Runnable updateRunnable = () -> {
        Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
        getApplicationContext().sendBroadcast(intent);
    };

}
