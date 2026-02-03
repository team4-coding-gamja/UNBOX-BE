# 구매 입찰(Buying Bid) 도입 및 즉시 판매(Immediate Sell) 구현 가이드

서비스에 **구매 입찰(Buying Bid)** 기능을 도입하면, 사용자가 원하는 상품에 대해 희망 구매 가격을 제시할 수 있습니다.
또한, 이를 통해 판매자는 해당 가격에 **즉시 판매(Immediate Sell)** 를 할 수 있게 됩니다.

이를 위해 `Trade Service`와 `Order Service` 양쪽의 코드 수정이 필요합니다.

---

## 1. 개요 (Architecture)

| 기능 | 기존 (즉시 구매) | 변경/추가 (즉시 판매) |
| :--- | :--- | :--- |
| **주체** | 구매자 (Buyer) | 판매자 (Seller) |
| **행동** | 판매 입찰(`SellingBid`)을 선택하여 주문 | 구매 입찰(`BuyingBid`)을 선택하여 판매 |
| **데이터 흐름** | `Order` -> `SellingBid` (LIVE -> RESERVED) | `Order` -> `BuyingBid` (LIVE -> MATCHED) |

---

## 2. Unbox Trade Service 수정

### 2.1 Entity 수정: `BuyingBid.java`
`BuyingBid` 엔티티가 존재하지만, 효율적인 조회와 인덱싱을 위해 `productId` 필드를 추가하는 것을 권장합니다.

```java
// com.example.unbox_trade.trade.domain.entity.BuyingBid.java

public class BuyingBid extends BaseEntity {
    // ... 기존 필드 ...

    @Column(name = "product_id", nullable = false)
    private UUID productId; // 상품 단위 조회를 위해 추가 권장

    // ...
}
```

### 2.2 Repository 생성: `BuyingBidRepository`
`SellingBidRepository`와 유사하게 구현합니다. 특히 **최고가 입찰 정보**를 조회하는 메서드가 중요합니다.

```java
// com.example.unbox_trade.trade.domain.repository.BuyingBidRepository.java

public interface BuyingBidRepository extends JpaRepository<BuyingBid, UUID> {
    
    // 특정 옵션의 최고가 입찰 조회 (즉시 판매 가격 노출용)
    @Query("SELECT MAX(b.price) FROM BuyingBid b WHERE b.optionId = :optionId AND b.status = 'LIVE'")
    Optional<Integer> findHighestPriceByOptionId(@Param("optionId") UUID optionId);

    // ...
}
```

### 2.3 Service 구현: `BuyingBidService`
`SellingBidService`와 대칭되는 기능을 구현해야 합니다.

*   **`createBuyingBid`**: 구매 입찰 생성
*   **`cancelBuyingBid`**: 본인의 입찰 취소
*   **`getBuyingBidForOrder` (Internal)**: 주문(즉시 판매) 시 `Order Service`에서 호출할 정보 제공
    *   입찰 가격, 구매자 ID(`buyerId`), 상품 옵션 ID 등을 반환
*   **`matchBuyingBid` (Internal)**: 주문 생성 시 입찰 상태 변경 (`LIVE` -> `MATCHED`)
    *   `SellingBid`의 `reserve`와 유사하지만, 즉시 체결되므로 `MATCHED`로 표현하거나 `RESERVED`를 사용할 수 있습니다.

### 2.4 Controller 구현
*   **`BuyingBidController`**: `POST /api/bids/buying` (입찰 생성), `DELETE /api/bids/buying/{id}` (취소) 등
*   **`BuyingBidInternalController`**: `Order Service`가 호출할 내부 API
    *   `GET /internal/bids/buying/{id}/for-order`
    *   `POST /internal/bids/buying/{id}/match`

---

## 3. Unbox Order Service 수정

### 3.1 Entity 수정: `Order.java`
주문은 이제 "판매 입찰"에서 올 수도 있고, "구매 입찰"에서 올 수도 있습니다.

```java
// com.example.unbox_order.order.domain.entity.Order.java

public class Order extends BaseEntity {

    // 1. sellingBidId를 Nullable로 변경
    @Column(name = "selling_bid_id") // nullable = false 제거
    private UUID sellingBidId;

    // 2. buyingBidId 추가
    @Column(name = "buying_bid_id")
    private UUID buyingBidId;

    // ...
}
```

### 3.2 DTO 수정: `OrderCreateRequestDto`
판매자가 "즉시 판매"를 할 때 사용할 필드를 추가합니다.

