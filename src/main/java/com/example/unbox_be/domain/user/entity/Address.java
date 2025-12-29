package com.example.unbox_be.domain.user.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_address")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "address_uuid", nullable = false, unique = true, updatable = false)
    private UUID addressUuid; // 외부 노출용 UUID

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiver_name;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "zip_code", length = 10)
    private String zipCode;


    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Address(User user, String receiver_name, String address, String detailAddress, String zipCode, boolean isDefault) {
        this.addressUuid = UUID.randomUUID();
        this.user = user;
        this.receiver_name = receiver_name;
        this.address = address;
        this.detailAddress = detailAddress;
        this.zipCode = zipCode;
        this.isDefault = isDefault;
    }

}
