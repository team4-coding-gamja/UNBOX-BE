package com.example.unbox_payment.payment.test;

import com.example.unbox_common.pagination.PageSizeLimiter;
import com.example.unbox_common.response.CustomApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payment")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;
    private final AdminPaymentDataGenerator adminPaymentDataGenerator;

    @PostMapping("/test/init-data")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    public CustomApiResponse<String> initData(@RequestParam(defaultValue = "100000") int count) {
        adminPaymentDataGenerator.generateData(count);
        return CustomApiResponse.success("Generated " + count + " payments.");
    }

    @GetMapping("/v1")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    public CustomApiResponse<Page<AdminPaymentResponse>> getPaymentsV1(
            @ModelAttribute AdminPaymentSearchCondition condition,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminPaymentResponse> responses = adminPaymentService.getPaymentsV1(condition, limited);
        return CustomApiResponse.success(responses);
    }

    @GetMapping("/v2")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    public CustomApiResponse<Page<AdminPaymentResponse>> getPaymentsV2(
            @ModelAttribute AdminPaymentSearchCondition condition,
            @PageableDefault(size = 20, sort = "approvedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminPaymentResponse> responses = adminPaymentService.getPaymentsV2(condition, limited);
        return CustomApiResponse.success(responses);
    }

    @GetMapping("/v3")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    public CustomApiResponse<Page<AdminPaymentResponse>> getPaymentsV3(
            @ModelAttribute AdminPaymentSearchCondition condition,
            @PageableDefault(size = 20, sort = "approvedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminPaymentResponse> responses = adminPaymentService.getPaymentsV3(condition, limited);
        return CustomApiResponse.success(responses);
    }

    @GetMapping("/test/explain")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    public CustomApiResponse<java.util.List<String>> explainPaymentsForCoverageTest(
            @RequestParam(defaultValue = "0") long offset,
            @RequestParam(defaultValue = "20") int limit) {
        return CustomApiResponse.success(adminPaymentService.explainPaymentsForCoverageTest(offset, limit));
    }
}
