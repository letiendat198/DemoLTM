package com.grpc.sec01;

import com.grpc.models.buffer.ChatMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.time.Instant;
import java.util.Scanner;

//mvn exec:java -Dexec.mainClass="com.grpc.sec01.GrpcChatClient"
public class GrpcChatClient {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 6565)
                .usePlaintext()
                .build();

        com.grpc.models.buffer.ChatServiceGrpc.ChatServiceStub stub = com.grpc.models.buffer.ChatServiceGrpc.newStub(channel);

        StreamObserver<com.grpc.models.buffer.ChatMessage> requestObserver = stub.chat(new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage value) {
                System.out.println(value.getUser() + ": " + value.getMessage());
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

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String user = scanner.nextLine();

        System.out.println("Type your message (type 'exit' to quit):");
        while (true) {
            String msg = scanner.nextLine();
            if ("exit".equalsIgnoreCase(msg)) {
                requestObserver.onCompleted();
                break;
            }
            ChatMessage chatMessage = ChatMessage.newBuilder()
                    .setUser(user)
                    .setMessage(msg)
                    .setTimestamp(Instant.now().getEpochSecond())
                    .build();
            requestObserver.onNext(chatMessage);
        }

        channel.shutdown();
    }
}