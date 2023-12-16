package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import cn.nodemedia.NodePublisher.OnNodePublisherEffectorListener;

public class EffectorListener implements OnNodePublisherEffectorListener {
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    private Mat grayFrame;

    @Override
    public void onCreateEffector(Context context) {
        // Initialize the grayscale frame Mat
        grayFrame = new Mat();
    }





    @Override
    public int onProcessEffector(int textureID, int width, int height) {



        return textureID;
    }
















    // Add this method to update the content of the texture from a Mat
    private void updateTextureFromMat(Mat mat, int textureID) {
        int dataSize = mat.cols() * mat.rows() * (int) mat.elemSize();
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(dataSize);
        mat.get(0, 0, byteBuffer.array());

        // Bind the original texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);

        // Update the texture content with the processed Mat data
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mat.cols(), mat.rows(),
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);

        // Unbind the texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }


    private int matToTexture(Mat mat) {
        int[] textureId = new int[1];

        // Generate a new OpenGL texture
        GLES20.glGenTextures(1, textureId, 0);

        // Bind the texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

        // Set texture parameters
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Allocate storage for the texture with the data from the Mat
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGRA);
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);

        // Load the bitmap into the OpenGL texture
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // Clean up
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        bitmap.recycle();
        mat.release();

        // Check for OpenGL errors
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e("OpenGL Error", "Error after texImage2D: " + error);
        }

        return textureId[0];
    }

    @Override
    public void onReleaseEffector() {
        // Release the grayscale frame Mat
        grayFrame.release();
    }

    @Nullable
    private Mat getMatFromTexture(int textureID, int width, int height) {
        int[] frameBuffer = new int[1];
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);

        // Create a texture to attach to the framebuffer
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[0], 0);

        // Check framebuffer completeness
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("Framebuffer", "Framebuffer is not complete: " + status);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glDeleteFramebuffers(1, frameBuffer, 0);
            GLES20.glDeleteTextures(1, textures, 0);
            return null;
        }

        // Read pixels from the framebuffer
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);

        // Reset buffer position
        buffer.position(0);

        // Create OpenCV Mat and put the buffer data
        Mat rgbaMat = new Mat();
        rgbaMat.create(height, width, CvType.CV_8UC4);
        byte[] pixelData = new byte[width * height * 4];
        buffer.get(pixelData);
        rgbaMat.put(0, 0, pixelData);

        // Clean up OpenGL resources
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDeleteFramebuffers(1, frameBuffer, 0);
        GLES20.glDeleteTextures(1, textures, 0);

        return rgbaMat;
    }
}
