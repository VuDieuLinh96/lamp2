package com.lynkmyu.lamp2;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * Lamp2Plugin
 */
public class Lamp2Plugin implements MethodCallHandler {

    private Boolean hasFlashlight;
    private Registrar _registrar;

    /**
     * using with camera1. sdk < 23
     */
    private Camera _camera;

    /**
     * using with camera2. sdk >= 23
     */
    private CameraManager cameraManager;
    private String cameraId;

    /**
     * initialization
     */
    private Lamp2Plugin(Registrar registrar) {
        _registrar = registrar;
        hasFlashlight = false;

        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            Activity activity = _registrar.activity();
            cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            try {
                for (String id : cameraManager.getCameraIdList()) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                    if (facing != null && facing.equals(CameraCharacteristics.LENS_FACING_BACK)) {
                        cameraId = id;
                        hasFlashlight = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    }
                }
            } catch (NullPointerException e) {
                // ignore it
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            this._camera = this.getCamera();
            hasFlashlight = _registrar.context().getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        }
    }

    /**
     * get and store camera when api level < 23.
     */
    private Camera getCamera() {
        try {
            return Camera.open();
        } catch (Exception e) {
            System.out.println("Failed to get camera : " + e.toString());
            return null;
        }
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flashlight");
        channel.setMethodCallHandler(new Lamp2Plugin(registrar));
    }

    /**
     * Entrance for invokeMethod()
     */
    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("hasFlashlight")) {
            result.success(hasFlashlight);
        } else if (call.method.equals("lightOn")) {
            turnLight(result, true);
        } else if (call.method.equals("lightOff")) {
            turnLight(result, false);
        } else {
            result.notImplemented();
        }
    }

    /**
     * set the flash light on/off
     */
    private void turnLight(Result result, boolean on) {
        if (!hasFlashlight) {
            return;
        } else if (VERSION.SDK_INT >= VERSION_CODES.M) {
            try {
                cameraManager.setTorchMode(cameraId, on);
                result.success(null);
            } catch (CameraAccessException e) {
                result.error("TORCH_ERROR", e.toString(), e);
            }
        } else {
            if (_camera == null) {
                return;
            }

            /**
             * According to https://developer.android.com/reference/android/hardware/Camera.Parameters
             * the Camera.Parameters and its' setFlashMode() are deprecated in API level 21.
             * However, Camera2 was added at API level 23.
             * Therefore we still use Camera.Parameters if it's API level 21/22.
             * If you have a better solution, please submit an issue at
             * github: https://github.com/RyanYuanHu/flashlight
             * Thanks!
             */
            Camera.Parameters params = _camera.getParameters();
            params.setFlashMode(on ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            _camera.setParameters(params);
            _camera.startPreview();
        }
    }
}
