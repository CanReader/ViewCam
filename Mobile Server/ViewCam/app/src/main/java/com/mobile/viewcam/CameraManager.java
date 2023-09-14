package com.mobile.viewcam;

import static android.content.Context.CAMERA_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraManager {
    private static final String TAG = "CameraManager";

    private String camID;

    private final Context context;
    private final TextureView textureView;
    private Size previewSize;

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private android.hardware.camera2.CameraManager cameraManager;


    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private ImageSocket imgSocket;


    ImageReader imgReader;

    public CameraManager(Context context, TextureView textureView) {
        this.context = context;
        this.textureView = textureView;
    }

    @SuppressLint("MissingPermission")
    /*
    * Setup camera and set orientations
    * */
    public boolean setupCamera(int width,int height) {
        cameraManager = (android.hardware.camera2.CameraManager) context.getSystemService(CAMERA_SERVICE);

        imgReader = ImageReader.newInstance(width,height, ImageFormat.JPEG,1);
        imgReader.setOnImageAvailableListener(ImageReaderListener,backgroundHandler);

        try {
            String[] ids = cameraManager.getCameraIdList();

            camID = ids[0];

            CameraCharacteristics characteristics = getCameraCharacteristics(camID);

            StreamConfigurationMap map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            int deviceOrientation =
                    ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
            int totalRotation = sensorToDeviceRotation(characteristics,deviceOrientation);

            boolean swapRotation = totalRotation == 90 || totalRotation == 270;

            int RotationW = width;
            int RotationH = height;

            if(swapRotation)
            {
                RotationW = height;
                RotationH = width;
            }

            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),RotationW,
                    RotationH);

        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }



    @SuppressLint("MissingPermission")
    private void connectCamera()
    {
        if(cameraManager == null) return;

        try
        {
            cameraManager.openCamera(camID,stateCallback,backgroundHandler);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void createCameraPreview() {
        if(cameraDevice == null)
            return;

        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);

            CameraCharacteristics cameraCharacteristics = getCameraCharacteristics(cameraManager.getCameraIdList()[1]);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            int deviceRotation =
                    ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
            int jpegOrientation = (sensorOrientation + deviceRotation * 90 + 360) % 360;

            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,jpegOrientation);
            captureRequestBuilder.addTarget(imgReader.getSurface());
            captureRequestBuilder.addTarget(surface);


            cameraDevice.createCaptureSession(Arrays.asList(surface,imgReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (cameraDevice == null)
                        return;
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.d("Camera2App", "onConfigureFailed");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void updatePreview() {
        if (cameraDevice == null)
            return;
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    public void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    public void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private int sensorToDeviceRotation(CameraCharacteristics characteristic, int Orientation)
    {
        int sensorOri = characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return (sensorOri + Orientation + 360)%360;
    }

    private Size chooseOptimalSize(Size[] outputSizes, int width, int height)
    {
        List<Size> bigEnough = new ArrayList<>();

        for(Size i : outputSizes)
        {
            if(i.getHeight() == i.getWidth() * height/width && i.getWidth() >= width && i.getHeight() >= height)
                bigEnough.add(i);
        }

        if(bigEnough.size() > 0)
            return Collections.min(bigEnough,new CompareSizeByArea());
        else
            return outputSizes[0];
    }

    /*
    *
    * SETTER METHODS!
    *
    * */

    public void setImageSocket(ImageSocket imageSocket)
    {
        imgSocket = imageSocket;
    }

    /*
    *
    * GETTER METHODS!
    *
    * */

    public TextureView.SurfaceTextureListener getTextureListener() {return textureListener;}

    private CameraCharacteristics getCameraCharacteristics(String id) throws CameraAccessException {
        return cameraManager.getCameraCharacteristics(id);
    }

    /*

    Callbacks

    */

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
        {
            if(setupCamera(width, height))
            {
                connectCamera();
            }
            else
                throw new IllegalStateException("Failed to setup camera!");
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
        {
            Log.d(TAG, "Oops, surface size has been changed!!!");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.e("Tag","The surface has been destroyed!");
            closeCamera();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            if(cameraDevice == null)
                cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private final ImageReader.OnImageAvailableListener ImageReaderListener =
            new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader)
        {
            Image image = imageReader.acquireLatestImage();

            if(image != null)
            {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                //So we filled bytes array, now we are going to send these bytes via socket, for
                // this we are going to use a callback method from NetworkManager!
                if(imgSocket != null)
                    imgSocket.Run(bytes);


            }

            image.close();
        }
    };

    /*
    *
    *
    * CLASSES!
    *
    *
    * */

    public interface ImageSocket
    {
        void Run(byte[] data);
    }

    private static class CompareSizeByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size s1, Size s2) {
            return Long.signum(((long) s1.getWidth() *s1.getWidth())/((long) s2.getWidth() *s2.getWidth()));
        }
    }
}
