package com.dji.UAVDetection;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.List;

import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;


public class DetectionFragment extends Fragment implements View.OnClickListener {
    public  boolean fodViewFlag = true;
    private View    view;

    //Tensorflow Model Detection Params
    private static final int                          TF_OD_API_INPUT_SIZE   = 640;
    private static final boolean                      TF_OD_API_IS_QUANTIZED = false;
    private static final String                       TF_OD_API_MODEL_FILE   = "v5-s.tflite";   //TODO 修改模型路径
    private static final String                       TF_OD_API_LABELS_FILE  = "file:///android_asset/VOC.txt";  //TODO 修改模型labels路径


    private              YoloV5Classifier             detector               = null;
    private              List<Classifier.Recognition> results                = null;
    public static       double                       dronelat               = 30.751042;
    public static       double                       dronelng               = 103.934626;
    public static       float                        speedX                 = 10f;
    public static       float                        speedY                 = 10f;

    // 视频检测
    private DJICodecManager mCodecManager = null; // 用于视频实时视图的编解码器
    private Bitmap          bitmap        = null;
    private Thread          thread        = null;

    private int             width, height;
    public  static  Button      mReturnBtn;
    private Detect      modelDetect = null;
    public  static  Button      detectButton;
    public  static  Button      detectButton2;
    private boolean     detectFlag  = false;
    private boolean     cleanFlag   = false;
    private boolean     detectFlag2 = false;
    private boolean     cleanFlag2  = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null)
            view = inflater.inflate(R.layout.fragment_detect, container, false);

        //1、初始化照UI
        initUI();

        //2、初始化照相机
        initCamera();

        //3、初始化Tensorflow，启动模型推理子线程
        bitmap = Utils.getBitmapFromAsset(getContext(), "00005_aug_1.jpg");
        initTensorflow();



        // 启动推理子线程
       new Thread(new Runnable() {
            @Override
            public void run() {
                while (true)
                    synchronized (this) {
                        if (detectFlag) {
                            Bitmap cropBitmap;
                            dronelat = AutonomousFlight.droneLocationLat;
                            dronelng = AutonomousFlight.droneLocationLng;
                            cropBitmap = Utils.processBitmap(bitmap, TF_OD_API_INPUT_SIZE);   // 裁剪获取的实时bitmap
                            results = detector.recognizeImage(cropBitmap);
                            modelDetect.handleResult(results, width, height,dronelat, dronelng);
                        } else {
                            if (cleanFlag) {
                                modelDetect.cleanView();
                                cleanFlag = false;
                                showToast("清除");
                            }
                        }

                        if (detectFlag2) {
                            Bitmap cropBitmap;
                            dronelat = AutonomousFlight.droneLocationLat;
                            dronelng = AutonomousFlight.droneLocationLng;
                            speedX = AutonomousFlight.droneSpeedX;
                            speedY = AutonomousFlight.droneSpeedY;
                            cropBitmap = Utils.processBitmap(bitmap, TF_OD_API_INPUT_SIZE);   // 裁剪获取的实时bitmap
                            results = detector.recognizeImage(cropBitmap);
                            modelDetect.handleResult2(results, width, height,dronelat,dronelng,speedX, speedY);
                        } else {
                            if (cleanFlag2) {
                                modelDetect.cleanView();
                                cleanFlag2 = false;
                                showToast("清除");
                            }
                        }
                    }
            }
        }).start();

       //thread.start();


        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_return1: {
                getActivity().finish();
                break;
            }
            case R.id.detectButton1: {
                if (detectFlag) {
                    detectFlag = false;
                    detectButton.setText("一次检测");
                } else {
                    detectFlag = true;
                    cleanFlag = true;
                    detectButton.setText("一次检测中");
                }
                break;
            }
            case R.id.detectButton2: {
                if (detectFlag2) {
                    detectFlag2 = false;
                    detectButton2.setText("二次检测");

                } else {
                    //initTensorflow2();
                    // 启动推理子线程
// 启动推理子线程
                    detectFlag2 = true;
                    cleanFlag2 = true;
                    detectButton2.setText("二次检测中");
                }

                break;
            }

            default:
                break;
        }
    }

    private void initUI() {
        mReturnBtn = view.findViewById(R.id.btn_return1);
        detectButton = view.findViewById(R.id.detectButton1);
        detectButton2 = view.findViewById(R.id.detectButton2);

        mReturnBtn.setOnClickListener(this);
        detectButton.setOnClickListener(this);
        detectButton2.setOnClickListener(this);


        SurfaceView surfaceview = view.findViewById(R.id.surfaceview1);
        surfaceview.setZOrderOnTop(true);//处于顶层
        surfaceview.getHolder().setFormat(PixelFormat.TRANSPARENT);//设置surface为透明
        modelDetect = new Detect(view.getContext(), surfaceview);

        TextureView mVideoSurface = view.findViewById(R.id.video_previewer_surface1);
        if (null != mVideoSurface)
            mVideoSurface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    if (mCodecManager == null) {
                        mCodecManager = new DJICodecManager(getContext(), surface, width, height);
                        DetectionFragment.this.width = width;
                        DetectionFragment.this.height = height;
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    if (mCodecManager != null) {
                        mCodecManager.cleanSurface();
                        mCodecManager = null;
                    }
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    bitmap = mVideoSurface.getBitmap();
                }
            });




        //相机实时取景接收原始H264视频数据的回调
        VideoFeeder.VideoDataListener mReceivedVideoDataListener = (videoBuffer, size) -> {
            if (mCodecManager != null)
                mCodecManager.sendDataToDecoder(videoBuffer, size);
        };

        if (fodViewFlag) setViewGone();
        else setViewVisible();
    }

    private void initCamera() {
        Camera camera = Tools.getCameraInstance();
        if (camera != null) {
            camera.setSystemStateCallback(cameraSystemState -> {
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initTensorflow() {
        // 初始化tensorflow推理模型
        try {
            detector = YoloV5Classifier.create(
                    getContext().getAssets(),
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_IS_QUANTIZED,
                    TF_OD_API_INPUT_SIZE);
            //detector.useCPU();
            detector.useGpu();
        } catch (final IOException e) {
            e.printStackTrace();
            showToast("分类器无法初始化!");
            getActivity().finish();
        }
    }







    private void setViewGone() {
        RelativeLayout relativeLayout = view.findViewById(R.id.uxsdk);
        relativeLayout.setVisibility(View.GONE);
    }

    private void setViewVisible() {
        RelativeLayout relativeLayout = view.findViewById(R.id.uxsdk);
        relativeLayout.setVisibility(View.VISIBLE);
    }

    private void showToast(String s) {
        getActivity().runOnUiThread(() -> Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show());
    }

}