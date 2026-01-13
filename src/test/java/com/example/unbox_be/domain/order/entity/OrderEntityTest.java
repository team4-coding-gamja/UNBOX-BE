package com.example.unbox_be.domain.order.entity;

import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderEntityTest {

    private User createUser(Long id) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Order createOrder(User buyer, User seller) {
        return Order.builder()
                .sellingBidId(UUID.randomUUID())
                .buyer(buyer)
                .seller(seller)
                .productOptionId(UUID.randomUUID()) // Mock ID
                .productId(UUID.randomUUID())       // Mock ID
                .productName("Product Name")        // Snapshot
                .modelNumber("Model-123")           // Snapshot
                .optionName("Option-1")             // Snapshot
                .imageUrl("http://image.url")       // Snapshot
                .brandName("Brand Name")            // Snapshot
                .price(BigDecimal.valueOf(10000))
                .receiverName("name")
                .receiverPhone("phone")
                .receiverAddress("addr")
                .receiverZipCode("zip")
                .build();
    }

    // --- Cancel Tests ---
    @Test
    @DisplayName("주문 취소 성공")
    void cancel_Success() {
        User buyer = createUser(1L);
        Order order = createOrder(buyer, createUser(2L));
        order.cancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 취소 실패 - 이미 배송 중")
    void cancel_Fail_Shipped() {
        Order order = createOrder(createUser(1L), createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_CENTER);

        assertThatThrownBy(order::cancel)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_CANNOT_BE_CANCELLED);
    }

    // --- RegisterTracking Tests ---
    @Test
    @DisplayName("운송장 등록 성공")
    void registerTracking_Success() {
        Order order = createOrder(createUser(1L), createUser(2L));
        order.registerTracking("1234");

        assertThat(order.getTrackingNumber()).isEqualTo("1234");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED_TO_CENTER);
    }

    @Test
    @DisplayName("운송장 등록 실패 - 상태 불일치")
    void registerTracking_Fail_InvalidStatus() {
        Order order = createOrder(createUser(1L), createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.CANCELLED); // 이미 취소됨

        assertThatThrownBy(() -> order.registerTracking("1234"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }

    // --- AdminStatus Tests ---
    @Test
    @DisplayName("관리자 상태 변경 성공 (순차적)")
    void updateAdminStatus_Success() {
        Order order = createOrder(createUser(1L), createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_CENTER);

        // SHIPPED_TO_CENTER -> ARRIVED_AT_CENTER
        order.updateAdminStatus(OrderStatus.ARRIVED_AT_CENTER, null);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ARRIVED_AT_CENTER);
    }

    @Test
    @DisplayName("관리자 상태 변경 성공 - 배송 시작 시 운송장 필수")
    void updateAdminStatus_ShippedToBuyer() {
        Order order = createOrder(createUser(1L), createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.INSPECTION_PASSED);

        // 정상 케이스
        order.updateAdminStatus(OrderStatus.SHIPPED_TO_BUYER, "final-123");
        assertThat(order.getFinalTrackingNumber()).isEqualTo("final-123");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED_TO_BUYER);
    }

    @Test
    @DisplayName("관리자 상태 변경 실패 - 운송장 누락")
    void updateAdminStatus_Fail_NoTracking() {
        Order order = createOrder(createUser(1L), createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.INSPECTION_PASSED);

        assertThatThrownBy(() -> order.updateAdminStatus(OrderStatus.SHIPPED_TO_BUYER, ""))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRACKING_NUMBER_REQUIRED);
    }

    @Test
    @DisplayName("관리자 상태 변경 실패 - 순서 건너뛰기")
    void updateAdminStatus_Fail_InvalidTransition() {
        Order order = createOrder(createUser(1L), createUser(2L));
        // PENDING_SHIPMENT -> DELIVERED (불가능)

        assertThatThrownBy(() -> order.updateAdminStatus(OrderStatus.DELIVERED, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
    }

    // --- Confirm Tests ---
    @Test
    @DisplayName("구매 확정 성공")
    void confirm_Success() {
        User buyer = createUser(1L);
        Order order = createOrder(buyer, createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.DELIVERED);

        order.confirm(buyer);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("구매 확정 실패 - 아직 배송 중")
    void confirm_Fail_NotDelivered() {
        User buyer = createUser(1L);
        Order order = createOrder(buyer, createUser(2L));
        ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_BUYER);

        assertThatThrownBy(() -> order.confirm(buyer))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
    }
}