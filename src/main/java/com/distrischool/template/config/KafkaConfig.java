package com.distrischool.template.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka topics configuration for notifications service.
 * Topics will be created automatically if they don't exist.
 */
@Slf4j
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @PostConstruct
    public void init() {
        log.info("KafkaConfig initialized with bootstrap-servers: {}, group-id: {}", bootstrapServers, groupId);
    }

    /**
     * Consumer factory configured to deserialize events as Map.
     * This allows accepting different event types from different services.
     */
    @Bean
    public ConsumerFactory<String, Map<String, Object>> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Use 'latest' to skip old compressed messages and only consume new ones
        // Change to 'earliest' if you need to process historical messages (after fixing Snappy support)
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        
        // JsonDeserializer configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
        
        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            new JsonDeserializer<>(Map.class, false) // false = não usar type info
        );
    }

    /**
     * Kafka listener container factory.
     * Configured with error handling to ensure consumers don't fail silently.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Custom error handler that logs errors but continues processing
        // Uses SeekToCurrentErrorHandler pattern to skip problematic messages
        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
            log.error("Failed to process Kafka message from topic '{}', partition {}, offset {}: {}", 
                record.topic(), record.partition(), record.offset(), exception.getMessage());
            // Skip problematic messages and continue
        }, new FixedBackOff(1000L, 3L));
        
        factory.setCommonErrorHandler(errorHandler);
        
        // Enable auto-commit to ensure offsets are committed
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        
        // Log consumer lifecycle events
        factory.setConcurrency(1); // Start with 1 consumer, can be increased if needed
        
        return factory;
    }
}

