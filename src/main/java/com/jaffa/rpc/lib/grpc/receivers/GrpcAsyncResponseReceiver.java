package com.jaffa.rpc.lib.grpc.receivers;

import com.jaffa.rpc.grpc.services.CallbackRequest;
import com.jaffa.rpc.grpc.services.CallbackResponse;
import com.jaffa.rpc.grpc.services.CallbackServiceGrpc;
import com.jaffa.rpc.lib.common.RequestInvoker;
import com.jaffa.rpc.lib.entities.CallbackContainer;
import com.jaffa.rpc.lib.exception.JaffaRpcExecutionException;
import com.jaffa.rpc.lib.exception.JaffaRpcSystemException;
import com.jaffa.rpc.lib.grpc.Converters;
import com.jaffa.rpc.lib.zookeeper.Utils;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class GrpcAsyncResponseReceiver implements Runnable, Closeable {

    private Server server;

    private static final ExecutorService requestService = Executors.newFixedThreadPool(3);

    @Override
    public void run() {
        try {
            server = ServerBuilder
                    .forPort(Utils.getCallbackPort())
                    .executor(requestService)
                    .addService(new CallbackServiceImpl()).build();
            server.start();
            server.awaitTermination();
        } catch (InterruptedException | IOException zmqStartupException) {
            log.error("Error during gRPC async response receiver startup:", zmqStartupException);
            throw new JaffaRpcSystemException(zmqStartupException);
        }
        log.info("{} terminated", this.getClass().getSimpleName());
    }

    @Override
    public void close() {
        server.shutdown();
        requestService.shutdown();
        log.info("gRPC async response receiver stopped");
    }

    private static class CallbackServiceImpl extends CallbackServiceGrpc.CallbackServiceImplBase {
        @Override
        public void execute(CallbackRequest request, StreamObserver<CallbackResponse> responseObserver) {
            try {
                CallbackContainer callbackContainer = Converters.fromGRPCCallbackRequest(request);
                RequestInvoker.processCallbackContainer(callbackContainer);
                responseObserver.onNext(CallbackResponse.newBuilder().setResponse("OK").build());
                responseObserver.onCompleted();
            } catch (Exception exception) {
                log.error("gRPC callback execution exception", exception);
                throw new JaffaRpcExecutionException(exception);
            }
        }
    }
}