package com.transport.lib.zeromq;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import kafka.admin.RackAwareMode;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.reflections.Reflections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.UUID;

import static com.transport.lib.zeromq.ZeroRPCService.*;

@SuppressWarnings("WeakerAccess, unchecked")
public class KafkaSyncRequestReceiver implements Runnable {

    public static volatile boolean active = true;
    private static final HashSet<String> serverTopics = new HashSet<>();
    private static final ArrayList<Thread> serverConsumers = new ArrayList<>(3);

    @Override
    public void run() {
         Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", getOption("bootstrap.servers"));
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        consumerProps.put("enable.auto.commit", "false");
        consumerProps.put("group.id", UUID.randomUUID().toString());

        new Reflections(getOption("service.root")).getTypesAnnotatedWith(Api.class).forEach(x -> {if(x.isInterface()) serverTopics.add(x.getName() + "-" + getOption("module.id") + "-server-sync");});
        Properties topicConfig = new Properties();
        serverTopics.forEach(topic -> {
            if(!zkClient.topicExists(topic)){
                adminZkClient.createTopic(topic,3,1,topicConfig,RackAwareMode.Disabled$.MODULE$);
            }
        });

        Runnable consumerThread = () ->  {
            KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(consumerProps);
            KafkaProducer<String,byte[]> producer = new KafkaProducer<>(producerProps);
            consumer.subscribe(serverTopics);
            while(active){
                ConsumerRecords<String, byte[]> records = consumer.poll(10);
                for(ConsumerRecord<String,byte[]> record: records){
                    try {
                        Kryo kryo = new Kryo();
                        Input input = new Input(new ByteArrayInputStream(record.value()));
                        Command command = kryo.readObject(input, Command.class);
                        Object result = invoke(command);
                        ByteArrayOutputStream bOutput = new ByteArrayOutputStream();
                        Output output = new Output(bOutput);
                        kryo.writeClassAndObject(output, getResult(result));
                        output.close();
                        ProducerRecord<String,byte[]> resultPackage = new ProducerRecord<>(command.getServiceClass().replace("Transport", "") + "-" + getOption("module.id") + "-client-sync", command.getRqUid(), bOutput.toByteArray());
                        producer.send(resultPackage).get();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                try {
                    consumer.commitSync();
                }catch (CommitFailedException e){
                    e.printStackTrace();
                }
            }
        };
        serverConsumers.add(new Thread(consumerThread));
        serverConsumers.add(new Thread(consumerThread));
        serverConsumers.add(new Thread(consumerThread));
        serverConsumers.forEach(Thread::start);
        serverConsumers.forEach(x -> {try{x.join();} catch (Exception e){e.printStackTrace();}});
    }
}
