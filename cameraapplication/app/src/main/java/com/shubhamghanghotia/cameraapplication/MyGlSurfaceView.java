package com.shubhamghanghotia.cameraapplication;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class MyGlSurfaceView extends GLSurfaceView {

   GlSurfaceRenderer glSurfaceRenderer;
    public MyGlSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        glSurfaceRenderer = new GlSurfaceRenderer();
        setRenderer(glSurfaceRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
