package com.shubhamghanghotia.oprngl_image_rendering;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class AnotherRendrer implements GLSurfaceView.Renderer {

    private Bitmap bitmap;

    private int textureId;

    public AnotherRendrer(Bitmap bitmap) {
        this.bitmap = bitmap;
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.4f, 0.3f, 0.7f, 0.4f);

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

    }
}
