package com.dji.UAVDetection;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

public class MApplication extends Application {

    private Init fpvApplication;

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);

        MultiDex.install(MApplication.this);
        com.secneo.sdk.Helper.install(MApplication.this);

        if (fpvApplication == null) {
            fpvApplication = new Init();
            fpvApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fpvApplication.onCreate();
    }
}
