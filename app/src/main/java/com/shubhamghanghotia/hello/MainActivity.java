package com.shubhamghanghotia.hello;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.Manifest;
import android.widget.Toast;

import cn.nodemedia.NodePublisher;

public class MainActivity extends AppCompatActivity {
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_PERMISSION_CODE = 0XFF00;

    FrameLayout frameLayout;

    NodePublisher np;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        frameLayout = findViewById(R.id.video_view);

        np = new NodePublisher(this, "");




        np = new NodePublisher(this, "");
        np.setAudioCodecParam(NodePublisher.NMC_CODEC_ID_AAC, NodePublisher.NMC_PROFILE_AUTO, 48000, 1, 64_000);
        np.setVideoOrientation(NodePublisher.VIDEO_ORIENTATION_PORTRAIT);
        np.setVideoCodecParam(NodePublisher.NMC_CODEC_ID_H264, NodePublisher.NMC_PROFILE_AUTO, 480, 854, 30, 1_000_000);
        np.openCamera(true);
        np.setVideoFrontMirror(false);

        np.setHWAccelEnable(true);
        np.attachView(frameLayout);

        Button button = findViewById(R.id.button1);
        Button button1 = findViewById(R.id.button1);

        boolean isStreaming = false;

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Pressed", "Switch button pressed");

                np.switchCamera();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStreaming) {
                    int result = np.stop();

                    if (result == 0) {
                        button.setText("Start");
                        Toast.makeText(MainActivity.this, "Streaming Started", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to start streaming", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    int result = np.start("rtmp://192.168.1.36/live/new_stream");
                    if (result == 0) {
                        button.setText("Stop");
                        Toast.makeText(MainActivity.this, "Streaming Started", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to start streaming", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });







    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_PERMISSION_CODE);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        np.detachView();
        np.closeCamera();
        np.stop();
    }






}