package com.grpc.server;

import com.grpc.models.buffer.ChatMessage;
import com.grpc.models.buffer.ClientSpec;
import com.grpc.models.buffer.ChatServiceGrpc;
import com.grpc.models.buffer.MESSAGE_TYPE;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {

    Map<String, ClientSpec> clientMap = new HashMap<>();
    Map<String, StreamObserver<ChatMessage>> streamMap = new HashMap<>();
    Map<String, List<ChatMessage>> backlogMap = new HashMap<>();

    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
        return new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage chatMessage) {
                switch (chatMessage.getType()) {
                    case MESSAGE_TYPE_UNICAST -> streamMap.get(chatMessage.getTargetUUID()).onNext(chatMessage);
                    case MESSAGE_TYPE_BROADCAST -> {
                        for (String uuid: streamMap.keySet()) streamMap.get(uuid).onNext(chatMessage);
                    }
                    case MESSAGE_TYPE_STATUS -> {

                    }
                    default -> System.out.println("Message type unspecified!");
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void register(ClientSpec request, StreamObserver<ClientSpec> responseObserver) {
        clientMap.put(request.getUuid(), request);
        for (String key: clientMap.keySet()) {
            if (!Objects.equals(key, request.getUuid())) responseObserver.onNext(clientMap.get(key));
        }
    }
}