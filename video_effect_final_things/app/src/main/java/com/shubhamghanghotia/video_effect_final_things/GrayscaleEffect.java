package com.shubhamghanghotia.video_effect_final_things;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cn.nodemedia.NodePublisher;

public class GrayscaleEffect implements NodePublisher.OnNodePublisherEffectorListener {

    private int program;
    private int fbo;
    private int processedTextureID;

    // Vertex shader code remains the same
    String vertexShaderCode =
            "attribute vec4 vPosition;" +
                    "attribute vec2 inputTextureCoordinate;" +
                    "varying vec2 textureCoordinate;" +
                    "void main() {" +
                    "  gl_Position = vPosition;" +
                    "  textureCoordinate = inputTextureCoordinate;" +
                    "}";

    // Fragment shader code for grayscale effect
    private String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform sampler2D uTexture;" +
                    "varying vec2 textureCoordinate;" +
                    "void main() {" +
                    "  vec4 color = texture2D(uTexture, textureCoordinate);" +
                    "  float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));" +
                    "  gl_FragColor = vec4(vec3(gray), 1.0);" +
                    "}";

    @Override
    public void onCreateEffector(Context context) {
        program = createGrayscaleShaderProgram();
        Log.d("EffectGreyScale", "program" + program);
        fbo = createFBO();
        processedTextureID = getTextureFromFBO(fbo);
    }

    @Override
    public int onProcessEffector(int textureID, int width, int height) {
        GLES20.glUseProgram(program);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);

        drawFullscreenQuad();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);

        return processedTextureID;
    }

    @Override
    public void onReleaseEffector() {
        GLES20.glDeleteProgram(program);
        GLES20.glDeleteFramebuffers(1, new int[]{fbo}, 0);
        GLES20.glDeleteTextures(1, new int[]{processedTextureID}, 0);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private int createGrayscaleShaderProgram() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        return program;
    }

    private int createFBO() {
        int[] fboId = new int[1];
        GLES20.glGenFramebuffers(1, fboId, 0); // step 1.
        Log.d("EffectGreyScale", "step 1 " + " "+ fboId[0]);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]); // step 2.
        Log.d("EffectGreyScale", "step 2 " + " "+ fboId[0]);

        // Attach textures or renderbuffers to the FBO
        int textureId = createTexture(); // step 3

        Log.d("EffectGreyScale", "step 3 " + " "+ textureId);



        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId, 0);

        // step 4
        Log.d("EffectGreyScale", "step 4 " + " "+ fboId[0]);



        // Check framebuffer completeness
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER); // step 5
        Log.d("EffectGreyScale", "step 5" + " "+ fboId[0]);

        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Failed to create FBO. Status: " + status);
        }

        // Unbind the FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        return fboId[0];
    }


    private int createTexture() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    private int getTextureFromFBO(int fbo) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo);
        int texture = GLES20.GL_COLOR_ATTACHMENT0;
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return texture;
    }

    private void drawFullscreenQuad() {
        // Vertex coordinates for a full-screen quad
        float[] vertices = {
                -1.0f, 1.0f, 0.0f,  // Top-left
                -1.0f, -1.0f, 0.0f, // Bottom-left
                1.0f, -1.0f, 0.0f,  // Bottom-right
                1.0f, 1.0f, 0.0f    // Top-right
        };

        // Texture coordinates for the quad
        float[] textureCoords = {
                0.0f, 1.0f,  // Top-left
                0.0f, 0.0f,  // Bottom-left
                1.0f, 0.0f,  // Bottom-right
                1.0f, 1.0f   // Top-right
        };

        // Create and bind VBOs for vertices and texture coordinates
        int vertexBuffer = createVBO(vertices);
        int textureBuffer = createVBO(textureCoords);

        // Enable vertex attributes for position and texture coordinates
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);

        // Bind vertex data to attributes
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer);
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureBuffer);
        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);

        // Draw the quad with 2 triangles
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex attributes
        GLES20.glDisableVertexAttribArray(0);
        GLES20.glDisableVertexAttribArray(1);

        // Delete VBOs
        GLES20.glDeleteBuffers(1, new int[]{vertexBuffer}, 0);
        GLES20.glDeleteBuffers(1, new int[]{textureBuffer}, 0);
    }

    private int createVBO(float[] data) {
        int[] buffer = new int[1];
        GLES20.glGenBuffers(1, buffer, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(data);
        fb.position(0);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.length * 4, fb, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        return buffer[0];
    }
}
