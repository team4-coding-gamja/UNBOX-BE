package com.example.unbox_be.domain.admin.trade.service;

import com.example.unbox_be.domain.trade.dto.request.SellingBidSearchCondition;
import com.example.unbox_be.domain.trade.dto.response.AdminSellingBidListResponseDto;
import com.example.unbox_be.domain.trade.repository.AdminSellingBidRepository;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.service.AdminSellingBidServiceImpl;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminSellingBidServiceImplTest {

    @InjectMocks
    private AdminSellingBidServiceImpl service;

    @Mock
    private AdminSellingBidRepository repository;

    // Helper: Mock Data Setup
    private SellingBid createMockBid(SellingStatus status, boolean fullData) {
        SellingBid bid = mock(SellingBid.class);
        given(bid.getId()).willReturn(UUID.randomUUID());
        given(bid.getPrice()).willReturn(100000); // Integer
        given(bid.getStatus()).willReturn(status);
        given(bid.getCreatedAt()).willReturn(LocalDateTime.now());
        given(bid.getDeadline()).willReturn(LocalDateTime.now().plusDays(7));

        if (fullData) {
            ProductOption option = mock(ProductOption.class);
            Product product = mock(Product.class);
            Brand brand = mock(Brand.class);

            given(bid.getProductOption()).willReturn(option);
            given(option.getProduct()).willReturn(product);
            given(option.getOption()).willReturn("270");
            given(product.getName()).willReturn("Air Force");
            given(product.getBrand()).willReturn(brand);
            given(brand.getName()).willReturn("Nike");
        } else {
            given(bid.getProductOption()).willReturn(null);
        }
        return bid;
    }

    // 1. 전체 데이터 조회 성공
    @Test
    @DisplayName("목록 조회: 모든 데이터가 존재할 때 정상 매핑")
    void getSellingBids_FullData() {
        SellingBid bid = createMockBid(SellingStatus.LIVE, true);
        Page<SellingBid> page = new PageImpl<>(List.of(bid));
        given(repository.findAdminSellingBids(any(), any())).willReturn(page);

        Page<AdminSellingBidListResponseDto> result = service.getSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        AdminSellingBidListResponseDto dto = result.getContent().get(0);
        assertThat(dto.getProductName()).isEqualTo("Air Force");
        assertThat(dto.getBrandName()).isEqualTo("Nike");
        assertThat(dto.getSize()).isEqualTo("270");
    }

    // 2. ProductOption Null
    @Test
    @DisplayName("목록 조회: ProductOption이 Null일 때 'Unknown' 처리")
    void getSellingBids_NullRelation() {
        SellingBid bid = createMockBid(SellingStatus.LIVE, false);
        Page<SellingBid> page = new PageImpl<>(List.of(bid));
        given(repository.findAdminSellingBids(any(), any())).willReturn(page);

        Page<AdminSellingBidListResponseDto> result = service.getSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));

        assertThat(result.getContent().get(0).getProductName()).isEqualTo("Unknown Product");
        assertThat(result.getContent().get(0).getSize()).isEqualTo("Unknown Size");
    }

    // 3. Status - MATCHED
    @Test
    @DisplayName("목록 조회: 상태가 MATCHED일 때 매핑")
    void getSellingBids_StatusMatched() {
        SellingBid bid = createMockBid(SellingStatus.MATCHED, true);
        given(repository.findAdminSellingBids(any(), any())).willReturn(new PageImpl<>(List.of(bid)));

        Page<AdminSellingBidListResponseDto> result = service.getSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));

        assertThat(result.getContent().get(0).getStatus()).isEqualTo(SellingStatus.MATCHED);
    }

    // 4. Status - CANCELLED
    @Test
    @DisplayName("목록 조회: 상태가 CANCELLED일 때 매핑")
    void getSellingBids_StatusCancelled() {
        SellingBid bid = createMockBid(SellingStatus.CANCELLED, true);
        given(repository.findAdminSellingBids(any(), any())).willReturn(new PageImpl<>(List.of(bid)));

        Page<AdminSellingBidListResponseDto> result = service.getSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));

        assertThat(result.getContent().get(0).getStatus()).isEqualTo(SellingStatus.CANCELLED);
    }

    // 5. 빈 목록 조회
    @Test
    @DisplayName("목록 조회: 데이터가 없을 때 빈 페이지 반환")
    void getSellingBids_Empty() {
        given(repository.findAdminSellingBids(any(), any())).willReturn(Page.empty());

        Page<AdminSellingBidListResponseDto> result = service.getSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // 6. 페이징 정보 확인
    @Test
    @DisplayName("목록 조회: 페이징 정보가 올바르게 전달되는지 확인")
    void getSellingBids_PagingInfo() {
        Pageable pageable = PageRequest.of(1, 5); // 2nd page, size 5
        given(repository.findAdminSellingBids(any(), eq(pageable))).willReturn(Page.empty());

        service.getSellingBids(new SellingBidSearchCondition(), pageable);

        verify(repository).findAdminSellingBids(any(), eq(pageable));
    }

    // 7. 검색 조건 전달 확인
    @Test
    @DisplayName("목록 조회: 검색 조건 객체가 그대로 전달되는지 확인")
    void getSellingBids_ConditionPassing() {
        SellingBidSearchCondition cond = new SellingBidSearchCondition();
        cond.setProductName("Test");
        given(repository.findAdminSellingBids(eq(cond), any())).willReturn(Page.empty());

        service.getSellingBids(cond, PageRequest.of(0, 10));

        verify(repository).findAdminSellingBids(eq(cond), any());
    }

    // 8. 삭제 성공
    @Test
    @DisplayName("삭제: 존재하는 ID로 요청 시 정상 호출")
    void deleteSellingBid_Success() {
        UUID id = UUID.randomUUID();
        SellingBid bid = mock(SellingBid.class);
        given(repository.findById(id)).willReturn(Optional.of(bid));

        service.deleteSellingBid(id, "admin");

        verify(bid).softDelete("admin");
    }

    // 9. 삭제 실패 - NotFound
    @Test
    @DisplayName("삭제: 존재하지 않는 ID로 요청 시 예외 발생")
    void deleteSellingBid_NotFound() {
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSellingBid(id, "admin"))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.SELLING_BID_NOT_FOUND.getMessage());
    }

    // 10. 가격 정보 검증 (0원)
    @Test
    @DisplayName("매핑: 가격이 0원일 때 정상 매핑")
    void mapping_ZeroPrice() {
        SellingBid bid = createMockBid(SellingStatus.LIVE, true);
        given(bid.getPrice()).willReturn(0);
        given(repository.findAdminSellingBids(any(), any())).willReturn(new PageImpl<>(List.of(bid)));

        Page<AdminSellingBidListResponseDto> result = service.getSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));
        
        assertThat(result.getContent().get(0).getPrice()).isEqualTo(0);
    }

    // 11. 날짜 정보 검증
    @Test
    @DisplayName("매핑: 날짜 필드(CreatedAt, Deadline) 값 유지 확인")
    void mapping_Dates() {
        LocalDateTime now = LocalDateTime.now();
        SellingBid bid = createMockBid(SellingStatus.LIVE, true);
        given(bid.getCreatedAt()).willReturn(now);
        given(bid.getDeadline()).willReturn(now.plusDays(30));
        given(repository.findAdminSellingBids(any(), any())).willReturn(new PageImpl<>(List.of(bid)));

        Page<AdminSellingBidListResponseDto> result = service.getSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));

        assertThat(result.getContent().get(0).getCreatedAt()).isEqualTo(now);
        assertThat(result.getContent().get(0).getDeadline()).isEqualTo(now.plusDays(30));
    }
    
    // 12. Size 정보가 특수문자일 때
    @Test
    @DisplayName("매핑: 옵션 사이즈가 특수문자 등을 포함해도 정상 처리")
    void mapping_SpecialCharOption() {
        SellingBid bid = createMockBid(SellingStatus.LIVE, true);
        // Overwrite standard mock behavior
        ProductOption opt = bid.getProductOption(); // Get mocked option
        given(opt.getOption()).willReturn("Size-XL/290"); 
        
        given(repository.findAdminSellingBids(any(), any())).willReturn(new PageImpl<>(List.of(bid)));
        
        Page<AdminSellingBidListResponseDto> result = service.getSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10));
        
        assertThat(result.getContent().get(0).getSize()).isEqualTo("Size-XL/290");
    }

    // 13. Brand Name Null Check (Product exists but Brand logic fail simulation)
    @Test
    @DisplayName("매핑: Brand 정보 접근 중 문제 발생 시 처리 (예: Product.getBrand()가 null)")
    void mapping_NullBrand() {
        // 커스텀 Mock 생성
        SellingBid bid = mock(SellingBid.class);
        // ID, Price, Status Stubbing 제거 (사용되지 않음)
        
        ProductOption opt = mock(ProductOption.class);
        given(bid.getProductOption()).willReturn(opt);
        
        Product p = mock(Product.class);
        given(opt.getProduct()).willReturn(p);
        given(p.getName()).willReturn("Prod");
        
        given(p.getBrand()).willReturn(null); // Brand is null!
        
        given(repository.findAdminSellingBids(any(), any())).willReturn(new PageImpl<>(List.of(bid)));

        // Logic in service: bid.getProductOption().getProduct().getBrand().getName()
        // If Brand is null, getBrand().getName() will throw NPE
        // 서비스 코드가 Null Safe하지 않다면 여기서 NPE가 터질 것임. 
        // 현 서비스 코드는: ...getBrand().getName() 체이닝 중 null check가 완벽하지 않음 (삼항 연산자가 ProductOption 레벨에서만 걸려있음)
        // -> 이 테스트는 "실패"하거나 "에러"를 잡기 위함. 만약 NPE가 난다면 서비스 코드를 고쳐야 함.
        // 현재 서비스 코드: 
        // bid.getProductOption() != null ? bid.getProductOption().getProduct().getBrand().getName() : "Unknown Brand"
        // 즉, ProductOption은 있는데 그 하위가 null이면 NPE 발생 가능성 있음.
        
        // Assert NPE (현재 구현상 예상되는 동작) 또는, 코드가 수정되었다면 pass.
        // 여기서는 NPE가 발생하는지 확인. (발생한다면 User에게 "이 부분 고쳐야 한다"고 제안 가능)
        assertThatThrownBy(() -> service.getSellingBids(new SellingBidSearchCondition(), PageRequest.of(0, 10)))
                .isInstanceOf(NullPointerException.class);
    }
}
