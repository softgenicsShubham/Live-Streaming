package com.shubhamghanghotia.opengl_testing;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.nodemedia.NodePublisher;

public class WorkingEffector implements NodePublisher.OnNodePublisherEffectorListener {

    private Context context;
    private float transformMatrix[] = new float[16];

    private int textureId = -1;

    private int textureHandle;

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

    private int generateTexture(){
        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        }
        Log.d("Effector", "textureid"+ " " + textureHandle[0]);
        return textureHandle[0];
    };

    public WorkingEffector() {
        initalize();
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }


    @Override
    public void onCreateEffector(Context context) {
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

        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        textureCoordinateHandle = GLES20.glGetAttribLocation(program, "inputTextureCoordinate");
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture");

        // Generate a texture for rendering the effect
        textureId = generateTexture();

    }

    @Override
    public int onProcessEffector(int originalTextureID, int width, int height) {
        // Bind our generated texture for rendering
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        // Set texture parameters for handling incoming texture
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        // Use our shader program for rendering
        GLES20.glUseProgram(program);

        // Bind vertex and texture coordinate buffers
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);

        // Bind the input texture to the uniform
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, originalTextureID);
        GLES20.glUniform1i(textureHandle, 0);

        // Draw the textured quad to apply the effect
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Unbind textures and shader program
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);

        // Return the modified texture ID (if NodePublisher expects it)
        return textureId;
    }


    @Override
    public void onReleaseEffector() {
        // Delete generated texture
        GLES20.glDeleteTextures(1, new int[]{textureId}, 0);

        // Delete shader program
        GLES20.glDeleteProgram(program);
    }

}
