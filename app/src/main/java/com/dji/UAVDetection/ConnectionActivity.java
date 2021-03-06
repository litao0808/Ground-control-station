package com.dji.UAVDetection;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class ConnectionActivity extends Activity {
    private static final String   TAG = ConnectionActivity.class.getName();
    private              TextView mTextConnectionStatus;
    private              TextView mTextProduct;
    private              Button   mBtnOpen;
    private static final String[]          REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private final        List<String>      missingPermission        = new ArrayList<>();
    private final        AtomicBoolean     isRegistrationInProgress = new AtomicBoolean(false);
    private static final int               REQUEST_PERMISSION_CODE  = 12345;
    protected            BroadcastReceiver mReceiver                = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();  //??????SDK?????????UI??????
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //1???????????????
        checkAndRequestPermissions();
        //2???????????????
        setContentView(R.layout.activity_connection);
        //3????????????????????????
        initUI();
        // 4?????????????????????????????????????????????????????????
        IntentFilter filter = new IntentFilter();
        filter.addAction(Init.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }

    /**
     * ???????????????????????????????????????
     * ???????????????????????????????????????
     */
    private void checkAndRequestPermissions() {
        // ??????????????????
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // ??????????????????????????????
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, missingPermission.toArray(new String[missingPermission.size()]), REQUEST_PERMISSION_CODE);
        }
    }

    private void initUI() {
        //????????????
        mTextConnectionStatus = findViewById(R.id.text_connection_status);
        //????????????
        mTextProduct = findViewById(R.id.text_product_info);
        //??????????????????????????????
        mBtnOpen = findViewById(R.id.btn_open);


        //???????????????
        mBtnOpen.setOnClickListener((view) -> {


                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);

        });
    /*    btn_class = findViewById(R.id.btn_class);
        btn_class.setOnClickListener((view) -> {
            Intent intent = new Intent(this, ClassActivity.class);
            startActivity(intent);
        });*/
        //SDK??????????????????
        TextView mVersionTv = findViewById(R.id.textView2);
        mVersionTv.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getRegistrationSDKVersion()));
    }

    /**
     * ??????????????????????????????
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // ????????????????????????????????????????????????
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // ????????????????????????????????????????????????
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("????????????!!!");
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(() -> {
                showToast("????????????????????????...");
                DJISDKManager.getInstance().registerApp(getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                    @Override
                    public void onRegister(DJIError djiError) {
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                            DJISDKManager.getInstance().startConnectionToProduct();
                            showToast("????????????");
                        } else {
                            showToast("??????sdk?????????????????????????????????");
                        }
                    }

                    @Override
                    public void onProductDisconnect() {
                        showToast("??????????????????");
                    }

                    @Override
                    public void onProductConnect(BaseProduct baseProduct) {
                        showToast("????????????");
                    }

                    @Override
                    public void onProductChanged(BaseProduct baseProduct) {

                    }

                    @Override
                    public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                        if (newComponent != null) {
                            newComponent.setComponentListener(isConnected -> Log.d(TAG, "onComponentConnectivityChanged: " + isConnected));
                        }
                    }

                    @Override
                    public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {
                    }

                    @Override
                    public void onDatabaseDownloadProgress(long l, long l1) {
                    }
                });
            });
        }
    }

    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = Tools.getProductInstance();
        if (null != mProduct && mProduct.isConnected()) {
            String str = mProduct instanceof Aircraft ? "DJI?????????" : "DJI?????????";
            mTextConnectionStatus.setText("????????????: " + str + " ?????????");

            if (null != mProduct.getModel()) {
                mTextProduct.setText("???????????????" + mProduct.getModel().getDisplayName());
            } else {
                mTextProduct.setText(R.string.product_information);
            }

        } else {
            mTextProduct.setText(R.string.product_information);
            mTextConnectionStatus.setText(R.string.connection_loose);
        }
    }

    private void showToast(final String toastMsg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
