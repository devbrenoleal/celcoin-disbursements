package com.celcoin.disbursement.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    public static final String PIX_REQUEST_TOPIC = "disbursement-requests-pix";
    public static final String TED_REQUEST_TOPIC = "disbursement-requests-ted";
    public static final String TED_RESPONSE_TOPIC = "disbursement-responses-ted";
    public static final String DEAD_LETTER_TOPIC = "disbursement-requests.DLT";

    @Bean
    public NewTopic pixRequestTopic() {
        return TopicBuilder.name(PIX_REQUEST_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tedRequestTopic() {
        return TopicBuilder.name(TED_REQUEST_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tedResponseTopic() {
        return TopicBuilder.name(TED_RESPONSE_TOPIC).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name(DEAD_LETTER_TOPIC).partitions(1).replicas(1).build();
    }
}
