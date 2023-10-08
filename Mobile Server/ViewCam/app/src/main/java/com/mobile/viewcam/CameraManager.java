package com.mobile.viewcam;

import static android.content.Context.CAMERA_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraManager {

    //region Variables
    private static final String TAG = "CameraManager";

    private String camID;

    private final Context context;
    private final TextureView textureView;
    private Size previewSize;

    private android.hardware.camera2.CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imgReader;


    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private ImageSocket imgSocket;

    //endregion

    //region Constructors
    public CameraManager(Context context, TextureView textureView) {
        this.context = context;
        this.textureView = textureView;
    }
    //endregion

    //region Methods
    @SuppressLint("MissingPermission")
    /*
    * Setup camera and set orientations
    * */
    public boolean setupCamera(int width,int height) {
        cameraManager = (android.hardware.camera2.CameraManager) context.getSystemService(CAMERA_SERVICE);
        ////32 256 34 35 36 37
        imgReader = ImageReader.newInstance(width,height, ImageFormat.JPEG,2);
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


            if (map != null)
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),RotationW,
                        RotationH); //720 1448

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

            if (texture != null)
                texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            Surface surface = new Surface(texture);

            CameraCharacteristics cameraCharacteristics = getCameraCharacteristics(cameraManager.getCameraIdList()[1]);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //noinspection DataFlowIssue
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
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null)
                        return;
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
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
        //noinspection DataFlowIssue
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

    //endregion

    //region Setters
    public void setImageSocket(ImageSocket imageSocket)
    {
        imgSocket = imageSocket;
    }

    //endregion

    //region Getters
    public TextureView.SurfaceTextureListener getTextureListener() {return textureListener;}

    private CameraCharacteristics getCameraCharacteristics(String id) throws CameraAccessException {
        return cameraManager.getCameraCharacteristics(id);
    }
    //endregion

    //region Callback
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height)
        {
            if(setupCamera(width, height))
            {
                connectCamera();
            }
            else
                throw new IllegalStateException("Failed to setup camera!");
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height)
        {
            Log.d(TAG, "Oops, surface size has been changed!!!");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            Log.e("Tag","The surface has been destroyed!");
            closeCamera();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            if(cameraDevice == null)
                cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
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
                byte[] imageArray = ImageUtils.getImageArray(image);

                if(imageArray == null)
                    Log.e(TAG,"Failed to convert image format! The format: " + image.getFormat());

                if(imgSocket != null && imageArray != null)
                    imgSocket.Run(imageArray);
            }

            if(image != null)
                image.close();
        }
    };
    //endregion

    //region Classes
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

    private static class ImageUtils
    {
        public static byte[] getImageArray(Image image)
        {
            byte[] data = null;
            if(image.getFormat() == ImageFormat.JPEG)
            {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                data = new byte[buffer.capacity()];
                buffer.get(data);
            }
            else if(image.getFormat() == ImageFormat.YUV_420_888)
            {
                data = NV21toJpeg(YUV_420_888toNV21(image),image.getWidth(), image.getHeight());
            }

            return data;
        }

        public static byte[] YUV_420_888toNV21(Image image)
        {
            byte[] nv21 = null;
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            ByteBuffer uvBuffer = image.getPlanes()[2].getBuffer();

            nv21 = new byte[yBuffer.remaining() + uvBuffer.remaining()];

            yBuffer.get(nv21, 0, yBuffer.remaining());

            uvBuffer.get(nv21, yBuffer.remaining(), uvBuffer.remaining());

            return nv21;
        }

        public static byte[] NV21toJpeg(byte[] data, int width, int height)
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            YuvImage yuv = new YuvImage(data,ImageFormat.NV21,width,height,null);
            yuv.compressToJpeg(new Rect(0,0,width,height),100,out);
            return out.toByteArray();
        }
    }
    //endregion
}
