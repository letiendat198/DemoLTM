package com.grpc.server;

import com.grpc.models.buffer.ChatMessage;
import com.grpc.models.buffer.ClientSpec;
import com.grpc.models.buffer.ChatServiceGrpc;
import com.grpc.models.buffer.MESSAGE_TYPE;
import io.grpc.Status;
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
    Map<String, StreamObserver<ClientSpec>> clientStreamMap = new HashMap<>();

    // Mysterious null pointer bug is probably because a client trying to send Unicast
    // to a target that wasn't init-ed, thus not found in streamMap
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
                            if (streamMap.containsKey(chatMessage.getTargetUUID()))
                                streamMap.get(chatMessage.getTargetUUID()).onNext(chatMessage);
                            else {
                                log.error("Target not found in cache: {}. Probably not initiated", chatMessage.getTargetUUID());
                                throw new StatusRuntimeException(Status.CANCELLED);
                            }
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
        ClientSpec doneSignal = ClientSpec.newBuilder()
                .setUuid("-1")
                .build();

        // Inform the request of all targets. Skip if same uuid in case of re-register
        for (String key: clientMap.keySet()) {
            if (!request.getUuid().equals(key)) responseObserver.onNext(clientMap.get(key));
        }
        responseObserver.onNext(doneSignal);


        // Inform all existing stream of new target, using the request data
        for(String key: clientStreamMap.keySet()) {
            try {
                // Skip the requester so it doesn't get it's own info
                if (!key.equals(request.getUuid())) {
                    clientStreamMap.get(key).onNext(request);
                    clientStreamMap.get(key).onNext(doneSignal);
                }

            }
            catch (StatusRuntimeException sre) {
                log.error("Can't inform new targets because stream closed");
            }
        }
        // Add new target to observer list
        clientStreamMap.put(request.getUuid(), responseObserver);

        // Write back new data to client map and stream list
        clientMap.put(request.getUuid(), request);
    }
}