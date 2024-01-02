package com.shubhamghanghotia.opengl_testing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.Manifest;

import cn.nodemedia.NodePublisher;

public class MainActivity extends AppCompatActivity {

    private FrameLayout frameLayout;
    private NodePublisher nodePublisher;


    private WorkingEffector workingEffector;

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_PERMISSION_CODE = 0XFF00;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        frameLayout = findViewById(R.id.video_view);
        Button button = findViewById(R.id.my_button);
        workingEffector = new WorkingEffector();
        nodePublisher = new NodePublisher(this, "");
        nodePublisher.setAudioCodecParam(NodePublisher.NMC_CODEC_ID_AAC, NodePublisher.NMC_PROFILE_AUTO, 48000, 1, 64_000);
        nodePublisher.setVideoOrientation(NodePublisher.VIDEO_ORIENTATION_PORTRAIT);
        nodePublisher.setVideoCodecParam(NodePublisher.NMC_CODEC_ID_H264, NodePublisher.NMC_PROFILE_AUTO, 480, 854, 30, 1_000_000);
        nodePublisher.attachView(frameLayout);
        nodePublisher.setOnNodePublisherEffectorListener(workingEffector);
        nodePublisher.openCamera(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nodePublisher.start("rtmp://192.168.1.2/live/hello_viewers");
            }
        });
    }


    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_PERMISSION_CODE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nodePublisher.detachView();
        nodePublisher.closeCamera();
        nodePublisher.stop();
    }


}