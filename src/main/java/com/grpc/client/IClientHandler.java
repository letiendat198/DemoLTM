package com.grpc.client;

import com.grpc.models.buffer.ChatMessage;
import com.grpc.models.buffer.ClientSpec;

import java.util.Map;

public interface IClientHandler {
    public void onMessage(ChatMessage msg);
    public void onRegisterComplete(boolean success, Map<String, ClientSpec> targetMap);
}
