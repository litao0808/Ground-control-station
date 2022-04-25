package com.dji.UAVDetection;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.sdkmanager.DJISDKManager;

public class AutonomousFlight {
    private static final String                  TAG              = "ModelMap";
    private final        Handler                 myHandler        = new Handler(Looper.getMainLooper());
    private              Context                 myContext;
    public               AMap                    aMap;
    public static        double                  droneLocationLat = 30.750942;    // 初始在电子科技大学足球场
    public static        double                  droneLocationLng = 103.934500;
    public               WaypointMissionOperator waypointMissionOperator;
    public static        float                   droneSpeedX      = 10f;
    public static        float                   droneSpeedY      = 10f;

    //public static final double x = -0.00272f;
    //public static final double y = 0.002f;


    public AutonomousFlight(@NonNull Context context) {
        myContext = context;
    }

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (waypointMissionOperator == null) {
            waypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return waypointMissionOperator;
    }

    // config ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    private float                         altitude        = 100.0f;
    private float                         mSpeed          = 10.0f;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode    mHeadingMode    = WaypointMissionHeadingMode.AUTO;
    private WaypointMission.Builder       waypointMissionBuilder;    //航路点任务建立类

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void showSettingDialog() {    //设置参数
        LinearLayout wayPointSettings = (LinearLayout) LayoutInflater.from(myContext).inflate(R.layout.dialog_waypointsetting, null);
        //弹出一个对话框，上面为对话框的内容
        final TextView wpAltitude_TV = wayPointSettings.findViewById(R.id.altitude);   //点组实例化
        EditText editText = wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = wayPointSettings.findViewById(R.id.heading);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mSpeed = Float.parseFloat(s.toString());
            }
        });
        //点组监听器
        actionAfterFinished_RG.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.finishNone) {
                mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
            } else if (checkedId == R.id.finishGoHome) {
                mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
            } else if (checkedId == R.id.finishAutoLanding) {
                mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
            } else if (checkedId == R.id.finishToFirst) {
                mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
            }
        });
        heading_RG.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.headingNext) {
                mHeadingMode = WaypointMissionHeadingMode.AUTO;
            } else if (checkedId == R.id.headingInitDirec) {
                mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
            } else if (checkedId == R.id.headingRC) {
                mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
            } else if (checkedId == R.id.headingWP) {
                mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
            }
        });
        //点击完成按钮监听器
        //点击取消按钮监听器
        new AlertDialog.Builder(myContext)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("完成", (dialog, id) -> {
                    String altitudeString = wpAltitude_TV.getText().toString();
                    altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                    configWayPointMission();    //设置航路点属性
                })
                .setNegativeButton("取消", (dialog, id) -> dialog.cancel())
                .create()
                .show();
    }

    private String nulltoIntegerDefalt(String value) {
        if (!isIntValue(value)) value = "0";
        return value;
    }

    private boolean isIntValue(String val) {
        try {
            val = val.replace(" ", "");
            Integer.parseInt(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void configWayPointMission() {
        if (waypointMissionBuilder == null) {    //目前没有，航路点任务建立，类
            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        } else {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }

        if (waypointMissionBuilder.getWaypointList().size() > 0) {   //列表不为空

            for (int i = 0; i < waypointMissionBuilder.getWaypointList().size(); i++) {
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }
            showToast("成功设置航路点姿态");
        }
        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            showToast("加载航路点成功");
        } else {
            showToast("加载航路点失败：" + error.getDescription());
        }
    }

    public void configWayPointMission2() {
        altitude = 7.0f;
        mSpeed = 10.0f;
        mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
        mHeadingMode = WaypointMissionHeadingMode.AUTO;
        if (waypointMissionBuilder == null) {    //目前没有，航路点任务建立，类
            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        } else {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }
       // altitude = 7.0f;
        if (waypointMissionBuilder.getWaypointList().size() > 0) {   //列表不为空
            for (int i = 0; i < waypointMissionBuilder.getWaypointList().size(); i++) {
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }
            showToast("成功设置航路点姿态");
        }
        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            showToast("加载航路点成功");
        } else {
            showToast("加载航路点失败：" + error.getDescription());
        }
    }

    public void showToast(final String string) {
        myHandler.post(() -> Toast.makeText(myContext, string, Toast.LENGTH_SHORT).show());
    }

    // replace ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    private final List<Waypoint> waypointList = new ArrayList<>();   //飞机航路点

    public void replaceFunc() {
        waypointList.clear();
        try {
            waypointMissionBuilder.waypointList(waypointList);
        } catch (NullPointerException e) {
            showToast("waypointMissionBuilder为空指针！");
            e.printStackTrace();
        }
    }

    // upload ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    public void uploadWayPointMission() {
        getWaypointMissionOperator().uploadMission(error -> {
            if (error == null) {
                showToast("任务上传成功!");
            } else {
                showToast("任务上传失败, 错误: " + error.getDescription() + " 重试...");
                getWaypointMissionOperator().retryUploadMission(null);
            }
        });
    }

    // start ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    public void startWaypointMission() {
        getWaypointMissionOperator().startMission(error -> showToast("任务开始: " + (error == null ? "开始成功" : error.getDescription())));
    }

    // stop ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    public void stopWaypointMission() {
        getWaypointMissionOperator().stopMission(error -> showToast("任务停止: " + (error == null ? "停止成功" : error.getDescription())));
    }

    // locate ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    public boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    public void cameraUpdate() {    //视图革新
        CoordinateConverter converter  = new CoordinateConverter();
