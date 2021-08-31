package com.example.sandbox.rabbitmq.topics;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EmitLogTopics {

    private static final String EXCHANGE_NAME = "log_topics";

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "topic");

            channel.basicPublish(EXCHANGE_NAME, args[0], null, Arrays.stream(args).skip(1).collect(Collectors.joining(" ")).getBytes(StandardCharsets.UTF_8));
        } catch (Exception exc) {
            System.out.println("Something went wrong...");
        }
    }
}
