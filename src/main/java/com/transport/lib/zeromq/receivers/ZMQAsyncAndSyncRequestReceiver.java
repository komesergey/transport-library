package com.transport.lib.zeromq.receivers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.transport.lib.entities.Command;
import com.transport.lib.entities.RequestContext;
import com.transport.lib.exception.TransportExecutionException;
import com.transport.lib.exception.TransportSystemException;
import com.transport.lib.zookeeper.Utils;
import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import zmq.ZError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.transport.lib.TransportService.*;

/*
    Class responsible for receiving synchronous and asynchronous requests using ZeroMQ
 */
@Slf4j
public class ZMQAsyncAndSyncRequestReceiver implements Runnable, Closeable {

    // ZeroMQ async requests are processed by 3 receiver threads
    private static final ExecutorService service = Executors.newFixedThreadPool(3);

    private ZMQ.Context context;
    private ZMQ.Socket socket;

    @Override
    public void run() {

        try {
            context = ZMQ.context(10);
            socket = context.socket(SocketType.REP);
            socket.bind("tcp://" + Utils.getZeroMQBindAddress());
        } catch (UnknownHostException zmqStartupException) {
            log.error("Error during ZeroMQ request receiver startup:", zmqStartupException);
            throw new TransportSystemException(zmqStartupException);
        }

        // New Kryo instance per thread
        Kryo kryo = new Kryo();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Receiver raw bytes
                byte[] bytes = socket.recv();
                // Unmarshal message to Command object
                Input input = new Input(new ByteArrayInputStream(bytes));
                final Command command = kryo.readObject(input, Command.class);
                // If it is async request - answer with "OK" message before target method invocation
                if (command.getCallbackKey() != null && command.getCallbackClass() != null) {
                    socket.send("OK");
                }
                // If it is async request - start target method invocation in separate thread
                if (command.getCallbackKey() != null && command.getCallbackClass() != null) {
                    Runnable runnable = () -> {
                        try {
                            // Target method will be executed in current Thread, so set service metadata
                            // like client's module.id and SecurityTicket token in ThreadLocal variables
                            RequestContext.setSourceModuleId(command.getSourceModuleId());
                            RequestContext.setSecurityTicket(command.getTicket());
                            // Invoke target method and receive result
                            Object result = invoke(command);
                            // Marshall result as CallbackContainer
                            ByteArrayOutputStream bOutput = new ByteArrayOutputStream();
                            Output output = new Output(bOutput);
                            // Construct CallbackContainer and marshall it to output stream
                            kryo.writeObject(output, constructCallbackContainer(command, result));
                            output.close();
                            // Connect to client
                            ZMQ.Socket socketAsync = context.socket(SocketType.REQ);
                            socketAsync.connect("tcp://" + command.getCallBackZMQ());
                            // And send response
                            socketAsync.send(bOutput.toByteArray());
                            socketAsync.close();
                        } catch (ClassNotFoundException | NoSuchMethodException e) {
                            log.error("Error while receiving async request");
                            throw new TransportExecutionException(e);
                        }
                    };
                    service.execute(runnable);
                } else {
                    // Target method will be executed in current Thread, so set service metadata
                    // like client's module.id and SecurityTicket token in ThreadLocal variables
                    RequestContext.setSourceModuleId(command.getSourceModuleId());
                    RequestContext.setSecurityTicket(command.getTicket());
                    // Invoke target method and receive result
                    Object result = invoke(command);
                    ByteArrayOutputStream bOutput = new ByteArrayOutputStream();
                    Output output = new Output(bOutput);
                    // Marshall result
                    kryo.writeClassAndObject(output, getResult(result));
                    output.close();
                    // Send result back to client
                    socket.send(bOutput.toByteArray());
                }
            } catch (ZMQException | ZError.IOException recvTerminationException) {
                if (!recvTerminationException.getMessage().contains("156384765")) {
                    log.error("General ZMQ exception", recvTerminationException);
                    throw new TransportSystemException(recvTerminationException);
                }
            }
        }
        log.info("{} terminated", this.getClass().getSimpleName());
    }

    @Override
    public void close() {
        Utils.closeSocketAndContext(socket, context);
        service.shutdownNow();
    }
}
