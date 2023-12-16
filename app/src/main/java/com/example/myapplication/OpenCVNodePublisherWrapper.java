package com.example.myapplication;

import android.content.Context;
import android.widget.FrameLayout;

import org.opencv.core.Mat;

import cn.nodemedia.NodePublisher;
import cn.nodemedia.NodePublisher.OnNodePublisherEffectorListener;
import cn.nodemedia.NodePublisher.OnNodePublisherEventListener;

public class OpenCVNodePublisherWrapper {
    private NodePublisher nodePublisher;
    private OnNodePublisherEffectorListener effectorListener;
    private OnNodePublisherEventListener eventListener;


    public OpenCVNodePublisherWrapper(Context context, String license) {
        // Initialize the wrapped NodePublisher instance
        nodePublisher = new NodePublisher(context, license);
        nodePublisher.setHWAccelEnable(true);

        effectorListener = new EffectorListener();
        eventListener = new EventListener();

        nodePublisher.setOnNodePublisherEffectorListener(effectorListener);
        nodePublisher.setOnNodePublisherEventListener(eventListener);

    }

    // Delegate methods from NodePublisher
    public void setAudioCodecParam(int codec, int profile, int sampleRate, int channels, int bitrate) {
        nodePublisher.setAudioCodecParam(codec, profile, sampleRate, channels, bitrate);
    }

    public void setVideoOrientation(int orientation) {
        nodePublisher.setVideoOrientation(orientation);

    }

    public void setVideoCodecParam(int codec, int profile, int width, int height, int fps, int bitrate) {
        nodePublisher.setVideoCodecParam(codec, profile, width, height, fps, bitrate);
    }

    public void attachView(FrameLayout vg) {
        nodePublisher.attachView(vg);
    }

    public void openCamera(boolean frontCamera) {
        nodePublisher.openCamera(frontCamera);
    }

    public void switchCamera() {
        nodePublisher.switchCamera();
    }

    public void startPublishing(String rtmpUrl) {
        nodePublisher.start(rtmpUrl);
    }

    public void stopPublishing() {
        nodePublisher.stop();
    }

    public void onDestroy() {
        // Detach view, close camera, and stop streaming
        nodePublisher.detachView();
        nodePublisher.closeCamera();
        nodePublisher.stop();

    }

}
