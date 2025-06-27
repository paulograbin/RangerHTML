package com.paulograbin.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeoutException;

public class Receiver {

    private static final Logger LOG = LoggerFactory.getLogger(Receiver.class);


    public void receiveMessage() throws IOException, TimeoutException {
        LOG.info("Received waking up at " + new Date());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(Sender.QUEUE_NAME, false, false, false, null);


        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String s = new String(delivery.getBody(), StandardCharsets.UTF_8);
            LOG.info("Message received: " + s);
        };

        channel.basicConsume(Sender.QUEUE_NAME, true, deliverCallback, consumerTag -> {
        });
    }

}