```java
// OrderCreateRequestDto.java
public class OrderCreateRequestDto {
    private UUID sellingBidId; // 구매자가 즉시 구매할 때
    private UUID buyingBidId;  // 판매자가 즉시 판매할 때 (신규)
    
    // ... Receiver Info ...
}
```

### 3.3 Feign Client 수정: `TradeClient`
`BuyingBid`와 통신할 메서드를 추가합니다.

```java
// TradeClient.java
@GetMapping("/internal/bids/buying/{buyingBidId}/for-order")
BuyingBidForOrderResponse getBuyingBidForOrder(@PathVariable("buyingBidId") UUID buyingBidId);

@PostMapping("/internal/bids/buying/{buyingBidId}/match")
void matchBuyingBid(@PathVariable("buyingBidId") UUID buyingBidId, @RequestParam("updatedBy") String updatedBy);
```

### 3.4 Service 로직 수정: `OrderServiceImpl.createOrder`
주문 생성 로직 분기 처리가 필요합니다.

```java
// OrderServiceImpl.java

@Transactional
public UUID createOrder(OrderCreateRequestDto requestDto, Long userId) {
    if (requestDto.getSellingBidId() != null) {
        // [CASE A] 기존 즉시 구매 로직 (Buyer가 User)
        // 1. userClient.getUserInfoForOrder(userId) -> 구매자 정보 정보
        // 2. tradeClient.getSellingBidForOrder(...)
        // 3. tradeClient.reserveSellingBid(...)
        // 4. Order 빌드 (buyerId = userId, sellerId = bid.sellerId)
        
    } else if (requestDto.getBuyingBidId() != null) {
        // [CASE B] 신규 즉시 판매 로직 (Seller가 User)
        
        // 1. 구매 입찰 정보 조회
        BuyingBidForOrderResponse buyingBidInfo = tradeClient.getBuyingBidForOrder(requestDto.getBuyingBidId());
        
        // 2. 판매자(본인) 정보 조회
        // UserInfoForOrderResponse seller = userClient.getUserInfoForOrder(userId); 
        
        // 3. 자기 자신과의 거래 방지 체크
        if (buyingBidInfo.getBuyerId().equals(userId)) { throw ... }
        
        // 4. 구매 입찰 체결 처리 (LIVE -> MATCHED)
        tradeClient.matchBuyingBid(buyingBidInfo.getBuyingBidId(), "ORDER_SERVICE");
        
        // 5. Order 구조 생성
        Order order = Order.builder()
            .buyingBidId(buyingBidInfo.getBuyingBidId())
            .buyerId(buyingBidInfo.getBuyerId()) // 입찰 올린 사람이 구매자
            .sellerId(userId)                  // 현재 로그인한 사람이 판매자
            // ... 상품 정보는 ProductClient 등을 통해 채움 ...
            .price(buyingBidInfo.getPrice())
            // 주의: 배송지 정보는 구매 입찰 시점에 입력받았거나, 
            // buyingBidInfo에 포함되어 있어야 합니다. 
            // 또는, 결제 단계가 즉시 판매의 경우 '정산 -> 배송' 흐름이므로
            // 판매자는 자신의 계좌만 등록하면 되고, 구매자의 배송지는 이미 시스템에 있어야 합니다.
            .build();
            
        // ... 저장 및 타이머 설정 ...
    }
}
```

> **주의점**: '즉시 판매' 시 배송 정보(`receiverName` 등)는 구매자가 입찰을 올릴 때 미리 설정해두었거나, `BuyingBid` 정보에 포함되어 있어야 합니다. `requestDto`로 들어오는 정보는 **판매자가 입력하는 정보**(반품 주소 등)일 수 있으므로 구분이 필요합니다. 보통 구매 입찰 생성 시 배송지 정보를 입력받아 `BuyingBid`에 저장해두는 것이 일반적입니다.

---

## 4. 요약
1. `unbox_trade`: `BuyingBid` 관련 Repository, Service, Controller(내부/외부) 풀 세트 구현.
2. `unbox_order`: `Order` 엔티티 필드 추가(buyingBidId) 및 `createOrder` 로직 분기(즉시구매 vs 즉시판매).
3. `UI/UX`: 프론트엔드에서 '구매 입찰 하기' 버튼과 '즉시 판매' 버튼을 각각 연결.

이 가이드를 바탕으로 구현을 시작하시면 됩니다. 상세 코드가 필요하면 말씀해 주세요.
