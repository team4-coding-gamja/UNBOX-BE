package com.example.unbox_payment.payment.test;

import com.example.unbox_payment.payment.domain.entity.Payment;
import com.example.unbox_payment.payment.domain.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentService {

        private final AdminPaymentRepository adminPaymentRepository;

        public List<PaymentCoverageDto> getPaymentsForCoverageTest(long offset, int limit) {
                return adminPaymentRepository.findPaymentsForCoverageTest(PaymentStatus.DONE, offset, limit);
        }

        public List<String> explainPaymentsForCoverageTest(long offset, int limit) {
                return adminPaymentRepository.explainPaymentsForCoverageTest(PaymentStatus.DONE, offset, limit);
        }

        public Page<AdminPaymentResponse> getPaymentsV1(
                        AdminPaymentSearchCondition condition,
                        Pageable pageable) {
                return adminPaymentRepository.searchAdminPayments(PaymentStatus.DONE, pageable)
                                .map(this::toResponse);
        }

        public Page<AdminPaymentResponse> getPaymentsV2(
                        AdminPaymentSearchCondition condition,
                        Pageable pageable) {
                return adminPaymentRepository.searchAdminPayments(PaymentStatus.DONE, pageable)
                                .map(this::toResponse);
        }

        public Page<AdminPaymentResponse> getPaymentsV3(
                        AdminPaymentSearchCondition condition,
                        Pageable pageable) {
                return adminPaymentRepository.searchAdminPayments(PaymentStatus.DONE, pageable)
                                .map(this::toResponse);
        }

        private AdminPaymentResponse toResponse(Payment payment) {
                return new AdminPaymentResponse(
                                payment.getId(),
                                payment.getOrderId(),
                                payment.getBuyerId(),
                                payment.getSellerId(),
                                payment.getAmount(),
                                payment.getMethod() == null ? null : payment.getMethod().name(),
                                payment.getStatus() == null ? null : payment.getStatus().name(),
                                payment.getCreatedAt(),
                                payment.getApprovedAt());
        }
}
