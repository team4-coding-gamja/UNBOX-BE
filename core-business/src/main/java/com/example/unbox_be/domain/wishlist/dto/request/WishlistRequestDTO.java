package com.example.unbox_be.domain.wishlist.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WishlistRequestDTO {
    private UUID optionId;
}
