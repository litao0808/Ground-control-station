package com.dji.UAVDetection;


import androidx.annotation.Nullable;

import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;

public final class Tools {
    public static BaseProduct baseProduct;

    private Tools() {
    }


    public static synchronized BaseProduct getProductInstance() {
        if (baseProduct == null) {
            baseProduct = DJISDKManager.getInstance().getProduct();
        }
        return baseProduct;
    }

    @Nullable
    public static synchronized Camera getCameraInstance() {
        if (getProductInstance() == null) return null;
        Camera camera = null;
        if (getProductInstance() instanceof Aircraft) {
            camera = getProductInstance().getCamera();
        } else if (getProductInstance() instanceof HandHeld) {
            camera = getProductInstance().getCamera();
        }
        return camera;
    }

}