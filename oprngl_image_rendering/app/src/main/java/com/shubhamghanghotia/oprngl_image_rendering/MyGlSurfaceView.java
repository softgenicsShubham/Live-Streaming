package com.shubhamghanghotia.oprngl_image_rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class MyGlSurfaceView extends GLSurfaceView {

   private GlSurfaceRenderer glSurfaceRenderer;
    public MyGlSurfaceView(Context context, Bitmap bitmap) {
        super(context);
        setEGLContextClientVersion(2);
        glSurfaceRenderer = new GlSurfaceRenderer(bitmap);
        setRenderer(glSurfaceRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


}
