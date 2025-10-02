package com.grpc.server;

public class ServerMain {
    public static void main(String[] args) {
        GrpcServer.create(new ChatServiceImpl())
                .start()
                .await();
    }
}