// CoordType.GPS 待转换坐标类型
        converter.from(CoordinateConverter.CoordType.GPS);
// sourceLatLng待转换坐标点 LatLng类型
        converter.coord(new LatLng(droneLocationLat, droneLocationLng));
// 执行转换操作
        LatLng pos = converter.convert();

        float zoomlevel = (float) 20.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);//返回一个CameraUpdate 对象，包括可视区域框移动目标点屏幕中心位置的经纬度以及缩放级别。
        aMap.moveCamera(cu);
    }

    // clear_fly ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    public void clearFlyFunc() {
        File.getInstance().deleteLatLng("fly");
    }

    // add_2 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();

    public void markWrite(LatLng point) {
        useColorMarker(point, "BLUE");
        File.getInstance().writeLatLng(point, "fly");
    }

    public void markWrite2(LatLng point) {
        useColorMarker2(point, "BLUE");
        double p[] = transformGCJ02ToWGS84(point.longitude, point.latitude);
        LatLng point2 = new LatLng(p[1], p[0]);
        File.getInstance().writeLatLng(point2, "fly");
    }

    private static final double x_PI =3.14159265358979324*3000.0/180.0;
    private static final double PI =3.1415926535897932384626;
    private static final double a =6378245.0;
    private static final double ee =0.00669342162296594323;


    public static double[] transformGCJ02ToWGS84(double lng,double lat){
        if(outOfChina(lng, lat)){
            return new double[]{lng, lat};
        }else{
            double dLat =transformLat(lng -105.0, lat -35.0);
            double dLng =transformLng(lng -105.0, lat -35.0);
            double radLat = lat /180.0* PI;
            double magic = Math.sin(radLat);
            magic =1- ee * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            dLat =(dLat *180.0)/((a *(1- ee))/(magic * sqrtMagic)* PI);
            dLng =(dLng *180.0)/(a / sqrtMagic * Math.cos(radLat)* PI);
            double mgLat = lat + dLat;
            double mgLng = lng + dLng;
            return new double[]{lng *2- mgLng, lat *2- mgLat};
        }
    }

    private static double transformLat(double lng,double lat){
        double ret =-100.0+2.0* lng +3.0* lat +0.2* lat * lat +0.1* lng * lat +0.2* Math.sqrt(Math.abs(lng));
        ret +=(20.0* Math.sin(6.0* lng * PI)+20.0* Math.sin(2.0* lng * PI))*2.0/3.0;
        ret +=(20.0* Math.sin(lat * PI)+40.0* Math.sin(lat /3.0* PI))*2.0/3.0;
        ret +=(160.0* Math.sin(lat /12.0* PI)+320* Math.sin(lat * PI /30.0))*2.0/3.0;
        return ret;
    }

    private static double transformLng(double lng,double lat){
        double ret =300.0+ lng +2.0* lat +0.1* lng * lng +0.1* lng * lat +0.1* Math.sqrt(Math.abs(lng));
        ret +=(20.0* Math.sin(6.0* lng * PI)+20.0* Math.sin(2.0* lng * PI))*2.0/3.0;
        ret +=(20.0* Math.sin(lng * PI)+40.0* Math.sin(lng /3.0* PI))*2.0/3.0;
        ret +=(150.0* Math.sin(lng /12.0* PI)+300.0* Math.sin(lng /30.0* PI))*2.0/3.0;
        return ret;
    }

    public static boolean outOfChina(double lng,double lat){
        return(lng <72.004|| lng >137.8347)||(lat <0.8293|| lat >55.8271);
    }



    public void useColorMarker2(LatLng point, @NonNull String color) {
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(point.latitude, point.longitude));//Marker 的图标会根据Marker.position 位置渲染在地图上
        switch (color) {
            case "BLUE":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                break;
            case "RED":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                break;
            case "YELLOW":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                break;
            default:
                break;
        }
        Marker marker = aMap.addMarker(markerOptions);//加一个Marker（标记）到地图上。
        mMarkers.put(mMarkers.size(), marker);
    }

    // 航迹点用BLUE，目标位置点用RED
    public void useColorMarker(LatLng point, @NonNull String color) {
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        CoordinateConverter converter  = new CoordinateConverter();
// CoordType.GPS 待转换坐标类型
        converter.from(CoordinateConverter.CoordType.GPS);
// sourceLatLng待转换坐标点 LatLng类型
        converter.coord(new LatLng(point.latitude, point.longitude));
// 执行转换操作
       // LatLng showPoint = converter.convert();
        markerOptions.position(converter.convert());//Marker 的图标会根据Marker.position 位置渲染在地图上
        switch (color) {
            case "BLUE":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                break;
            case "RED":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                break;
            case "YELLOW":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                break;
            default:
                break;
        }
        Marker marker = aMap.addMarker(markerOptions);//加一个Marker（标记）到地图上。
        mMarkers.put(mMarkers.size(), marker);
    }

    public void join(@NonNull LatLng point) {
        Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);

        //向航路点阵列列表添加航路点；
        if (waypointMissionBuilder != null) {
            waypointList.add(mWaypoint);
            waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
        } else {
            waypointMissionBuilder = new WaypointMission.Builder();
            waypointList.add(mWaypoint);
            waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
        }
    }

    public void join2(@NonNull LatLng point) {
        Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
        mWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,-90));
        mWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,1));
        mWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,-60));
        mWaypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,-90));
        mWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,1));
        mWaypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,0));
        mWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,1));
        mWaypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,90));
        mWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,1));
        mWaypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,180));
        mWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,1));
        //向航路点阵列列表添加航路点；
        if (waypointMissionBuilder != null) {
            waypointList.add(mWaypoint);
            waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
        } else {
            waypointMissionBuilder = new WaypointMission.Builder();
            waypointList.add(mWaypoint);
            waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
        }
    }

    // read_pos ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    public void readSDcard() {
        ArrayList<String> arrayList = new ArrayList<>();

        File.getInstance().readLatLng(arrayList, "position");

        // 对ArrayList中存储的字符串进行处理
        int len = arrayList.size();
        for (int i = 0; i < len; i++) {
            String s = arrayList.get(i);
            String[] tokens = s.split(",");
            double[] data = new double[tokens.length];
            for (int j = 0; j < data.length; j++)
                data[j] = Double.parseDouble(tokens[j]);
            LatLng fod = new LatLng(data[0], data[1]);

            useColorMarker(fod, "RED");
        }
    }

    // read_fly ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    public void readFlyData() {

        waypointList.clear(); //清除航点任务列表，避免重复上传

        ArrayList<String> arrayList = new ArrayList<>();
        List<LatLng> latLngs = new ArrayList<LatLng>();

        File.getInstance().readLatLng(arrayList, "fly");

        // 对ArrayList中存储的字符串进行处理
        int len = arrayList.size();
        LatLng fod;
        for (int i = 0; i < len; i++) {
            String s = arrayList.get(i);
            String[] tokens = s.split(",");
            double[] data = new double[tokens.length];
            for (int j = 0; j < data.length; j++)
                data[j] = Double.parseDouble(tokens[j]);
            fod = new LatLng(data[0], data[1]);

            markAndLink(latLngs, fod, "BLUE", i + 1 + "");
            join(fod);
        }

    }



    private void markAndLink(@NonNull List<LatLng> latLngs, LatLng point, @NonNull String color, String number) {
        // 将当前点加入到列表
        CoordinateConverter converter  = new CoordinateConverter();
// CoordType.GPS 待转换坐标类型
        converter.from(CoordinateConverter.CoordType.GPS);
// sourceLatLng待转换坐标点 LatLng类型
        converter.coord(new LatLng(point.latitude, point.longitude));
// 执行转换操作
        LatLng showPoint = converter.convert();
       // LatLng showPoint = new LatLng(point.latitude + x, point.longitude + y);
        latLngs.add(showPoint);

        // 选择点的样式
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(showPoint);//Marker 的图标会根据Marker.position 位置渲染在地图上
        switch (color) {
            case "BLUE":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                break;
            case "RED":
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                break;
            default:
                break;
        }

        // 画点连线
        Marker marker = aMap.addMarker(markerOptions);//加一个Marker（标记）到地图上。
        String s1 = point.latitude + "";
        String s2 = point.longitude + "";
        marker.setTitle(number + "\n" + s1 + "\n" + s2);
        mMarkers.put(mMarkers.size(), marker);
        aMap.addPolyline(new PolylineOptions()
                .addAll(latLngs)
                .width(5)
                .color(Color.argb(255, 255, 1, 1))
        );
    }

    // clear_point ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    public void clear_pointFunc() {
        aMap.clear();
    }

    // delete_pos ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    public void delete_posFunc() {
        File.getInstance().deleteLatLng("position");
    }


    // other ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
    public void routePlan(String path) {

        waypointList.clear(); //清除航点任务列表，避免重复上传
        File.getInstance().deleteLatLng("position2");//清除规划后的文件，避免重复写入

        List<LatLng> latLngs = readFile(path);
        List<LatLng> sortlatLngs = new ArrayList<>();
        int index = latLngs.size() - 1;   // 初始下标最后一个点
        int count = 1;

        while (!latLngs.isEmpty()) {
            LatLng toBeCompared, cursor, min;
            float distance = Float.MAX_VALUE;   // 设置一个巨大的距离
            min = latLngs.get(index);           // 找出最小距离点
            latLngs.remove(index);              // 将当前的最小距离点从集合中删掉
            toBeCompared = min;                 // 将当前的最小距离点作为被比较的点

            // 地图画点连线
            markAndLink(sortlatLngs, min, "RED", count + "");
            count++;
            // 加入无人机航点列表
            join2(min);
            // 写入文件
            File.getInstance().writeLatLng(min, "position2");

            // 寻找最小距离点在当前集合中的下标
            for (int j = 0; j < latLngs.size(); ++j) {
                cursor = latLngs.get(j);
                float temp = Math.abs(AMapUtils.calculateLineDistance(toBeCompared, cursor));
                if (distance > temp) {
                    distance = temp;
                    index = j;
                }
            }
        }

    }

    private List<LatLng> readFile(String path) {
        //读取文件中的字符串
        ArrayList<String> arrayList = new ArrayList<>();

        File.getInstance().readLatLng(arrayList, path);

        // 对ArrayList中存储的字符串进行处理，生成航点文件
        int len = arrayList.size();
        List<LatLng> latLngs = new ArrayList<LatLng>();
        LatLng fod;
        for (int i = 0; i < len; i++) {
            String s = arrayList.get(i);
            String[] tokens = s.split(",");
            double[] data = new double[tokens.length];
            for (int j = 0; j < data.length; j++)
                data[j] = Double.parseDouble(tokens[j]);
            fod = new LatLng(data[0], data[1]);
            latLngs.add(fod);
        }
        return latLngs;
    }



}