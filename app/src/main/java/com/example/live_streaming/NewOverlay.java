package com.example.live_streaming;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;

public class NewOverlay extends View {

    private Mat grayMat; // Store the processed grayscale Mat here
    private Bitmap rotatedBitmap; // Store the rotated grayscale bitmap for rendering

    private Paint paint;

    public NewOverlay(Context context) {
        super(context);
        init();
    }

    public NewOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NewOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize paint for drawing
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE); // Set your desired style
        paint.setStrokeWidth(3); // Set your desired stroke width
    }

    public void setGrayMat(Mat grayMat) {
        this.grayMat = grayMat;

        // Convert the grayscale Mat to a Bitmap and rotate it
        rotatedBitmap = matToRotatedBitmap(grayMat);

        // Request a redraw when the Mat is updated
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (rotatedBitmap != null) {
            // Draw the rotated bitmap on the canvas
            canvas.drawBitmap(rotatedBitmap, 0, 0, paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Set the measured dimensions to fill the entire screen
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    // Utility method to convert Mat to rotated Bitmap
    private Bitmap matToRotatedBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);

        // Rotate the bitmap by 90 degrees
        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
