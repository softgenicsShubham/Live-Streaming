package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.Manifest;

import cn.nodemedia.NodePublisher;


public class MainActivity extends AppCompatActivity {


    static {
        System.loadLibrary("opencv_java4");
    }


    private OpenCVNodePublisherWrapper np;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_PERMISSION_CODE = 0XFF00;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        FrameLayout fl = findViewById(R.id.video_view);
        requestPermission();
        np = new OpenCVNodePublisherWrapper(this, "");
        np.setAudioCodecParam(NodePublisher.NMC_CODEC_ID_AAC, NodePublisher.NMC_PROFILE_AUTO, 48000, 1, 64_000);
        np.setVideoOrientation(NodePublisher.VIDEO_ORIENTATION_PORTRAIT);
        np.setVideoCodecParam(NodePublisher.NMC_CODEC_ID_H264, NodePublisher.NMC_PROFILE_AUTO, 480, 854, 30, 1_000_000);
        np.attachView(fl);


        np.openCamera(true);
        Button publishBtn = findViewById(R.id.button);
        publishBtn.setText("Start Publishing");
        Button switchBtn = findViewById(R.id.button2);
        switchBtn.setText("Switch b/w camera");
        switchBtn.setOnClickListener((v) -> {
            np.switchCamera();
        });
        publishBtn.setOnClickListener((v) -> {
            np.startPublishing("rtmp://192.168.1.33/live/demo");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        np.onDestroy();

    }





    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_PERMISSION_CODE);
    }

}