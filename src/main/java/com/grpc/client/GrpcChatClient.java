package com.grpc.client;

import com.grpc.models.buffer.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//mvn exec:java -Dexec.mainClass="com.grpc.sec01.GrpcChatClient"
public class GrpcChatClient {
    StreamObserver<ChatMessage> requestObserver;
    ManagedChannel channel;
    ChatServiceGrpc.ChatServiceStub stub;

    IClientHandler handler;
    public String uuid;
    public ClientSpec thisClient;

    public GrpcChatClient() {
        uuid = UUID.randomUUID().toString();

        channel = ManagedChannelBuilder.forAddress("localhost", 6565)
                .usePlaintext()
                .build();

        stub = ChatServiceGrpc.newStub(channel);

        requestObserver = stub.chat(new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage value) {
                try {
                    if (handler != null) handler.onMessage(value);
                }
                catch (Exception e){
                    System.err.println(e.getMessage());
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Chat ended.");
            }
        });
    }

    // Register tell the server this machine preferred username and color
    // so that the other client can correctly display
    public void register(String username, String color) {
        ClientSpec spec = ClientSpec.newBuilder()
                .setUuid(uuid)
                .setUsername(username)
                .setColor(color).build();
        thisClient = spec;

        StreamObserver<ClientSpec> responseObserver = new StreamObserver<ClientSpec>() {
            boolean success = true;
            Map<String, ClientSpec> targetMap = new HashMap<>();
            @Override
            public void onNext(ClientSpec clientSpec) {
                targetMap.put(clientSpec.getUuid(), clientSpec);
            }

            @Override
            public void onError(Throwable throwable) {
                success = false;
                onCompleted();
            }

            @Override
            public void onCompleted() {
                handler.onRegisterComplete(success, targetMap);
            }
        };
        stub.register(spec, responseObserver);

        ChatMessage initMsg = ChatMessage.newBuilder()
                .setSourceUUID(thisClient.getUuid())
                .setType(MESSAGE_TYPE.MESSAGE_TYPE_INIT)
                .build();
        requestObserver.onNext(initMsg);
    }

    public void sendMessage(ChatMessage msg) {
        requestObserver.onNext(msg);
    }

    public void stop() {
        channel.shutdown();
    }

    public void setHandler(IClientHandler handler) {
        this.handler = handler;
    }
}