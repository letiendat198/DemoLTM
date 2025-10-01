package com.grpc.sec01;


import com.grpc.models.buffer.ChatMessage;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServiceImpl extends com.grpc.models.buffer.ChatServiceGrpc.ChatServiceImplBase {

    private final List<StreamObserver<com.grpc.models.buffer.ChatMessage>> observers = new CopyOnWriteArrayList<>();

    @Override
    public StreamObserver<com.grpc.models.buffer.ChatMessage> chat(StreamObserver<com.grpc.models.buffer.ChatMessage> responseObserver) {
        observers.add(responseObserver);

        return new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage chatMessage) {
                for (StreamObserver<ChatMessage> observer : observers) {
                    observer.onNext(chatMessage);
                }
            }

            @Override
            public void onError(Throwable t) {
                observers.remove(responseObserver);
            }

            @Override
            public void onCompleted() {
                observers.remove(responseObserver);
                responseObserver.onCompleted();
            }
        };
    }
}