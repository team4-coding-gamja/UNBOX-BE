package com.example.unbox_common.event.product;

import java.util.List;
import java.util.UUID;

public record BrandDeletedEvent(UUID brandId, List<UUID> deletedProductIds, List<UUID> deletedOptionIds){
}
