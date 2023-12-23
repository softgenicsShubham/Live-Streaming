package cn.nodemedia;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NodePublisher1 {
    static {
        System.loadLibrary("NodeMediaClient");
        System.loadLibrary("opencv_java4");
    }

    public static final int LOG_LEVEL_ERROR = 0;
    public static final int LOG_LEVEL_INFO = 1;
    public static final int LOG_LEVEL_DEBUG = 2;

    public static final int NMC_CODEC_ID_H264 = 27;
    public static final int NMC_CODEC_ID_H265 = 173;
    public static final int NMC_CODEC_ID_AAC = 86018;

    public static final int NMC_PROFILE_AUTO = 0;
    public static final int NMC_PROFILE_H264_BASELINE = 66;
    public static final int NMC_PROFILE_H264_MAIN = 77;
    public static final int NMC_PROFILE_H264_HIGH = 100;
    public static final int NMC_PROFILE_H265_MAIN = 1;
    public static final int NMC_PROFILE_AAC_LC = 1;
    public static final int NMC_PROFILE_AAC_HE = 4;
    public static final int NMC_PROFILE_AAC_HE_V2 = 28;
    public static final int NMC_PROFILE_AAC_LD = 22;
    public static final int NMC_PROFILE_AAC_ELD = 38;

    public static final int VIDEO_RC_CRF = 0;
    public static final int VIDEO_RC_ABR = 1;
    public static final int VIDEO_RC_CBR = 2;
    public static final int VIDEO_RC_VBV = 3;

    public static final int VIDEO_ORIENTATION_PORTRAIT = 0;
    public static final int VIDEO_ORIENTATION_LANDSCAPE_RIGHT = 1;
    public static final int VIDEO_ORIENTATION_LANDSCAPE_LEFT = 3;

    public static final int EffectorTextureTypeT2D = 0;
    public static final int EffectorTextureTypeEOS = 1;


    private Camera camera;
    private GLCameraView glCameraView;

    private ImageAnalysis imageAnalysis;
    private Context context;


    private long id;
    private int fpsCount;
    private long fpsTime;
    private boolean isOpenFrontCamera = false;
    private int videoOrientation = Surface.ROTATION_0;
    private int videoWidth = 720;
    private int videoHeight = 1280;
    private int cameraWidth = 0;
    private int cameraHeight = 0;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;





    private final ViewGroup.LayoutParams LP = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
    );


    public NodePublisher1(@NonNull Context context, String license) {
        this.context = context;
        id = jniInit(context, license);
    }

    public void attachView(@NonNull ViewGroup viewGroup){
        if (glCameraView == null) {
            this.glCameraView = new GLCameraView(this.context);
            this.glCameraView.setLayoutParams(LP);
            this.glCameraView.setKeepScreenOn(true);
            viewGroup.addView(this.glCameraView);
        }
    }

    public void detachView() {
        if (this.glCameraView != null) {
            this.glCameraView.setKeepScreenOn(false);
            this.glCameraView = null;
            closeCamera();
            GPUImageDestroy();
        }
    }


    public void switchCamera() {
        this.isOpenFrontCamera = !this.isOpenFrontCamera;
        closeCamera();
        openCamera(this.isOpenFrontCamera);
    }

    public void setVideoOrientation(int orientation) {
        this.videoOrientation = orientation;
    }


    public void openCamera(boolean frontCamera){
        this.isOpenFrontCamera = frontCamera;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(()-> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider, this.isOpenFrontCamera);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this.context));
    }

    public void closeCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this.context));
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider, boolean front) {
        if (context != null && glCameraView != null && imageAnalysis != null) {
            CameraSelector cameraSelector = front ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
            Preview preview = new Preview.Builder()
                    .setTargetResolution(new Size(videoWidth, videoHeight))
                    .setTargetRotation(videoOrientation)
                    .build();

            preview.setSurfaceProvider(glCameraView.getSurfaceProvider());
            camera = cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, preview, imageAnalysis);
        } else {
            // Handle the case where any of the required components are null
        }
    }









    private native long jniInit(Context context, String license);
    private native void jniFree();
    public native void setLogLevel(int logLevel);
    public native void setHWAccelEnable(boolean enable);
    public native void setDenoiseEnable(boolean enable);
    public native void setVideoFrontMirror(boolean mirror);
    public native void setCameraFrontMirror(boolean mirror);
    public native void setAudioCodecParam(int codec, int profile, int sampleRate, int channels, int bitrate);
    public native void setVideoCodecParam(int codec, int profile, int width, int height, int fps, int bitrate);
    public native void setVideoRateControl(int rc);
    public native void setKeyFrameInterval(int keyFrameInterval);
    public native void setCryptoKey(@NonNull String cryptoKey);
    public native void setEnhancedRtmp(boolean enhancedRtmp);
    public native void setVolume(float volume);
    public native int addOutput(@NonNull String url);
    public native int removeOutputs();
    public native int start(@NonNull String url);
    public native int stop();
    public native void setEffectorTextureType(int type);
    private native int GPUImageCreate(int textureID);
    private native int GPUImageChange(int sw, int sh, int cw, int ch, int so, int co, boolean f);
    private native int GPUImageDraw(int textureID, float[] mtx, int len);
    private native int GPUImageDestroy();
    private native int GPUImageGenOESTextureID();


    public CameraInfo getCameraInfo() {
        return camera.getCameraInfo();
    }

    private void onViewChange() {
        if (this.cameraWidth == 0 || this.cameraHeight == 0 || this.surfaceWidth == 0 || this.surfaceHeight == 0) {
            return;
        }
        WindowManager wm = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        int surfaceRotation = wm.getDefaultDisplay().getRotation();
        int sensorRotationDegrees = getCameraInfo().getSensorRotationDegrees(this.videoOrientation);
        GPUImageChange(this.surfaceWidth, this.surfaceHeight, this.cameraWidth, this.cameraHeight, surfaceRotation, sensorRotationDegrees, this.isOpenFrontCamera);
    }



    private class GLCameraView extends GLSurfaceView implements GLSurfaceView.Renderer {
        private static final String TAG = "NodeMedia.GLCameraView";

        private SurfaceTexture surfaceTexture;
        private int textureId = -1;
        private float transformMatrix[] = new float[16];


        private Context context;




        private Preview.SurfaceProvider getSurfaceProvider() {
            return request -> {
                Size resolution = request.getResolution();
                surfaceTexture.setDefaultBufferSize(resolution.getWidth(), resolution.getHeight());
                request.provideSurface(new Surface(surfaceTexture), ContextCompat.getMainExecutor(this.context), result -> {
                    result.getSurface().release();
                });
                this.queueEvent(() -> {
                    NodePublisher1.this.cameraWidth = resolution.getWidth();
                    NodePublisher1.this.cameraHeight = resolution.getHeight();
                    NodePublisher1.this.onViewChange();
                });
            };
        }








        public GLCameraView(Context context) {
            super(context);
            this.context = context;
            setEGLContextClientVersion(2);
            setRenderer(this);
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            textureId = GPUImageGenOESTextureID();
            surfaceTexture = new SurfaceTexture(textureId);
            surfaceTexture.setOnFrameAvailableListener(surfaceTexture -> requestRender());
            NodePublisher1.this.GPUImageCreate(textureId);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int w, int h) {
            NodePublisher1.this.surfaceHeight = w;
            NodePublisher1.this.surfaceWidth = h;
            NodePublisher1.this.onViewChange();
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(transformMatrix);
            NodePublisher1.this.GPUImageDraw(textureId, transformMatrix, transformMatrix.length);
        }
    }













}
