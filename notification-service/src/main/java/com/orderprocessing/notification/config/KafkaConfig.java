package com.orderprocessing.notification.config;

import com.orderprocessing.notification.event.OrderCompletedEvent;
import com.orderprocessing.notification.event.PaymentFailedEvent;
import com.orderprocessing.notification.event.PaymentProcessedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    @Bean
    public ConsumerFactory<String, PaymentProcessedEvent> paymentProcessedConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(baseConsumerProps(),
            new StringDeserializer(),
            new JsonDeserializer<>(PaymentProcessedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentProcessedEvent>
    paymentProcessedListenerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PaymentProcessedEvent>();
        factory.setConsumerFactory(paymentProcessedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, PaymentFailedEvent> paymentFailedConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(baseConsumerProps(),
            new StringDeserializer(),
            new JsonDeserializer<>(PaymentFailedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentFailedEvent>
    paymentFailedListenerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PaymentFailedEvent>();
        factory.setConsumerFactory(paymentFailedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> orderCompletedConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(baseConsumerProps(),
            new StringDeserializer(),
            new JsonDeserializer<>(OrderCompletedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent>
    orderCompletedListenerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent>();
        factory.setConsumerFactory(orderCompletedConsumerFactory());
        return factory;
    }
}
