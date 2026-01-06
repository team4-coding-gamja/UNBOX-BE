package com.example.unbox_be.domain.payment.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_be.domain.payment.entity.Payment;
import com.example.unbox_be.domain.payment.entity.PaymentStatus;
import com.example.unbox_be.domain.payment.repository.PaymentRepository;
import com.example.unbox_be.domain.settlement.service.SettlementService;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @InjectMocks
    private PaymentService paymentService;

    @Mock private TossApiService tossApiService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentTransactionService paymentTransactionService;
    @Mock private SettlementService settlementService;

    private User createUser(Long id) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Order createOrder(UUID id, Long buyerId, Long price) {
        User buyer = createUser(buyerId);
        Order order = Order.builder()
                .price(BigDecimal.valueOf(price))
                .buyer(buyer)
                .build();
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    private Payment createPayment(UUID id, UUID orderId, Integer amount, PaymentStatus status) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .method("CARD")
                .status(status)
                .build();
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }

    @Test
    @DisplayName("결제 생성 성공 - 신규 결제 레코드 생성")
    void createPayment_Success() {
        // given
        Long userId = 1L;
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Order order = createOrder(orderId, userId, 10000L);

        given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId)).willReturn(Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> {
            Payment p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", paymentId);
            return p;
        });

        // when
        PaymentReadyResponseDto result = paymentService.createPayment(userId, orderId, "CARD");

        // then
        assertThat(result.paymentId()).isEqualTo(paymentId);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }



}
