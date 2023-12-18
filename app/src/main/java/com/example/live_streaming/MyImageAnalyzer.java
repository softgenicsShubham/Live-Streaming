package com.example.live_streaming;
import android.content.Context;
import android.util.Log; // Add this import statement
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MyImageAnalyzer implements ImageAnalysis.Analyzer {
    private static final String TAG = "MyImageAnalyzer"; // Add this constant for the tag

    private Mat mRgba;
    private CascadeClassifier faceCascade;

    public MyImageAnalyzer(Context context) {
        initializeOpenCV(context);
        initializeFaceCascade(context);
    }

    private void initializeOpenCV(Context context) {
        OpenCVLoader.initDebug();
        Log.d(TAG, "OpenCV initialized");
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

            faceCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
            Log.d(TAG, "Cascade classifier initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing cascade classifier", e);
        }
    }

    @Override
    public void analyze(ImageProxy imageProxy) {
        try {
            if (mRgba == null) {
                mRgba = new Mat();
            }

            ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            int yuvSize = imageProxy.getHeight() + imageProxy.getHeight() / 2;
            Mat yuvMat = new Mat(yuvSize, imageProxy.getWidth(), CvType.CV_8UC1);
            yuvMat.put(0, 0, data);

            Imgproc.cvtColor(yuvMat, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            yuvMat.release();

            Mat grayMat = new Mat();
            Imgproc.cvtColor(mRgba, grayMat, Imgproc.COLOR_RGBA2GRAY);

            MatOfRect faces = new MatOfRect();
            faceCascade.detectMultiScale(grayMat, faces, 1.1, 2, 2, new Size(30, 30), new Size());

            // Draw rectangles around detected faces
            Rect[] facesArray = faces.toArray();
            for (Rect face : facesArray) {
                Imgproc.rectangle(mRgba, face.tl(), face.br(), new Scalar(255, 0, 0, 255), 3);
            }

            grayMat.release();

            // Perform additional image processing or visualization if needed

            // Don't forget to close the imageProxy to avoid memory leaks
            imageProxy.close();
        } catch (Exception e) {
            Log.e(TAG, "Error in image analysis", e);
        }
    }
}
