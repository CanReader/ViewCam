package com.mobile.viewcam.old;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Size;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

public class CameraManager
{
    public boolean isRunning;

    private int cameraFace;


    private final Activity activity;


    private final ListenableFuture<ProcessCameraProvider> providerFeature;
    ProcessCameraProvider provider;

    private CameraSelector selector;
    private CameraInfo cameraInfo;
    private LifecycleCameraController cameraController;
    private Camera camera;

    private ImageAnalysis analysis;
    private ImageAnalysis.Analyzer analyzer;

    private PreviewView view;
    private Preview preview;

    /*
    *         ================================= CONSTRUCTORS =================================
    * */
    public CameraManager(Activity activity)
    {
        isRunning = false;
        providerFeature = ProcessCameraProvider.getInstance(activity.getApplicationContext());
        this.activity = activity;
        preview = new Preview.Builder().build();
    }

    public CameraManager(Activity activity, PreviewView view)
    {
        isRunning = false;
        providerFeature = ProcessCameraProvider.getInstance(activity.getApplicationContext());
        this.activity = activity;
        this.view = view;
        preview = new Preview.Builder().build();
        cameraController = new LifecycleCameraController(activity.getApplicationContext());

    }

    /*
     *         ================================= METHODS =================================
     * */

    public void Start()
    {
        providerFeature.addListener(new Runnable(){
            @Override
            public void run() {
                bindCamera();
            }
        }, ContextCompat.getMainExecutor(activity.getApplicationContext()));
    }

    public void bindCamera()
    {
      bindCamera(CameraSelector.LENS_FACING_BACK);
    }

    public void bindCamera(int Facing)
    {
        try {
            if(provider == null)
                provider = providerFeature.get();
            else
                provider.unbindAll();

            selector = new CameraSelector.Builder().requireLensFacing(cameraFace).build();

            preview.setSurfaceProvider(view.getSurfaceProvider());

            SetupImageAnalysis();

            camera = provider.bindToLifecycle((LifecycleOwner)activity,selector,analysis,preview);

            cameraInfo =  camera.getCameraInfo();

        } catch (Exception e) {
            MessageBox.showErrorBox(activity.getApplicationContext(),e.getMessage());
            e.printStackTrace();
        }
    }

    public void ToggleFace()
    {
        if(cameraFace == CameraSelector.LENS_FACING_BACK)
            cameraFace = CameraSelector.LENS_FACING_FRONT;
        else
            cameraFace = CameraSelector.LENS_FACING_BACK;

        bindCamera(cameraFace);
    }

    public void unbindCamera()
    {
        if(provider != null)
            provider.unbindAll();
    }

    public void BindAnalyzer(ImageAnalysis.Analyzer analyzer)
    {
        this.analyzer = analyzer;
    }

    @SuppressLint("NewApi")
    private void SetupImageAnalysis()
    {
        analysis = new ImageAnalysis.Builder().setTargetResolution(new Size(100,200)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        if(analyzer == null)
            throw new RuntimeException("Analyzer argument cannot be null!");

        analysis.setAnalyzer(activity.getMainExecutor(), analyzer);
    }


    public void setPreviewView(PreviewView view)
    {
        this.view = view;
    }

    public void getCurrentFrame()
    {

    }
}
