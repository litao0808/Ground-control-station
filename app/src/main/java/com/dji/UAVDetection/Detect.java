package com.dji.UAVDetection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;

import java.util.List;

public class Detect {
    private final        Handler       myHandler            = new Handler(Looper.getMainLooper());
    //Tensorflow Model Detection Params
    private static final int           TF_OD_API_INPUT_SIZE = 640;
    private              int           count                = 0;
    private              int           count2               = 0;
    private              LatLng        beCompared;
    private              LatLng        beCompared2;
    private              Context       myContext;
    private              SurfaceHolder surfaceHolder;

    public Detect(Context context, SurfaceView surfaceview) {
        myContext = context;
        this.surfaceHolder = surfaceview.getHolder();
    }

    public void handleResult(@NonNull List<Classifier.Recognition> results, int width, int height,double dronelat, double dronelng) {
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);
        paint.setAlpha(255);

        final Paint paint2 = new Paint();
        paint2.setColor(Color.BLUE);
        paint2.setStrokeWidth(2.0f);
        paint2.setAlpha(255);
        paint2.setTextSize(40);

        Canvas canvas;
        canvas = surfaceHolder.lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //清除掉上一次的画框。

            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                location.left = (location.left / TF_OD_API_INPUT_SIZE) * width;
                location.right = (location.right / TF_OD_API_INPUT_SIZE) * width;
                location.top = (location.top / TF_OD_API_INPUT_SIZE) * height;
                location.bottom = (location.bottom / TF_OD_API_INPUT_SIZE) * height;

                if (location != null && result.getConfidence() >= Classifier.MINIMUM_CONFIDENCE_TF_OD_API) {
                    canvas.drawRect(location, paint);
                    canvas.drawText(result.toString(), location.left, location.top, paint2);
                    // FOD目标位置记录
                    switch (count) {
                        case 0: // 记录，记录目标信息
                            beCompared = new LatLng(dronelat, dronelng);
                            File.getInstance().writeLatLng(beCompared, "position");
                            count = 1;
                            break;
                        case 1: // 不记录，如果：当前位置-上一位置>阈值距离，才回到检测状态
                            LatLng cursor = new LatLng(dronelat, dronelng);
                            float temp = Math.abs(AMapUtils.calculateLineDistance(beCompared, cursor));
                            float threshold = 5f;
                            if (temp > threshold)
                                count = 0;
                            break;
                        default:
                            break;
                    }
                }
            }
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    public void handleResult2(@NonNull List<Classifier.Recognition> results, int width, int height,double dronelat,double dronelng,float speedX, float speedY) {
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);
        paint.setAlpha(255);

        final Paint paint2 = new Paint();
        paint2.setColor(Color.BLUE);
        paint2.setStrokeWidth(2.0f);
        paint2.setAlpha(255);
        paint2.setTextSize(40);

        Canvas canvas;
        canvas = surfaceHolder.lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //清除掉上一次的画框。

            for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                location.left = (location.left / TF_OD_API_INPUT_SIZE) * width;
                location.right = (location.right / TF_OD_API_INPUT_SIZE) * width;
                location.top = (location.top / TF_OD_API_INPUT_SIZE) * height;
                location.bottom = (location.bottom / TF_OD_API_INPUT_SIZE) * height;

                if (location != null && result.getConfidence() >= Classifier.MINIMUM_CONFIDENCE_TF_OD_API) {
                    canvas.drawRect(location, paint);
                    canvas.drawText(result.toString(), location.left, location.top, paint2);
                    // FOD目标位置记录
                    switch (count2) {
                        case 0: // 记录，记录目标信息
                            beCompared2 = new LatLng(dronelat, dronelng);
                            if(java.lang.Math.abs(speedX)+java.lang.Math.abs(speedY) < 3f) {
                                File.getInstance().writeLatLng(beCompared2, "result");
                                count2 = 1;
                            }
                            break;
                        case 1: // 不记录，如果：当前位置-上一位置>阈值距离，才回到检测状态
                            LatLng cursor = new LatLng(dronelat, dronelng);
                            float temp = Math.abs(AMapUtils.calculateLineDistance(beCompared2, cursor));
                            float threshold = 5.5f;
                            if (temp > threshold)
                                count2 = 0;
                            break;
                        default:
                            break;
                    }
                }
            }
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    public void cleanView() {
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas();
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        } catch (Exception e) {
        } finally {
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

}