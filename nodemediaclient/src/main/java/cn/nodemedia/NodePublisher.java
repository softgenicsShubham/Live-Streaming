
package cn.nodemedia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
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
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutionException;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NodePublisher {
    static {
        System.loadLibrary("NodeMediaClient");
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


    private static final String TAG = "NodeMedia.java";
    private OnNodePublisherEventListener onNodePublisherEventListener;
    private OnNodePublisherEffectorListener onNodePublisherEffectorListener;
    private GLCameraView glpv;
    private Camera mCamera;
    private Context ctx;
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

    private final FrameLayout.LayoutParams LP = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER);

    public NodePublisher(@NonNull Context context, @NonNull String license) {
        ctx = context;
        id = jniInit(context, license);
    }

    public void setOnNodePublisherEventListener(OnNodePublisherEventListener onNodePublisherEventListener) {
        this.onNodePublisherEventListener = onNodePublisherEventListener;
    }

    public void setOnNodePublisherEffectorListener(OnNodePublisherEffectorListener onNodePublisherEffectorListener) {
        this.onNodePublisherEffectorListener = onNodePublisherEffectorListener;
    }

    public void attachView(@NonNull ViewGroup vg) {
        if (this.glpv == null) {
            this.glpv = new GLCameraView(this.ctx);
            this.glpv.setLayoutParams(LP);
            this.glpv.setKeepScreenOn(true);
            vg.addView(this.glpv);
        }
    }

    public void detachView() {
        if (this.glpv != null) {
            this.glpv.setKeepScreenOn(false);
            this.glpv = null;
            closeCamera();
            GPUImageDestroy();
        }
    }

    public void setVideoOrientation(int orientation) {
        this.videoOrientation = orientation;
    }

    public void openCamera(boolean frontCamera) {
        this.isOpenFrontCamera = frontCamera;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(ctx);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider, this.isOpenFrontCamera);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this.ctx));
    }

    public void closeCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(ctx);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this.ctx));
    }

    public void switchCamera() {
        this.isOpenFrontCamera = !this.isOpenFrontCamera;
        closeCamera();
        openCamera(this.isOpenFrontCamera);
    }

    public Camera getCamera() {
        return mCamera;
    }

    public CameraInfo getCameraInfo() {
        return mCamera.getCameraInfo();
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider, boolean front) {
        CameraSelector cameraSelector = front ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;

        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(videoWidth, videoHeight))
                .setTargetRotation(videoOrientation)
                .build();
        preview.setSurfaceProvider(this.glpv.getSurfaceProvider());
        mCamera = cameraProvider.bindToLifecycle((LifecycleOwner) this.ctx, cameraSelector  , preview);
    }

    private void onEvent(int event, String msg) {
//        Log.d(TAG, "on Event: " + event + " Message:" + msg);
        if (this.onNodePublisherEventListener != null) {
            this.onNodePublisherEventListener.onEventCallback(this, event, msg);
        }
    }

    private void onCreateEffector() {
        if (this.onNodePublisherEffectorListener != null) {
            this.onNodePublisherEffectorListener.onCreateEffector(this.ctx);
        }
    }

    private int onProcessEffector(int textureID) {
        if (this.onNodePublisherEffectorListener != null) {
            textureID = this.onNodePublisherEffectorListener.onProcessEffector(textureID, this.videoWidth, this.videoHeight);
        }
        return textureID;
    }

    private void onReleaseEffector() {
        if (this.onNodePublisherEffectorListener != null) {
            this.onNodePublisherEffectorListener.onReleaseEffector();
        }
    }

    protected void finalize() {
        jniFree();
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

    /**
     * 设置视频解密密码
     *
     * @param cryptoKey 16字节密码
     */
    public native void setCryptoKey(@NonNull String cryptoKey);

    /**
     * 设置是否使用enhanced-rtmp 标准推流
     * @param enhancedRtmp
     */
    public native void setEnhancedRtmp(boolean enhancedRtmp);

    /**
     * 设置音量
     * 0.0 最小值 麦克风静音
     * 1.0 默认值 原始音量
     * 2.0 最大值 增益音量
     * @param volume 0.0 ~~ 2.0
     */
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

    private void onViewChange() {
        if (this.cameraWidth == 0 || this.cameraHeight == 0 || this.surfaceWidth == 0 || this.surfaceHeight == 0) {
            return;
        }
        WindowManager wm = (WindowManager) this.ctx.getSystemService(Context.WINDOW_SERVICE);
        int surfaceRotation = wm.getDefaultDisplay().getRotation();
        int sensorRotationDegrees = getCameraInfo().getSensorRotationDegrees(this.videoOrientation);
        GPUImageChange(this.surfaceWidth, this.surfaceHeight, this.cameraWidth, this.cameraHeight, surfaceRotation, sensorRotationDegrees, this.isOpenFrontCamera);
    }

    private class GLCameraView extends GLSurfaceView implements GLSurfaceView.Renderer {
        private static final String TAG = "NodeMedia.GLCameraView";

        private SurfaceTexture surfaceTexture;
        private int textureId = -1;
        private Context context;
        private float transformMatrix[] = new float[16];

        private FloatBuffer vertexBuffer;
        private FloatBuffer textureBuffer;
        private int program;
        private int positionHandle;
        private int textureCoordinateHandle;

        private final float[] vertices = {
                -1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
        };

        private final float[] textureCoordinates = {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
        };

        String vertexShaderCode =
                "attribute vec4 vPosition;" +
                        "attribute vec2 inputTextureCoordinate;" +
                        "varying vec2 textureCoordinate;" +
                        "void main() {" +
                        "  gl_Position = vPosition;" +
                        "  textureCoordinate = inputTextureCoordinate;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform sampler2D uTexture;" +
                        "varying vec2 textureCoordinate;" +
                        "void main() {" +
                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
                        "  float gray = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));" +
                        "  gl_FragColor = vec4(gray, gray, gray, texColor.a);" +
                        "}";

        private void initalize() {
            // Initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.position(0);

            // Initialize texture coordinate byte buffer
            ByteBuffer tb = ByteBuffer.allocateDirect(textureCoordinates.length * 4);
            tb.order(ByteOrder.nativeOrder());
            textureBuffer = tb.asFloatBuffer();
            textureBuffer.put(textureCoordinates);
            textureBuffer.position(0);
        }

        protected GLCameraView(Context context) {
            super(context);
            this.context = context;
            setEGLContextClientVersion(2);
            setRenderer(this);
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            initalize();
        }

        private Preview.SurfaceProvider getSurfaceProvider() {
            return request -> {
                Size resolution = request.getResolution();
                surfaceTexture.setDefaultBufferSize(resolution.getWidth(), resolution.getHeight());
                request.provideSurface(new Surface(surfaceTexture), ContextCompat.getMainExecutor(this.context), result -> {
                    result.getSurface().release();
                });
                this.queueEvent(() -> {
                    NodePublisher.this.cameraWidth = resolution.getWidth();
                    NodePublisher.this.cameraHeight = resolution.getHeight();
                    NodePublisher.this.onViewChange();
                });
            };
        }

        private int generateTexture(){
            int[] textureHandle = new int[1];
            GLES20.glGenTextures(1, textureHandle, 0);
            if (textureHandle[0] != 0) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            }
            Log.d(TAG, "textureid"+ " " + textureHandle[0]);
            return textureHandle[0];
        };

        private void drawWhiteOverlay() {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            int overlaySize = 200;
            int screenWidth = getWidth();
            int screenHeight = getHeight();
            int left = (screenWidth - overlaySize) / 2;
            int top = (screenHeight - overlaySize) / 2;
            int right = left + overlaySize;
            int bottom = top + overlaySize;

            // Set the color to white (R, G, B, Alpha)
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glDisable(GLES20.GL_BLEND);
        }





        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            textureId = GPUImageGenOESTextureID();
            surfaceTexture = new SurfaceTexture(textureId);


            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            // Create empty OpenGL ES Program
            program = GLES20.glCreateProgram();

            // Add the vertex shader to program
            GLES20.glAttachShader(program, vertexShader);

            // Add the fragment shader to program
            GLES20.glAttachShader(program, fragmentShader);

            // Create OpenGL ES program executables
            GLES20.glLinkProgram(program);



            surfaceTexture.setOnFrameAvailableListener(surfaceTexture -> {
                requestRender();
            });

            NodePublisher.this.GPUImageCreate(textureId);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int w, int h) {
            NodePublisher.this.surfaceWidth = w;
            NodePublisher.this.surfaceHeight = h;
            NodePublisher.this.onViewChange();
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(transformMatrix);
            GLES20.glUseProgram(program);

            positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            textureCoordinateHandle = GLES20.glGetAttribLocation(program, "inputTextureCoordinate");

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glEnableVertexAttribArray(textureCoordinateHandle);

            // Set the vertex attribute pointers
            GLES20.glVertexAttribPointer(positionHandle, 3,
                    GLES20.GL_FLOAT, false,
                    12, vertexBuffer);
            GLES20.glVertexAttribPointer(textureCoordinateHandle, 2,
                    GLES20.GL_FLOAT, false,
                    8, textureBuffer);

            // Set the active texture unit to texture unit 0
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            // Bind the texture to this unit
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            // Set the sampler texture unit to 0
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0);

            // Draw the quad
//            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            // Disable vertex array
//            GLES20.glDisableVertexAttribArray(positionHandle);
//            GLES20.glDisableVertexAttribArray(textureCoordinateHandle);


            NodePublisher.this.GPUImageDraw(textureId, transformMatrix, transformMatrix.length);

        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }


    }

    public interface OnNodePublisherEffectorListener {

        void onCreateEffector(Context context);

        int onProcessEffector(int textureID, int width, int height);

        void onReleaseEffector();

    }

    public interface OnNodePublisherEventListener {
        void onEventCallback(NodePublisher publisher, int event, String msg);
    }
}