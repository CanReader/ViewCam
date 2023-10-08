package com.mobile.viewcam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements LifecycleOwner
{
    private static String TAG = "MainActivity";

    CameraManager camMan;
    NetworkManager netMan;

    private TextureView camView;

    private TextView adressArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check internet & wifi connection
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 101);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 102);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 103);

        // Check Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 104);

        // Check if the device has a camera
        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
            return;

        camView = findViewById(R.id.camView); // Initialize camView here
        adressArea = findViewById(R.id.IPArea);

        // Create an instance of CameraManager and pass the TextureView
        camMan = new CameraManager(this, camView);
        camView.setSurfaceTextureListener(camMan.getTextureListener());

        netMan = new NetworkManager(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart()
    {
        super.onStart();

        String localIP = netMan.getLocalIP();

        adressArea.setText("IP Address: " + localIP);

    }

    @Override
    protected void onResume() {
        super.onResume();
        camMan.startBackgroundThread();
        camMan.setImageSocket(netMan.getImageSocket());
        if (!camView.isAvailable()) {
            camView.setSurfaceTextureListener(camMan.getTextureListener());
        } else {
            camMan.setupCamera(camView.getWidth(),camView.getHeight());
        }
    }

    @Override
    protected void onPause() {
        camMan.stopBackgroundThread();
        camMan.closeCamera();

        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 103) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission allowed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "For using the application, you should allow camera usage!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);

        View bottom = getWindow().getDecorView();

        if(hasFocus)
        {
            bottom.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
        }
    }

    public void FlipBtn_click(View view) {
    }
}