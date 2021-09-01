package com.example.sandbox.rabbitmq.rpc;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class RPCServer {

    private static final String EXCHANGE_NAME = "rcp_task";

    private static int fib(int n) {
        return switch (n) {
            case 0 -> 0;
            case 1 -> 1;
            default -> fib(n-1) + fib(n-2);
        };
    }

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try(Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {
            channel.queueDeclare(EXCHANGE_NAME, false, false, false, null);
            channel.queuePurge(EXCHANGE_NAME);

            channel.basicQos(1);
            System.out.println(" [*] Awaiting RCP requests...");

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().correlationId(delivery.getProperties().getCorrelationId()).build();

                String response = "";

                try {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    int n = Integer.parseInt(message);

                    System.out.println(" [.] fib(" + n + ")");
                    response += fib(n);
                } catch (RuntimeException exception) {
                    System.out.println(" [.] " + exception);
                } finally {
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes(StandardCharsets.UTF_8));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };

            channel.basicConsume(EXCHANGE_NAME, false, deliverCallback, consumerTag -> {});
            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Something went wrong...");
        }
    }
}
