/**
 * ©2023 NodeMedia
 * <p>
 * Copyright © 2015 - 2023 NodeMedia.All Rights Reserved.
 */

package cn.nodemedia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NodePublisher {
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

    private ImageAnalysis imageAnalysis;

    private final FrameLayout.LayoutParams LP = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER);

    public NodePublisher(@NonNull Context context, @NonNull String license) {
        ctx = context;
        id = jniInit(context, license);
    }


    private void createImageAnalysis() {
        if (ctx != null) {
            imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(videoWidth, videoHeight))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx), new ImageAnalysis.Analyzer() {
                @OptIn(markerClass = ExperimentalGetImage.class)
                @Override
                public void analyze(@NonNull ImageProxy imageProxy) {
                    // Image analysis logic
                    int imageWidth = imageProxy.getWidth();
                    int imageHeight = imageProxy.getHeight();
                    Log.d(TAG, "Image Dimensions: " + imageWidth + " x " + imageHeight);


                    Image image = imageProxy.getImage();

                    if (image != null) {
                        Mat inputFrame = new Mat(image.getHeight(),  image.getWidth(), CvType.CV_8UC1);
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer yBuffer = planes[0].getBuffer();
                        ByteBuffer uvBuffer = planes[1].getBuffer();
                        int ySize = yBuffer.remaining();
                        int uvSize = uvBuffer.remaining();

                        byte[] data = new byte[ySize + uvSize];
                        yBuffer.get(data, 0, ySize);
                        uvBuffer.get(data, ySize, uvSize);
                        inputFrame.put(0, 0, data);

                        Mat processedFrame = new Mat();
                        Imgproc.cvtColor(inputFrame, processedFrame, Imgproc.COLOR_YUV2RGBA_NV21); // Convert to RGBA for display

                        Log.d(TAG, "Process rgba channel: " + processedFrame.width() + " x " + processedFrame.height());


                        int textureId = convertMatToOpenGLTexture(processedFrame);
                        Log.d(TAG, "Texture ID: " + textureId);




                    }

                    image.close();
                    imageProxy.close();
                }
            });
        }
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
                createImageAnalysis(); // Ensure imageAnalysis is properly created
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
        if (ctx != null && glpv != null && imageAnalysis != null) {
            CameraSelector cameraSelector = front ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
            Preview preview = new Preview.Builder()
                    .setTargetResolution(new Size(videoWidth, videoHeight))
                    .setTargetRotation(videoOrientation)
                    .build();

            preview.setSurfaceProvider(glpv.getSurfaceProvider());
            mCamera = cameraProvider.bindToLifecycle((LifecycleOwner) ctx, cameraSelector, preview, imageAnalysis);
        } else {
            // Handle the case where any of the required components are null
        }
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

    // Method to convert OpenCV Mat to OpenGL texture
    private int convertMatToOpenGLTexture(Mat mat) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        int textureId = textures[0];

        if (textureId != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            // Configure texture sampling/filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            // Allocate space for texture data
            Mat flippedMat = new Mat();
            Core.flip(mat, flippedMat, 0); // Flip the image vertically as OpenGL expects the bottom row to be first

            Bitmap bitmap = null;
            try {
                bitmap = Bitmap.createBitmap(flippedMat.cols(), flippedMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(flippedMat, bitmap);

                if (bitmap != null) {
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                flippedMat.release();
            }

            // Clean up
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        } else {
            // Handle the case where texture generation failed
            Log.e(TAG, "Error generating OpenGL texture.");
        }

        return textureId;
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

        private int programId;
        private int uTextureLocation;
        private int uEffectTypeLocation;

        private static final int EFFECT_GRAYSCALE = 1;

        private int grayscaleProgramId;


        private static final String GRAYSCALE_FRAGMENT_SHADER =
                "precision mediump float;\n" +
                        "varying vec2 textureCoordinate;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "void main() {\n" +
                        "    vec4 color = texture2D(uTexture, textureCoordinate);\n" +
                        "    float gray = (color.r + color.g + color.b) / 3.0;\n" +
                        "    gl_FragColor = vec4(gray, gray, gray, 1.0);\n" +
                        "}\n";

        private static final String VERTEX_SHADER =
                "attribute vec4 aPosition;\n" +
                        "attribute vec2 aTexCoord;\n" +
                        "varying vec2 textureCoordinate;\n" +
                        "void main() {\n" +
                        "    gl_Position = aPosition;\n" +
                        "    textureCoordinate = aTexCoord;\n" +
                        "}\n";


        private int loadShader(int type, String shaderSource) {
            int shader = GLES20.glCreateShader(type);
            if (shader != 0) {
                GLES20.glShaderSource(shader, shaderSource);
                GLES20.glCompileShader(shader);

                int[] compiled = new int[1];
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
                if (compiled[0] == 0) {
                    // Compilation failed, handle the error or log the shader info log
                    Log.e("ShaderCompilation", "Shader compilation failed: " + GLES20.glGetShaderInfoLog(shader));
                    GLES20.glDeleteShader(shader);
                    shader = 0;
                }
            }
            return shader;
        }


        private int loadShaderProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);

            return program;
        }

        public void switchEffect(int effectType) {
            GLES20.glUseProgram(programId);

            // Set the effect type
            GLES20.glUniform1i(uEffectTypeLocation, effectType);

            // Additional handling based on effect type (if needed)
        }


        protected GLCameraView(Context context) {
            super(context);
            this.context = context;
            grayscaleProgramId = loadShaderProgram(VERTEX_SHADER, GRAYSCALE_FRAGMENT_SHADER);
            if (grayscaleProgramId != 0) {
                uTextureLocation = GLES20.glGetUniformLocation(grayscaleProgramId, "uTexture");
                uEffectTypeLocation = GLES20.glGetUniformLocation(grayscaleProgramId, "effectType");
            }
            setEGLContextClientVersion(2);
            setRenderer(this);
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
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

        public void switchToGrayscaleEffect() {
            GLES20.glUseProgram(grayscaleProgramId);
            GLES20.glUniform1i(uEffectTypeLocation, EFFECT_GRAYSCALE);
        }


        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            textureId = GPUImageGenOESTextureID();
            surfaceTexture = new SurfaceTexture(textureId);
            surfaceTexture.setOnFrameAvailableListener(surfaceTexture -> requestRender());
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

            switchToGrayscaleEffect(); // Always apply grayscale effect for simplicity


            NodePublisher.this.GPUImageDraw(textureId, transformMatrix, transformMatrix.length);
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