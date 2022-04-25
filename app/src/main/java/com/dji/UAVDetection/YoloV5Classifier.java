/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.dji.UAVDetection;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dji.UAVDetection.Utils;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;


public class YoloV5Classifier implements Classifier {

    // Float model
    private final        float   IMAGE_MEAN  = 0;
    private final        float   IMAGE_STD   = 255.0f;
    //config yolo
    private              int     INPUT_SIZE  = -1;
    private              int     output_box;
    // Number of threads in the java app
    private static final int     NUM_THREADS = 1;
    private static final boolean isGPU       = false;
    private              boolean isModelQuantized;
    // holds a gpu delegate
    GpuDelegate gpuDelegate = null;
    // The loaded TensorFlow Lite model.
    private       MappedByteBuffer    tfliteModel;
    // Options for configuring the Interpreter.
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    // Pre-allocated buffers.
    private final Vector<String>      labels        = new Vector<String>();
    private       int[]               intValues;
    private       ByteBuffer          imgData;
    private       ByteBuffer          outData;
    private       Interpreter         tfLiteInterpreter;
    private       float               inp_scale;
    private       int                 inp_zero_point;
    private       float               oup_scale;
    private       int                 oup_zero_point;
    private       int                 numClass;
    protected     float               mNmsThresh    = 0.6f;

