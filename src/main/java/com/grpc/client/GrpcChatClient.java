package com.grpc.client;

import com.grpc.models.buffer.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//mvn exec:java -Dexec.mainClass="com.grpc.sec01.GrpcChatClient"
public class GrpcChatClient {
    StreamObserver<ChatMessage> chatRequestObserver;
    StreamObserver<ClientSpec> specResponseObserver;
    ManagedChannel channel;
    ChatServiceGrpc.ChatServiceStub stub;

    IClientHandler handler;
    static String uuid = UUID.randomUUID().toString();;
    static String username = null;
    static String color = null;
    public ClientSpec thisClient;

    public GrpcChatClient() throws UnknownHostException {
        if (username == null) username = InetAddress.getLocalHost().getHostName();
        if (color == null) color = "blue";

        channel = ManagedChannelBuilder.forAddress("localhost", 6565)
                .usePlaintext()
                .build();

        stub = ChatServiceGrpc.newStub(channel);

        chatRequestObserver = stub.chat(new StreamObserver<ChatMessage>() {
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
                System.err.println("Char error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Chat ended.");
            }
        });
    }

    // Register tell the server this machine preferred username and color
    // so that the other client can correctly display
    // TODO: Register won't re-initate a failed chat stub. Just re-construct this class and persist the uuid
    public void register() {
        ClientSpec spec = ClientSpec.newBuilder()
                .setUuid(uuid)
                .setUsername(username)
                .setColor(color).build();
        thisClient = spec;

        specResponseObserver = new StreamObserver<ClientSpec>() {
            boolean success = true;
            Map<String, ClientSpec> targetMap = new HashMap<>();
            @Override
            public void onNext(ClientSpec clientSpec) {
                // ALWAYS CATCH ERROR IN ON NEXT OTHERWISE THIS MFK OF A LIBRARY WILL SWALLOW THE FUCKING ERROR
                // AND GIVE A RANDOM ASS ERROR AND WASTE 1HR OF YOUR LIFE
                try {
                    if (clientSpec.getUuid().equals("-1")) {
                        System.out.println("New target found! Updating");
                        handler.onRegisterComplete(success, targetMap);
                    }
                    else targetMap.put(clientSpec.getUuid(), clientSpec);
                }
                catch (Exception e) {
                    System.out.println("Register onNext exception:" + e.getMessage());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println(throwable.getMessage());
                success = false;
                handler.onRegisterComplete(success, targetMap);
            }

            @Override
            public void onCompleted() {

            }
        };
        stub.register(spec, specResponseObserver);

        try {
            ChatMessage initMsg = ChatMessage.newBuilder()
                    .setSourceUUID(thisClient.getUuid())
                    .setType(MESSAGE_TYPE.MESSAGE_TYPE_INIT)
                    .build();
            chatRequestObserver.onNext(initMsg);
        }
        catch (Exception e) {
            System.err.println("Sending init message failed with exception: " + e.getMessage());
        }
    }

    public void sendMessage(ChatMessage msg) {
        chatRequestObserver.onNext(msg);
    }

    public void stop() {
        channel.shutdown();
    }

    public void setHandler(IClientHandler handler) {
        this.handler = handler;
    }

    public static void setUsername(String username) {
        GrpcChatClient.username = username;
    }

    public static void setColor(String color) {
        GrpcChatClient.color = color;
    }

    public static String getUsername() {
        return username;
    }

    public static String getColor() {
        return color;
    }
}