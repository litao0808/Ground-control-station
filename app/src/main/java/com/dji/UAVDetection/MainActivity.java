package com.dji.UAVDetection;

import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;

import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;

public class MainActivity extends FragmentActivity {
    private volatile     boolean     changeFlag  = true;
    private static       boolean     viewFlag    = true;
    public  static       Button      button;

    private GaodeFragment gdFragment  = null;
    private com.dji.UAVDetection.DetectionFragment FODFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
    }

    private void initUI() {
        // 初始化主画面
        initView();
        // 切换主画面
        button = findViewById(R.id.change);
        button.setOnClickListener(
                v -> {
                    if (changeFlag) { //主画面FPV
                        changeFlag = false;
                        changeToDetect();
                    } else {   //主画面地图
                        changeFlag = true;
                        changeToMap();
                    }
                }
        );


        // 展开小画面
        dji.ux.widget.dashboard.CompassWidget compassWidget = findViewById(R.id.Compass);

        FrameLayout frameLayout = findViewById(R.id.left_bottom);
        //compassWidget.setOnClickListener(v -> frameLayout.setVisibility(View.VISIBLE));
        compassWidget.setOnClickListener(v -> {


                frameLayout.setVisibility(View.VISIBLE);

        });
        // 关闭小画面
        Button buttonclose = findViewById(R.id.close_map);
        buttonclose.setOnClickListener(v -> frameLayout.setVisibility(View.GONE));
    }

    private void initView() {   //初始大小窗界面
        AsyncTask.execute(() -> {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            gdFragment = null;
            gdFragment = new GaodeFragment();
            transaction.replace(R.id.mainView, gdFragment);
            FODFragment = null;
            FODFragment = new DetectionFragment();
            transaction.replace(R.id.smallView, FODFragment);
            transaction.commit();
            gdFragment.fodBtnFlag = false;
            FODFragment.fodViewFlag = true;

        });
    }

    private void changeToDetect() {  //大窗设置为检测界面
        AsyncTask.execute(() -> {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            FODFragment = null;
            FODFragment = new DetectionFragment();
            transaction.replace(R.id.mainView, FODFragment);
            gdFragment = null;
            gdFragment = new GaodeFragment();
            transaction.replace(R.id.smallView, gdFragment);
            transaction.commit();
            gdFragment.fodBtnFlag = true;
            FODFragment.fodViewFlag = false;
        });
    }

    private void changeToMap() {  //大窗设置为地图界面
        AsyncTask.execute(() -> {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            gdFragment = null;
            gdFragment = new GaodeFragment();
            transaction.replace(R.id.mainView, gdFragment);
            FODFragment = null;
            FODFragment = new DetectionFragment();
            transaction.replace(R.id.smallView, FODFragment);
            transaction.commit();
            gdFragment.fodBtnFlag = false;
            FODFragment.fodViewFlag = true;
        });
    }
}


