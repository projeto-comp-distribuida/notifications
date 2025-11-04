package com.distrischool.notifications;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Notifications Service Application
 * 
 * This service consumes Kafka events and provides REST endpoints for notifications.
 * 
 * @EnableFeignClients - Habilita comunicação com outros microserviços via Feign
 * @EnableKafka - Habilita integração com Apache Kafka
 * @ComponentScan - Inclui o pacote template para escanear componentes Kafka
 */
@SpringBootApplication
@EnableFeignClients
@EnableKafka
@ComponentScan(basePackages = {"com.distrischool.notifications", "com.distrischool.template"})
public class NotificationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationsApplication.class, args);
    }
}


