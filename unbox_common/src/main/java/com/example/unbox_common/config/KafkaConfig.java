package com.example.unbox_common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
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
 * 
 * 토픽 자동 생성:
 * - 애플리케이션 시작 시 NewTopic Bean들이 자동으로 토픽 생성
 * - 이미 존재하는 토픽은 스킵
 * - MSK Serverless에서 UNKNOWN_TOPIC_OR_PARTITION 경고 방지
 */
@Configuration
public class KafkaConfig {

    // =========================================================
    // Consumer/Producer 설정
    // =========================================================

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

    // =========================================================
    // 토픽 자동 생성 설정
    // =========================================================

    /**
     * Order Events 토픽
     * - 주문 생성, 취소, 환불 등 주문 관련 이벤트
     * - MSK Serverless는 레플리카를 자동으로 관리 (최소 2개)
     */
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name("order-events")
                .partitions(3)
                .build();
    }

    /**
     * Payment Events 토픽
     * - 결제 승인, 실패, 환불 등 결제 관련 이벤트
     * - MSK Serverless는 레플리카를 자동으로 관리 (최소 2개)
     */
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment-events")
                .partitions(3)
                .build();
    }

    /**
     * Product Events 토픽
     * - 상품 등록, 수정, 재고 변경 등 상품 관련 이벤트
     * - MSK Serverless는 레플리카를 자동으로 관리 (최소 2개)
     */
    @Bean
    public NewTopic productEventsTopic() {
        return TopicBuilder.name("product-events")
                .partitions(3)
                .build();
    }

    /**
     * Trade Events 토픽
     * - 거래 체결, 입찰 등 거래 관련 이벤트
     * - MSK Serverless는 레플리카를 자동으로 관리 (최소 2개)
     */
    @Bean
    public NewTopic tradeEventsTopic() {
        return TopicBuilder.name("trade-events")
                .partitions(3)
                .build();
    }
}

