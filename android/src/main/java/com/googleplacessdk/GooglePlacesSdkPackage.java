package com.googleplacessdk;

import androidx.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.googleplacessdk.BuildConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GooglePlacesSdkPackage implements ReactPackage {
    // The main package class that will be used by React Native
    
    @NonNull
    @Override
    public List<NativeModule> createNativeModules(@NonNull ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        
        try {
            if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
                // For New Architecture, TurboModules are registered differently
                // The actual module will be registered via the TurboReactPackage mechanism
                // But we still add our module here for compatibility
                modules.add(new GooglePlacesSdkModule(reactContext));
            } else {
                modules.add(new GooglePlacesSdkModule(reactContext));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return modules;
    }

    @NonNull
    @Override
    public List<ViewManager> createViewManagers(@NonNull ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}
