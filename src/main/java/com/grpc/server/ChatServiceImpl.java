package com.grpc.server;

import com.grpc.models.buffer.ChatMessage;
import com.grpc.models.buffer.ClientSpec;
import com.grpc.models.buffer.ChatServiceGrpc;
import com.grpc.models.buffer.MESSAGE_TYPE;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger("ChatService");
    // Message stream related
    Map<String, StreamObserver<ChatMessage>> streamMap = new HashMap<>();
    Map<String, List<ChatMessage>> backlogMap = new HashMap<>();

    // Register (Client info) stream related
    Map<String, ClientSpec> clientMap = new HashMap<>();
    List<StreamObserver<ClientSpec>> clientStreamList = new ArrayList<>();

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
                    case MESSAGE_TYPE_UNICAST -> {
                        try {
                            streamMap.get(chatMessage.getTargetUUID()).onNext(chatMessage);
                        }
                        // Can't reach target, stream already closed
                        catch (StatusRuntimeException sre) {
                            log.error(sre.getMessage());
                            ChatMessage errorMsg = ChatMessage.newBuilder()
                                    .setSourceUUID("Server")
                                    .setTargetUUID(chatMessage.getSourceUUID())
                                    .setMessage("Target not reachable")
                                    .setType(MESSAGE_TYPE.MESSAGE_TYPE_UNICAST)
                                    .build();
                            responseObserver.onNext(errorMsg);
                        }
                    }
                    case MESSAGE_TYPE_BROADCAST -> {
                        log.info("Broadcast");
                        for (String uuid: streamMap.keySet()) {
                            try {
                                streamMap.get(uuid).onNext(chatMessage);
                            }
                            // If can't send to target in broadcast mode, ignore the problem
                            catch (StatusRuntimeException sre) {
                                log.error("Target {} not reachable", uuid);
                            }
                        }
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
        ClientSpec doneSignal = ClientSpec.newBuilder()
                .setUuid("-1")
                .build();
        responseObserver.onNext(doneSignal);
        for(StreamObserver<ClientSpec> obs: clientStreamList) {
            try {
                obs.onNext(request);
                obs.onNext(doneSignal);
            }
            catch (StatusRuntimeException sre) {
                log.error("Can't inform new targets because stream closed");
            }
        }
        if (!clientMap.containsKey(request.getUuid()))
            clientStreamList.add(responseObserver);
    }
}