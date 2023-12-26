package com.shubhamghanghotia.oprngl_image_rendering;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GlSurfaceRenderer implements GLSurfaceView.Renderer {

    private Bitmap bitmap;
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer textureBuffer;
    private int program;
    private int textureId;
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

    public GlSurfaceRenderer(Bitmap bitmap) {
        this.bitmap = bitmap;

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

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        String vertexShaderCode =
                "attribute vec4 vPosition;" +
                        "attribute vec2 inputTextureCoordinate;" +
                        "varying vec2 textureCoordinate;" +
                        "void main() {" +
                        "  gl_Position = vPosition;" +
                        "  textureCoordinate = inputTextureCoordinate;" +
                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  gl_FragColor = texture2D(uTexture, textureCoordinate);" +
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  float gray = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));" +
//                        "  gl_FragColor = vec4(gray, gray, gray, texColor.a);" +
//                        "}";
//
//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  float r = (texColor.r * 0.393) + (texColor.g * 0.769) + (texColor.b * 0.189);" +
//                        "  float g = (texColor.r * 0.349) + (texColor.g * 0.686) + (texColor.b * 0.168);" +
//                        "  float b = (texColor.r * 0.272) + (texColor.g * 0.534) + (texColor.b * 0.131);" +
//                        "  gl_FragColor = vec4(r, g, b, texColor.a);" +
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  gl_FragColor = vec4(1.0 - texColor.r, 1.0 - texColor.g, 1.0 - texColor.b, texColor.a);" +
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "uniform float brightness;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  gl_FragColor = vec4(texColor.rgb * brightness, texColor.a);" +
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "uniform float brightness;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  vec3 correctedColor = texColor.rgb * brightness;" +
//                        "  gl_FragColor = vec4(correctedColor, texColor.a);" +
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  float gray = (texColor.r + texColor.g + texColor.b) / 3.0;" +
//                        "  gl_FragColor = vec4(gray, gray, gray, texColor.a);" +
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  gl_FragColor = vec4(texColor.r, 0.0, 0.0, texColor.a);" +
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  gl_FragColor = vec4(0.0, 0.0, texColor.b, texColor.a);" +
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  vec3 sepiaColor = vec3(0.393, 0.769, 0.189);" +
//                        "  float gray = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));" +
//                        "  vec3 sepia = mix(vec3(gray), sepiaColor, 0.5);" +
//                        "  gl_FragColor = vec4(sepia, texColor.a);" +
//                        "}";


//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 textureCoordinate;" +
//                        "const vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  float luminance = dot(texColor.rgb, luminanceWeighting);" +
//                        "  vec3 greyScaleColor = vec3(luminance);" +
//                        "  vec3 highSaturationColor = mix(greyScaleColor, texColor.rgb, 0.5);" +
//                        "  gl_FragColor = vec4(highSaturationColor, texColor.a);" +
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "uniform vec4 backgroundColor;" +  // Define the background color
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  gl_FragColor = mix(backgroundColor, texColor, texColor.a);" +  // Blend with background color
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "uniform vec4 backgroundColor;" +  // Define the background color
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  vec4 redBackground = vec4(1.0, 0.0, 0.0, 1.0);" +  // Red background color
//                        "  gl_FragColor = mix(redBackground, texColor, texColor.a);" +  // Blend with red background color
//                        "}";

//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "uniform vec4 backgroundColor;" +  // Define the background color
//                        "uniform vec4 overlayColor;" +      // Define the overlay color
//                        "varying vec2 textureCoordinate;" +
//                        "void main() {" +
//                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
//                        "  vec4 redOverlay = vec4(1.0, 0.0, 0.0, 0.5);" +  // Red overlay color with 50% opacity
//                        "  gl_FragColor = mix(mix(backgroundColor, texColor, texColor.a), overlayColor * redOverlay.a, overlayColor.a);" +
//                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform sampler2D uTexture;" +
                        "uniform vec4 overlayColor;" +  // Define the overlay color
                        "uniform vec4 faceMask;" +      // Define the face region mask or coordinates
                        "varying vec2 textureCoordinate;" +
                        "void main() {" +
                        "  vec4 texColor = texture2D(uTexture, textureCoordinate);" +
                        "  if (textureCoordinate.x > faceMask.x && textureCoordinate.x < faceMask.z && " +
                        "      textureCoordinate.y > faceMask.y && textureCoordinate.y < faceMask.w) {" +
                        "      gl_FragColor = mix(texColor, overlayColor, overlayColor.a);" +
                        "  } else {" +
                        "      gl_FragColor = texColor;" + // Keep the rest of the image unchanged
                        "  }" +
                        "}";





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

        // Load the texture
        textureId = loadTexture(bitmap);
        bitmap.recycle();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Use the program
        GLES20.glUseProgram(program);

        // Set the red background color uniform value before drawing
//        int backgroundColorHandle = GLES20.glGetUniformLocation(program, "backgroundColor");
//        GLES20.glUniform4f(backgroundColorHandle, 1.0f, 0.0f, 0.0f, 1.0f); // Red color values (R, G, B, A)

        // Set the red background color uniform value before drawing
//        int backgroundColorHandle = GLES20.glGetUniformLocation(program, "backgroundColor");
//        GLES20.glUniform4f(backgroundColorHandle, 1.0f, 0.0f, 0.0f, 1.0f); // Red color values (R, G, B, A)
//
//// Set the red overlay color uniform value before drawing
//        int overlayColorHandle = GLES20.glGetUniformLocation(program, "overlayColor");
//        GLES20.glUniform4f(overlayColorHandle, 1.0f, 0.0f, 0.0f, 0.5f); // Red color with 50% opacity (R, G, B, A)


        // Get handle to vertex shader's vPosition and texture coordinates
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
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private int loadTexture(Bitmap bitmap) {
        int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            bitmap.recycle();
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }
}
