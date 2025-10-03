package com.grpc.server;

import com.grpc.models.buffer.ChatMessage;
import com.grpc.models.buffer.ClientSpec;
import com.grpc.models.buffer.ChatServiceGrpc;
import com.grpc.models.buffer.MESSAGE_TYPE;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger("ChatService");
    // Message stream related
    Map<String, StreamObserver<ChatMessage>> streamMap = new HashMap<>();
    Map<String, List<ChatMessage>> backlogMap = new HashMap<>();

    // Register (Client info) stream related
    Map<String, ClientSpec> clientMap = new HashMap<>();

    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
        return new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage chatMessage) {
                log.info(chatMessage.getMessage());
                switch (chatMessage.getType()) {
                    case MESSAGE_TYPE_INIT -> {
                        log.info("Client {} init", chatMessage.getSourceUUID());
                        streamMap.put(chatMessage.getSourceUUID(), responseObserver);
                    }
                    case MESSAGE_TYPE_UNICAST -> streamMap.get(chatMessage.getTargetUUID()).onNext(chatMessage);
                    case MESSAGE_TYPE_BROADCAST -> {
                        log.info("Broadcast");
                        for (String uuid: streamMap.keySet()) streamMap.get(uuid).onNext(chatMessage);
                    }
                    case MESSAGE_TYPE_STATUS -> {

                    }
                    default -> System.out.println("Message type unspecified!");
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error(t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void register(ClientSpec request, StreamObserver<ClientSpec> responseObserver) {
        log.info("Client register");
        clientMap.put(request.getUuid(), request);
        for (String key: clientMap.keySet()) {
            if (!Objects.equals(key, request.getUuid())) responseObserver.onNext(clientMap.get(key));
        }
        responseObserver.onCompleted();
    }
}