package com.example.myapplication;

import cn.nodemedia.NodePublisher;

public class EventListener implements NodePublisher.OnNodePublisherEventListener {
    @Override
    public void onEventCallback(NodePublisher publisher, int event, String msg) {
        // Implement your logic for handling NodePublisher events
    }
}