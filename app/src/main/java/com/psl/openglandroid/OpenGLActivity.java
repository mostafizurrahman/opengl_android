package com.psl.openglandroid;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.RecommendedStreamConfigurationMap;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OpenGLActivity extends AppCompatActivity {

    private final static String DEBUG_TAG = "MakePhotoActivity";
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    boolean canOpenCamera = false;
    DefaultCameraRenderer defaultCameraRenderer;
    private Camera camera;
    private int cameraId = 0;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private Surface surface;
    private TextureView textureView;
    private SurfaceTexture surfaceTexture;
    private HandlerThread backgroundThread = new HandlerThread("bg");
    private TextureViewGLWrapper textureViewGLWrapper;
    private Handler backgroundHandler;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_open_gl);
        // do we have a camera?
        textureView = findViewById(R.id.texture_view);


        defaultCameraRenderer = new DefaultCameraRenderer(this);
        textureViewGLWrapper = new TextureViewGLWrapper(defaultCameraRenderer);
        textureViewGLWrapper.setListener(new TextureViewGLWrapper.EGLSurfaceTextureListener() {
            @Override
            public void onSurfaceTextureReady(SurfaceTexture surfaceTxt) {
                surfaceTexture = surfaceTxt;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            openCamera();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    requestingCamera();
                }
            }
        }, new Handler(Looper.getMainLooper()));
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                textureViewGLWrapper.onSurfaceTextureAvailable(surface, width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                textureViewGLWrapper.onSurfaceTextureSizeChanged(surface, width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                textureViewGLWrapper.onSurfaceTextureDestroyed(surface);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                textureViewGLWrapper.onSurfaceTextureUpdated(surface);
            }
        });
        try {
            getCameraPermission();
        } catch (Exception e) {

        }
    }


    void requestingCamera(){
        try {
            canOpenCamera = true;
            openCamera();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void getCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                requestingCamera();
            }
            else if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
            } else {
                requestingCamera();
            }
        } else {
            requestingCamera();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            canOpenCamera = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (canOpenCamera) {
                try {
                    openCamera();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    void openCamera() throws CameraAccessException {
        if (!canOpenCamera) return;
        if (surfaceTexture == null) return;
        if (cameraDevice != null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            for (String cameraId : cameraManager.getCameraIdList()) {
                final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    Activity#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for Activity#requestPermissions for more details.
                            return;
                        }
                    }
                    cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {

                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {

                            cameraDevice = camera;
                            surface = new Surface(surfaceTexture);
                            StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);


                            Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
                            for(Size size : sizes){
                                if(size.getWidth() <= 3000 && size.getWidth() >= 1080){
                                    surfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                                    defaultCameraRenderer.updateViewPort(size.getWidth(), size.getHeight());
                                    break;
                                }
                            }
                            try {
                                final CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                List<Surface> surfaces = new ArrayList<Surface>();
                                surfaces.add(surface);
                                builder.addTarget(surface);
                                camera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(@NonNull CameraCaptureSession session) {


//                                        builder.set(CaptureRequest.CONTROL_AF_MODE,  CameraMetadata.CONTROL_AF_MODE_OFF);
                                        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP ||
                                                Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1){

                                        }
                                        builder.set(CaptureRequest.CONTROL_AF_MODE,
                                                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//                                        builder.set(CaptureRequest.EDGE_MODE, CameraMetadata.EDGE_MODE_HIGH_QUALITY);
//                                        builder.set(CaptureRequest.SHADING_MODE, CameraMetadata.SHADING_MODE_HIGH_QUALITY);
//                                        builder.set(CaptureRequest.TONEMAP_MODE, CameraMetadata.TONEMAP_MODE_HIGH_QUALITY);
//                                        builder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
//                                        builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
//                                        builder.set(CaptureRequest.HOT_PIXEL_MODE, CameraMetadata.HOT_PIXEL_MODE_HIGH_QUALITY);
//                                        builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
//                                        builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON);
//                                        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
                                        captureSession = session;
                                        try {
                                            session.setRepeatingRequest(builder.build(), null, null);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                        Log.i("", "onConfigure Failed");
                                    }
                                }, null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onDisconnected(@NonNull CameraDevice camera) {

                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {

                        }
                    }, backgroundHandler);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(captureSession != null){
                captureSession.close();
                captureSession = null;
            }
            if(cameraDevice != null){

                cameraDevice.close();
            }
        }
        cameraDevice = null;
        surfaceTexture = null;
        super.onPause();
    }

}
