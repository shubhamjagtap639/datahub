package com.linkedin.metadata.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;

@SpringBootApplication(exclude = {RestClientAutoConfiguration.class, KafkaAutoConfiguration.class})
public class MceConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MceConsumerApplication.class, args);
    }

}
