package com.example.unbox_be.common.event.product;

import java.util.List;
import java.util.UUID;

public record BrandDeletedEvent(UUID brandId, List<UUID> deletedProductIds, List<UUID> deletedOptionIds){
}
