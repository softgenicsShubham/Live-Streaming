package com.shubhamghanghotia.video_effect_final_things;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;
import android.Manifest;
import android.widget.FrameLayout;

import cn.nodemedia.NodePublisher;

public class MainActivity extends AppCompatActivity {

    private NodePublisher nodePublisher;

    private FrameLayout frameLayout;

    private GrayscaleEffect grayscaleEffect;



    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_PERMISSION_CODE = 0XFF00;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission();
        setContentView(R.layout.activity_main);
        nodePublisher = new NodePublisher(this, "");
        frameLayout = findViewById(R.id.video_view);
        grayscaleEffect = new GrayscaleEffect();
        nodePublisher.setAudioCodecParam(NodePublisher.NMC_CODEC_ID_AAC, NodePublisher.NMC_PROFILE_AUTO, 48000, 1, 64_000);
        nodePublisher.setVideoOrientation(NodePublisher.VIDEO_ORIENTATION_PORTRAIT);
        nodePublisher.setVideoCodecParam(NodePublisher.NMC_CODEC_ID_H264, NodePublisher.NMC_PROFILE_AUTO, 480, 854, 30, 1_000_000);
        nodePublisher.attachView(frameLayout);
        nodePublisher.setOnNodePublisherEffectorListener(grayscaleEffect);
        nodePublisher.openCamera(false);

    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_PERMISSION_CODE);
    }
}