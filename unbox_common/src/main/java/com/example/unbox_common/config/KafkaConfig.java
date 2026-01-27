package com.example.unbox_common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka 설정
 * 
 * ProducerFactory, ConsumerFactory, KafkaTemplate은 Spring Boot Auto Configuration이
 * application.yml 기반으로 자동 생성합니다.
 * 
 * MSK Serverless 인증 설정은 application-prod.yml의 spring.kafka.properties.*에서 관리:
 * - security.protocol: SASL_SSL
 * - sasl.mechanism: AWS_MSK_IAM
 * - sasl.jaas.config: ...
 */
@Configuration
public class KafkaConfig {

    /**
     * Consumer 리스너 컨테이너 팩토리
     * - Manual Immediate Ack Mode (데이터 정합성 최우선)
     * - Error Handling & Recovery (Retry + DLT)
     * 
     * @param consumerFactory Spring Boot가 자동 생성한 ConsumerFactory (application.yml 설정 적용됨)
     * @param kafkaTemplate Spring Boot가 자동 생성한 KafkaTemplate (application.yml 설정 적용됨)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        
        // Manual Immediate Ack Mode (데이터 정합성 최우선)
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Error Handling & Recovery (Retry + DLT)
        // 1초 간격, 최대 3회 재시도 (FixedBackOff)
        CommonErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), // 최종 실패 시 DLT로 발행
                new FixedBackOff(1000L, 3) // 1초 간격, 3회 시도
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}

