package com.example.live_streaming;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.Image;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private static final int API_LEVEL_23 = 23;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private PreviewView previewView; // Add this line

    private ExecutorService cameraExecutor; // Add this line

    private CascadeClassifier faceCascade;

    private static final String TAG = "MyImageAnalyzer";
    private OverlayView overlayView;



    static {
        System.loadLibrary("opencv_java4");
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView); // Add this line
        overlayView = findViewById(R.id.overlayView); // Add this line


        if (allPermissionsGranted()) {
            initializeFaceCascade(this); // Initialize face cascade here
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        Button captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(view -> {
            if (isRecording) {

            } else {

            }
        });
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                Log.e("CameraX", "Error binding camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();



        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // enable the following line if RGBA output is needed.
                        //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        imageAnalysis.setTargetRotation(rotation);


        cameraExecutor = Executors.newSingleThreadExecutor();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            @androidx.camera.core.ExperimentalGetImage
            public void analyze(@NonNull ImageProxy imageProxy) {
                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                int imageFormat = imageProxy.getFormat();
                Log.d("OverlayView", "Display Rotation: " + rotationDegrees);


//                Log.d("ImageAnalysis", "Rotation degrees: " + rotationDegrees);

                try {
                    Image image = imageProxy.getImage();

                    if (image != null) {
                        // Get image planes
                        Image.Plane[] planes = image.getPlanes();

                        // Initialize buffers
                        ByteBuffer yBuffer = planes[0].getBuffer();
                        ByteBuffer uBuffer = planes[1].getBuffer();
                        ByteBuffer vBuffer = planes[2].getBuffer();

                        // Calculate image size
                        int ySize = yBuffer.remaining();
                        int uSize = uBuffer.remaining();
                        int vSize = vBuffer.remaining();
                        int imageSize = ySize + uSize + vSize;

                        // Create a byte array to hold the image data
                        byte[] data = new byte[imageSize];

                        // Copy Y, U, and V planes into the byte array
                        yBuffer.get(data, 0, ySize);
                        uBuffer.get(data, ySize, uSize);
                        vBuffer.get(data, ySize + uSize, vSize);

                        // Create a Mat from the byte array
                        Mat matYUV = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
                        matYUV.put(0, 0, data);

                        // Convert YUV to RGBA
                        Mat matRGBA = new Mat();
                        Imgproc.cvtColor(matYUV, matRGBA, Imgproc.COLOR_YUV2RGBA_NV21, 4);

                        // Detect faces using the loaded cascade classifier
                        Mat grayMat = new Mat();
                        Imgproc.cvtColor(matRGBA, grayMat, Imgproc.COLOR_RGBA2GRAY);

                        MatOfRect faces = new MatOfRect();
                        faceCascade.detectMultiScale(grayMat, faces, 1.1, 2, 2, new org.opencv.core.Size(30, 30), new org.opencv.core.Size());

//                        int numFaces = faces.toArray().length;
//                        Log.d(TAG, "Number of faces detected: " + numFaces);


                        // Draw rectangles around detected faces
                        Rect[] facesArray = faces.toArray();
                        Bitmap bitmap = Bitmap.createBitmap(matRGBA.cols(), matRGBA.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(matRGBA, bitmap);

                        List<Rect> facesList = Arrays.asList(facesArray);
                        overlayView.setBitmap(bitmap);
                        overlayView.setFaces(facesList);



                        // Release Mats to avoid memory leaks
                        matYUV.release();
                        grayMat.release();
                        matRGBA.release();
                    }

                } finally {
                    imageProxy.close();
                }
            }
        });


        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }


    private void initializeFaceCascade(Context context) {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_default);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Initialize and load the faceCascade classifier
            faceCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (faceCascade.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
            } else {
                Log.d(TAG, "Cascade classifier initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing cascade classifier", e);
        }
    }


    private File createFile() {
        // Create a file to save the image or video
        return new File(getExternalMediaDirs()[0], "capture_" + System.currentTimeMillis() + ".mp4");
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                // Handle permissions not granted
                finish();
            }
        }
    }
}
