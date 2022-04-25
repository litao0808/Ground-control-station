package com.dji.UAVDetection;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;

import dji.common.error.DJIError;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;

public class GaodeFragment extends Fragment implements View.OnClickListener {
    private View    view    = null;
    private Context context = null;
    //高德地图
    private MapView mapView = null;
    private AMap    aMap    = null;
    // 按钮
    private Button  config, replace, upload, start, stop, locate, clear_point, clear_fly, read_fly, delete_pos, read_pos, enable, add_2,route,read_result,clear_result,hide;
    private boolean  isAdd      = false;
    public  boolean  fodBtnFlag = true;

    // 数据处理
    private AutonomousFlight modelMap;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_gd, container, false);
            context = view.getContext();

            modelMap = new AutonomousFlight(context);

            mapView = (MapView) view.findViewById(R.id.map);   //实例化地图视图
            mapView.onCreate(savedInstanceState);
        }

        initUI();
        initFlightController();
        addWaypointListener();


        // TODO test
        File.getInstance().deleteLatLng("fly");

        File.getInstance().writeLatLng(new LatLng(30.750942, 103.934500), "fly");
        File.getInstance().writeLatLng(new LatLng(30.750942, 103.934000), "fly");
        File.getInstance().writeLatLng(new LatLng(30.751037, 103.934000), "fly");
        File.getInstance().writeLatLng(new LatLng(30.751037, 103.934500), "fly");

        File.getInstance().writeLatLng(new LatLng(30.751131, 103.934500), "fly");
        File.getInstance().writeLatLng(new LatLng(30.751131, 103.934000), "fly");
        File.getInstance().writeLatLng(new LatLng(30.751226, 103.934000), "fly");
        File.getInstance().writeLatLng(new LatLng(30.751226, 103.934500), "fly");


        File.getInstance().deleteLatLng("position");
        //TODO test   路径规划数据测试




        File.getInstance().deleteLatLng("position2");
        File.getInstance().deleteLatLng("result");

        return view;
    }

    private void initUI() {
        locate = view.findViewById(R.id.locate);
        enable = view.findViewById(R.id.enable);
        add_2 = view.findViewById(R.id.add_2);
        config = view.findViewById(R.id.config);

        upload = view.findViewById(R.id.upload);
        start = view.findViewById(R.id.start);
        stop = view.findViewById(R.id.stop);
        replace = view.findViewById(R.id.replace);
        clear_fly = view.findViewById(R.id.clear_fly);
        read_pos = view.findViewById(R.id.read_pos);
        read_fly = view.findViewById(R.id.read_fly);
        clear_point = view.findViewById(R.id.clear_point);
        delete_pos = view.findViewById(R.id.delete_pos);
        route = view.findViewById(R.id.route);
        read_result = view.findViewById(R.id.read_result);
        clear_result = view.findViewById(R.id.clear_result);





        locate.setOnClickListener(this::onClick);
        enable.setOnClickListener(this::onClick);
        add_2.setOnClickListener(this::onClick);
        config.setOnClickListener(this::onClick);

        upload.setOnClickListener(this::onClick);
        start.setOnClickListener(this::onClick);
        stop.setOnClickListener(this::onClick);
        replace.setOnClickListener(this::onClick);
        clear_fly.setOnClickListener(this::onClick);
        read_pos.setOnClickListener(this::onClick);
        read_fly.setOnClickListener(this::onClick);
        clear_point.setOnClickListener(this::onClick);
        delete_pos.setOnClickListener(this::onClick);
        route.setOnClickListener(this::onClick);
        read_result.setOnClickListener(this::onClick);
        clear_result.setOnClickListener(this::onClick);






        // 高德地图相关
        if (aMap == null) {
            aMap = mapView.getMap();
            modelMap.aMap = aMap;
            aMap.setOnMapClickListener(latLng -> {
                if (isAdd) modelMap.markWrite2(latLng);
            });
        }
        CoordinateConverter converter  = new CoordinateConverter();
// CoordType.GPS 待转换坐标类型
        converter.from(CoordinateConverter.CoordType.GPS);
// sourceLatLng待转换坐标点 LatLng类型
        converter.coord(new LatLng(modelMap.droneLocationLat, modelMap.droneLocationLng));
// 执行转换操作
        LatLng cd = converter.convert();
        //LatLng cd = new LatLng(modelMap.droneLocationLat, modelMap.droneLocationLng);
        float zoomlevel = (float) 20.0;
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cd, zoomlevel));//按照传入的CameraUpdate 参数移动可视区域。
        modelMap.readSDcard();
        // 设置按钮组件是否可见
        if (fodBtnFlag) setViewGone();
        else setViewVisible();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.config: {     // 配置航点任务
                modelMap.showSettingDialog();
                break;
            }

            case R.id.replace: {    // 清除航点任务
                modelMap.replaceFunc();
                break;
            }
            case R.id.upload: {     //上传航点任务
                modelMap.uploadWayPointMission();
                break;
            }
            case R.id.start: {  //开始航点任务
                modelMap.startWaypointMission();
                break;
            }
            case R.id.stop: {
                modelMap.stopWaypointMission();
                break;
            }
            case R.id.locate: {
                updateDroneLocation();
                modelMap.cameraUpdate();
                break;
            }
            case R.id.clear_fly: {
                modelMap.clearFlyFunc();
                break;
            }
            case R.id.enable: {
                enableDisableAdd();
                break;
            }
            case R.id.add_2: {     // 将飞机当前位置，在地图上标点，并且写入航路文件
                if (isAdd) {
                    LatLng qidian = new LatLng(modelMap.droneLocationLat, modelMap.droneLocationLng);
                    modelMap.markWrite(qidian);
                }
                break;
            }
            case R.id.read_pos: {
                modelMap.readSDcard();
                break;
            }
            case R.id.read_fly: {
                modelMap.readFlyData();
                break;

            }
            case R.id.clear_point: {
                modelMap.clear_pointFunc();
                break;
            }
            case R.id.delete_pos: {
                modelMap.delete_posFunc();
                break;
            }
            case R.id.route: {
                modelMap.routePlan("position");
                break;
            }


            default:
                break;
        }
    }

    private void addWaypointListener() {      //添加航点任务操作类
        if (modelMap.getWaypointMissionOperator() != null) {   //如果此时有航点任务操作类
            modelMap.getWaypointMissionOperator().addListener(new WaypointMissionOperatorListener() {
                @Override
                public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent downloadEvent) {
                }

                @Override
                public void onUploadUpdate(@NonNull WaypointMissionUploadEvent uploadEvent) {
                }

                @Override
                public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent executionEvent) {
                }

                @Override
                public void onExecutionStart() {
                }

                @Override
                public void onExecutionFinish(@Nullable final DJIError error) {
                    modelMap.showToast("执行完成：" + (error == null ? "Success!" : error.getDescription()));
                }
            });    //添加侦听器以侦听事件。
        }
    }

    private void initFlightController() {
        FlightController mFlightController = null;
        BaseProduct baseProduct = Tools.getProductInstance();
        if (baseProduct != null && baseProduct.isConnected()) {
            if (baseProduct instanceof Aircraft) {
                mFlightController = ((Aircraft) baseProduct).getFlightController();
            }
        }
        if (mFlightController != null) {
            mFlightController.setStateCallback(flightControllerState -> {
                /*纬度获取*/
                modelMap.droneLocationLat = flightControllerState.getAircraftLocation().getLatitude();
                /*经度获取*/
                modelMap.droneLocationLng = flightControllerState.getAircraftLocation().getLongitude();
                updateDroneLocation();    // 实时将飞机图标绘制在地图上
                modelMap.droneSpeedX = flightControllerState.getVelocityX();
                modelMap.droneSpeedY = flightControllerState.getVelocityY();
            });
        }
    }

    private Marker droneMarker = null;

    // Update the drone location based on states from MCU.
    void updateDroneLocation() {
        CoordinateConverter converter  = new CoordinateConverter();
// CoordType.GPS 待转换坐标类型
        converter.from(CoordinateConverter.CoordType.GPS);
// sourceLatLng待转换坐标点 LatLng类型
        converter.coord(new LatLng(modelMap.droneLocationLat, modelMap.droneLocationLng));
// 执行转换操作
        //LatLng showPoint = converter.convert();
        LatLng pos = converter.convert();
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);//Marker 的图标会根据Marker.position 位置渲染在地图上
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        getActivity().runOnUiThread(() -> {
            if (droneMarker != null) {
                droneMarker.remove();
            }
            if (modelMap.checkGpsCoordination(modelMap.droneLocationLat, modelMap.droneLocationLng)) {
                droneMarker = aMap.addMarker(markerOptions);
            }
        });
    }

    private void enableDisableAdd() {
        if (!isAdd) {
            isAdd = true;
            enable.setText("点击退出");
            enable.setActivated(true);
            add_2.setActivated(true);
        } else {
            isAdd = false;
            enable.setText("点击添加");
            enable.setActivated(false);
            add_2.setActivated(false);
        }
    }

    private void setViewGone() {
        LinearLayout linearLayout = view.findViewById(R.id.gdBtn);
        linearLayout.setVisibility(View.GONE);

    }

    private void setViewVisible() {
        LinearLayout linearLayout = view.findViewById(R.id.gdBtn);
        linearLayout.setVisibility(View.VISIBLE);

    }


}