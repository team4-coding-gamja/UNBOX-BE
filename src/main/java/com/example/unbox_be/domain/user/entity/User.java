package com.example.unbox_be.domain.user.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Long 타입 사용 권장

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 10)
    @Size(min = 4, max = 10)
    @Pattern(regexp = "^[a-z0-9]*$", message = "소문자와 숫자만 입력 가능합니다.")
    private String nickname;

    @Pattern(regexp = "^01[0-9]{8,9}$")
    private String phone;


    // 1. 배송지 목록
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    // 2. 장바구니
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cart cart;

    // 3. 위시리스트
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Wishlist> wishlists = new ArrayList<>();

    // 4. 구매 입찰 내역
    @OneToMany(mappedBy = "user")
    private List<BuyingBid> buyingBids = new ArrayList<>();

    // 5. 판매 입찰 내역
    @OneToMany(mappedBy = "user")
    private List<SellingBid> sellingBids = new ArrayList<>();

    // 6. 주문 내역 (구매자로서)
    @OneToMany(mappedBy = "buyer")
    private List<Order> orderedPurchases = new ArrayList<>();

    // 7. 주문 내역 (판매자로서)
    @OneToMany(mappedBy = "seller")
    private List<Order> orderedSales = new ArrayList<>();

    // 8. 리뷰 작성 내역
    @OneToMany(mappedBy = "buyer")
    private List<Review> reviews = new ArrayList<>();

    public void update(String nickname, String phone) {
        this.nickname = nickname;
        this.phone = phone;
    }
}
