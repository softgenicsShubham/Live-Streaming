package com.shubhamghanghotia.oprngl_image_rendering;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    private Bitmap imgBitmap;
    private MyGlSurfaceView myGlSurfaceView;
    private static final int WAIT_TIME_MS = 4000; // 3 seconds as an example, modify as needed
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadImage();
        waitForBitmapAndInitSurfaceView();
    }

    private void waitForBitmapAndInitSurfaceView() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (imgBitmap != null) {
                    myGlSurfaceView = new MyGlSurfaceView(MainActivity.this, imgBitmap);
                    setContentView(myGlSurfaceView);
                } else {
                    // Bitmap not loaded yet, re-check after a delay
                    waitForBitmapAndInitSurfaceView();
                }
            }
        }, WAIT_TIME_MS);
    }

    private void loadImage() {
        String imgUri = "https://media.licdn.com/dms/image/D4D03AQFe9vksJNnGiA/profile-displayphoto-shrink_800_800/0/1683797624137?e=2147483647&v=beta&t=o01N6YYbG328pniVMknkFB9zyrfj0fpnsAPBU1cgYfk";
        ImageLoader imageLoader = new ImageLoader(new ImageLoader.OnImageLoadedListener() {
            @Override
            public void onImageLoaded(Bitmap bitmap) {
                imgBitmap = bitmap;
            }
        });
        imageLoader.execute(imgUri);
    }
}
