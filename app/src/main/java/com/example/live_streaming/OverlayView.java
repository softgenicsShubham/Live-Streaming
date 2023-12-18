package com.example.live_streaming;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    private Bitmap bitmap;
    private Paint paint;
    private List<Rect> faces;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        faces = new ArrayList<>();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        invalidate(); // Trigger onDraw
    }

    public void setFaces(List<Rect> faces) {
        this.faces = faces;
        invalidate(); // Trigger onDraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            // Draw the bitmap
            canvas.drawBitmap(bitmap, 0, 0, null);
        }

        // Draw rectangles around detected faces
        for (Rect face : faces) {
            // Convert double values to float
            float left = (float) face.tl().x;
            float top = (float) face.tl().y;
            float right = (float) face.br().x;
            float bottom = (float) face.br().y;

            // Adjust for display rotation
            int displayRotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            int sensorOrientation = displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180 ? 90 : 0;

            Log.d("OverlayView", "Before Adjust: left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom);

// Adjust for display rotation

            Log.d("OverlayView", "After Adjust: left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom);


            if (sensorOrientation == 90) {
                float temp = left;
                left = top;
                top = temp;

                temp = right;
                right = bottom;
                bottom = temp;
            }

            // Draw the rectangle
            canvas.drawRect(left, top, right, bottom, paint);
        }
    }


}
