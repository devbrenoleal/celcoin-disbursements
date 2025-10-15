package com.celcoin.disbursement.gateway;

public interface EventPublisher {
    void publish(String topic, Object payload);
}