    private YoloV5Classifier() {
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager  The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param isQuantized   Boolean representing model is quantized or not
     */
    @NonNull
    public static YoloV5Classifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final String labelFilename,
            final boolean isQuantized,
            final int inputSize) throws IOException {

        final YoloV5Classifier d = new YoloV5Classifier();

        // 读取类别
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        InputStream labelsInput = assetManager.open(actualFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            d.labels.add(line);
        }
        br.close();

        // 初始化Interpreter解释器
        try {
            Interpreter.Options options = (new Interpreter.Options());
            options.setNumThreads(NUM_THREADS);
            if (isGPU) {
                GpuDelegate.Options gpu_options = new GpuDelegate.Options();
                gpu_options.setPrecisionLossAllowed(true); // It seems that the default is true
                gpu_options.setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED);
                d.gpuDelegate = new GpuDelegate(gpu_options);
                options.addDelegate(d.gpuDelegate);
            }
            d.tfliteModel = Utils.loadModelFile(assetManager, modelFilename);
            d.tfLiteInterpreter = new Interpreter(d.tfliteModel, options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 初始化数值
        d.isModelQuantized = isQuantized;
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.INPUT_SIZE = inputSize;
        d.intValues = new int[d.INPUT_SIZE * d.INPUT_SIZE];
        d.output_box = (int) ((Math.pow((inputSize / 32), 2) + Math.pow((inputSize / 16), 2) + Math.pow((inputSize / 8), 2)) * 3);
        int[] shape = d.tfLiteInterpreter.getOutputTensor(0).shape();
        int numClass = shape[shape.length - 1] - 5;
        d.numClass = numClass;

        // 预分配缓冲区
        if (d.isModelQuantized) {
            Tensor inpten = d.tfLiteInterpreter.getInputTensor(0);
            d.inp_scale = inpten.quantizationParams().getScale();
            d.inp_zero_point = inpten.quantizationParams().getZeroPoint();
            Tensor oupten = d.tfLiteInterpreter.getOutputTensor(0);
            d.oup_scale = oupten.quantizationParams().getScale();
            d.oup_zero_point = oupten.quantizationParams().getZeroPoint();
        }
        d.imgData = ByteBuffer.allocateDirect(1 * d.INPUT_SIZE * d.INPUT_SIZE * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.outData = ByteBuffer.allocateDirect(d.output_box * (numClass + 5) * numBytesPerChannel);
        d.outData.order(ByteOrder.nativeOrder());

        return d;
    }


    /**
     * Writes Image data into a {@code ByteBuffer}.
     */
    protected ByteBuffer convertBitmapToByteBuffer(@NonNull Bitmap bitmap) {
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        //将bitmap数据转化为Bytebuffer
        imgData.rewind();
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                int pixelValue = intValues[i * INPUT_SIZE + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point));
                    imgData.put((byte) ((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point));
                    imgData.put((byte) (((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD / inp_scale + inp_zero_point));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }

        return imgData;
    }

    @Override
    public ArrayList<Recognition> recognizeImage(Bitmap bitmap) {
        // 输入
        ByteBuffer byteBuffer_ = convertBitmapToByteBuffer(bitmap);
        Object[] inputArray = {byteBuffer_};
        // 输出
        Map<Integer, Object> outputMap = new HashMap<>();
        outData.rewind();
        outputMap.put(0, outData);
        Log.d("YoloV5Classifier", "mObjThresh: " + Classifier.MINIMUM_CONFIDENCE_TF_OD_API);
        // TensorFlow
        tfLiteInterpreter.runForMultipleInputsOutputs(inputArray, outputMap);

        //对Tensorflow的推理结果进行处理
        float[][][] out = new float[1][output_box][numClass + 5];
        Log.d("YoloV5Classifier", "out[0] detect start");
        ByteBuffer byteBuffer = (ByteBuffer) outputMap.get(0);
        byteBuffer.rewind();
        for (int i = 0; i < output_box; ++i) {
            for (int j = 0; j < numClass + 5; ++j)
                if (isModelQuantized)
                    out[0][i][j] = oup_scale * (((int) byteBuffer.get() & 0xFF) - oup_zero_point);
                else
                    out[0][i][j] = byteBuffer.getFloat();
            // 去除归一化，得到实际：xPos，yPos，weight，height
            for (int j = 0; j < 4; ++j)
                out[0][i][j] *= INPUT_SIZE;
        }

        // 保存检测结果
        ArrayList<Recognition> detections = new ArrayList<Recognition>();
        for (int i = 0; i < output_box; ++i) {
            final int offset = 0;

            // 求最大类别数
            int detectedClass = -1;
            float maxClass = 0;
            final float[] classes = new float[labels.size()];
            for (int c = 0; c < labels.size(); ++c) {   // 记录类别
                classes[c] = out[0][i][5 + c];
            }
            for (int c = 0; c < labels.size(); ++c) {   // 记录类别的最大值
                if (classes[c] > maxClass) {
                    detectedClass = c;
                    maxClass = classes[c];
                }
            }

            // 求最大类的置信度
            final float confidence = out[0][i][4];
            final float confidenceInClass = maxClass * confidence;

            // 置信度大于阈值，则保存检测结果
            if (confidenceInClass > Classifier.MINIMUM_CONFIDENCE_TF_OD_API) {
                final float xPos = out[0][i][0];
                final float yPos = out[0][i][1];
                final float w = out[0][i][2];
                final float h = out[0][i][3];
                Log.d("YoloV5Classifier", Float.toString(xPos) + ',' + yPos + ',' + w + ',' + h);
                final RectF rect = new RectF(
                        Math.max(0, xPos - w / 2),
                        Math.max(0, yPos - h / 2),
                        Math.min(bitmap.getWidth() - 1, xPos + w / 2),
                        Math.min(bitmap.getHeight() - 1, yPos + h / 2));
                detections.add(new Recognition("" + offset, labels.get(detectedClass), confidenceInClass, rect, detectedClass));
            }
        }
        Log.d("YoloV5Classifier", "detect end");

        final ArrayList<Recognition> recognitions = nms(detections);
        return recognitions;
    }

    @Override
    public void close() {
        tfLiteInterpreter.close();
        tfLiteInterpreter = null;
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        tfliteModel = null;
    }

    public void setNumThreads(int num_threads) {
        tfliteOptions.setNumThreads(num_threads);
    }

    private void recreateInterpreter() {
        if (tfLiteInterpreter != null) {
            tfLiteInterpreter.close();
            tfLiteInterpreter = new Interpreter(tfliteModel, tfliteOptions);
        }
    }

    public void useGpu() {
        if (gpuDelegate == null) {
            gpuDelegate = new GpuDelegate();
            tfliteOptions.addDelegate(gpuDelegate);
            recreateInterpreter();
        }
    }

    public void useCPU() {
        recreateInterpreter();
    }

    // 非最大抑制
    protected ArrayList<Recognition> nms(ArrayList<Recognition> list) {
        ArrayList<Recognition> nmsList = new ArrayList<Recognition>();

        for (int k = 0; k < labels.size(); k++) {
            // 1.找到每个类别的最大置信度
            PriorityQueue<Recognition> pq = new PriorityQueue<>(50, (lhs, rhs) -> {
                // Intentionally reversed to put high confidence at the head of the queue.
                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
            });

            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).getDetectedClass() == k) {
                    pq.add(list.get(i));
                }
            }

            // 2.做非最大抑制
            while (pq.size() > 0) {
                //insert detection with max confidence
                Recognition[] a = new Recognition[pq.size()];
                Recognition[] detections = pq.toArray(a);
                Recognition max = detections[0];
                nmsList.add(max);
                pq.clear();
                for (int j = 1; j < detections.length; j++) {
                    Recognition detection = detections[j];
                    RectF b = detection.getLocation();
                    if (box_iou(max.getLocation(), b) < mNmsThresh) {
                        pq.add(detection);
                    }
                }
            }
        }
        return nmsList;
    }

    protected float box_iou(RectF a, RectF b) {
        return box_intersection(a, b) / box_union(a, b);
    }

    protected float box_intersection(@NonNull RectF a, @NonNull RectF b) {
        float w = overlap((a.left + a.right) / 2, a.right - a.left,
                (b.left + b.right) / 2, b.right - b.left);
        float h = overlap((a.top + a.bottom) / 2, a.bottom - a.top,
                (b.top + b.bottom) / 2, b.bottom - b.top);
        if (w < 0 || h < 0) return 0;
        float area = w * h;
        return area;
    }

    protected float box_union(RectF a, RectF b) {
        float i = box_intersection(a, b);
        float u = (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - i;
        return u;
    }

    protected float overlap(float x1, float w1, float x2, float w2) {
        float l1 = x1 - w1 / 2;
        float l2 = x2 - w2 / 2;
        float left = l1 > l2 ? l1 : l2;
        float r1 = x1 + w1 / 2;
        float r2 = x2 + w2 / 2;
        float right = r1 < r2 ? r1 : r2;
        return right - left;
    }
}
