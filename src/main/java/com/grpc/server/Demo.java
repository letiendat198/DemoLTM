package com.grpc.server;


import com.grpc.sec01.ChatServiceImpl;

public class Demo {
    public static void main(String[] args) {
        GrpcServer.create(new ChatServiceImpl())
                .start()
                .await();
    }
}
