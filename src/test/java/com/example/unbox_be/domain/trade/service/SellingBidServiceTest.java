package com.example.unbox_be.domain.trade.service;

import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SellingBidServiceTest {

    @Mock
    private SellingBidRepository sellingBidRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SellingBidService sellingBidService;

    @Test
    @DisplayName("시스템에 의한 상태 변경 성공 (이메일이 null인 경우 권한 체크 건너뜀)")
    void updateStatus_BySystem_Success() {
        // given
        UUID bidId = UUID.randomUUID();
        // SellingBid에도 빌더가 없다면 ReflectionTestUtils나 생성자를 사용하세요.
        SellingBid sellingBid = SellingBid.builder()
                .status(SellingStatus.LIVE)
                .build();

        given(sellingBidRepository.findById(bidId)).willReturn(Optional.of(sellingBid));

        // when
        sellingBidService.updateSellingBidStatus(bidId, SellingStatus.HOLD, null);

        // then
        assertEquals(SellingStatus.HOLD, sellingBid.getStatus());
    }

    @Test
    @DisplayName("사용자에 의한 상태 변경 성공 (본인 확인 통과)")
    void updateStatus_ByUser_Success() {
        // given
        UUID bidId = UUID.randomUUID();
        Long userId = 1L;
        String email = "test@example.com";

        // User.createUser() 사용
        User user = User.createUser(email, "password123!", "nickname1", "01012345678");
        // private 필드인 id에 강제로 값 주입
        ReflectionTestUtils.setField(user, "id", userId);

        SellingBid sellingBid = SellingBid.builder()
                .userId(userId)
                .status(SellingStatus.LIVE)
                .build();

        given(sellingBidRepository.findById(bidId)).willReturn(Optional.of(sellingBid));
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        sellingBidService.updateSellingBidStatus(bidId, SellingStatus.CANCELLED, email);

        // then
        assertEquals(SellingStatus.CANCELLED, sellingBid.getStatus());
    }

    @Test
    @DisplayName("권한 없는 사용자가 변경 시도 시 ACCESS_DENIED 예외 발생")
    void updateStatus_AccessDenied() {
        // given
        UUID bidId = UUID.randomUUID();
        String hackerEmail = "hacker@example.com";

        User hacker = User.createUser(hackerEmail, "password123!", "hacker12", "01099998888");
        ReflectionTestUtils.setField(hacker, "id", 999L); // 해커 ID는 999

        SellingBid sellingBid = SellingBid.builder()
                .userId(1L) // 실제 주인 ID는 1
                .status(SellingStatus.LIVE)
                .build();

        given(sellingBidRepository.findById(bidId)).willReturn(Optional.of(sellingBid));
        given(userRepository.findByEmail(hackerEmail)).willReturn(Optional.of(hacker));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            sellingBidService.updateSellingBidStatus(bidId, SellingStatus.CANCELLED, hackerEmail);
        });

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 MATCHED인 상태를 LIVE로 변경 시도 시 INVALID_INPUT_VALUE 예외 발생")
    void updateStatus_InvalidTransition() {
        // given
        UUID bidId = UUID.randomUUID();
        SellingBid sellingBid = SellingBid.builder()
                .status(SellingStatus.MATCHED)
                .build();

        given(sellingBidRepository.findById(bidId)).willReturn(Optional.of(sellingBid));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            sellingBidService.updateSellingBidStatus(bidId, SellingStatus.LIVE, null);
        });

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}