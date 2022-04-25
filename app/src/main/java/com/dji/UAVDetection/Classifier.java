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

import android.graphics.Bitmap;
import android.graphics.RectF;

import java.util.List;

/**
 * 用于与不同识别引擎交互的通用接口。
 */
public interface Classifier {
    float MINIMUM_CONFIDENCE_TF_OD_API = 0.3f; //阈值

    List<Recognition> recognizeImage(Bitmap bitmap);

    void close();

    /**
     * 由描述识别内容的分类器返回的不可变结果。
     */
    class Recognition {
        // 已识别内容的唯一标识符。 特定于类，而不是对象的实例。
        private final String id;
        // 识别的显示名称。
        private final String title;
        //相对于其他人的识别程度的可排序分数。 越高越好。
        private final Float  confidence;
        //源图像中已识别对象位置的可选位置。
        private       RectF  location;
        private       int    detectedClass;

        public Recognition(
                final String id,
                final String title,
                final Float confidence,
                final RectF location,
                int detectedClass) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
            this.detectedClass = detectedClass;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public int getDetectedClass() {
            return detectedClass;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (title != null) {
                resultString += title + " ";
            }
            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }
            return resultString.trim();
        }
    }
}
