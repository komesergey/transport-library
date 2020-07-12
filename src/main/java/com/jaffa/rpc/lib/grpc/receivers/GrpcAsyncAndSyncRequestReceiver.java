package com.jaffa.rpc.lib.grpc.receivers;

import com.google.protobuf.ByteString;
import com.jaffa.rpc.grpc.services.*;
import com.jaffa.rpc.lib.common.RequestInvoker;
import com.jaffa.rpc.lib.entities.Command;
import com.jaffa.rpc.lib.exception.JaffaRpcExecutionException;
import com.jaffa.rpc.lib.exception.JaffaRpcSystemException;
import com.jaffa.rpc.lib.grpc.Converters;
import com.jaffa.rpc.lib.zookeeper.Utils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class GrpcAsyncAndSyncRequestReceiver implements Runnable, Closeable {

    private static final ExecutorService asyncService = Executors.newFixedThreadPool(3);
    private static final ExecutorService requestService = Executors.newFixedThreadPool(3);

    private Server server;

    @Override
    public void run() {
        try {
            server = ServerBuilder
                    .forPort(Utils.getServicePort())
                    .executor(requestService)
                    .addService(new CommandServiceImpl()).build();
            server.start();
            server.awaitTermination();
        } catch (InterruptedException | IOException zmqStartupException) {
            log.error("Error during gRPC request receiver startup:", zmqStartupException);
            throw new JaffaRpcSystemException(zmqStartupException);
        }
        log.info("{} terminated", this.getClass().getSimpleName());
    }

    private static class CommandServiceImpl extends CommandServiceGrpc.CommandServiceImplBase {

        @Override
        public void execute(CommandRequest request, StreamObserver<CommandResponse> responseObserver) {
            try {
                final Command command = Converters.fromGRPCCommandRequest(request);
                if (StringUtils.isNotBlank(command.getCallbackKey()) && StringUtils.isNotBlank(command.getCallbackClass())) {
                    Runnable runnable = () -> {
                        try {
                            Object result = RequestInvoker.invoke(command);
                            CallbackRequest callbackResponse = Converters.toGRPCCallbackRequest(RequestInvoker.constructCallbackContainer(command, result));
                            String[] hostAndPort = command.getCallBackHost().split(":");
                            ManagedChannel channel = ManagedChannelBuilder.forAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])).usePlaintext().build();
                            CallbackServiceGrpc.CallbackServiceBlockingStub stub = CallbackServiceGrpc.newBlockingStub(channel);
                            stub.execute(callbackResponse);
                            channel.shutdown();
                        } catch (ClassNotFoundException | NoSuchMethodException e) {
                            log.error("Error while receiving async request", e);
                            throw new JaffaRpcExecutionException(e);
                        }
                    };
                    asyncService.execute(runnable);
                    responseObserver.onNext(CommandResponse.newBuilder().setResponse(ByteString.EMPTY).build());
                } else {
                    Object result = RequestInvoker.invoke(command);
                    CommandResponse commandResponse = Converters.toGRPCCommandResponse(RequestInvoker.getResult(result));
                    responseObserver.onNext(commandResponse);
                }
                responseObserver.onCompleted();
            } catch (ClassNotFoundException exception) {
                log.error("Error while receiving request ", exception);
                throw new JaffaRpcSystemException(exception);
            }
        }
    }

    @Override
    public void close() {
        server.shutdown();
        asyncService.shutdown();
        requestService.shutdown();
    }
}