package com.example.unbox_common.event.product;

import java.util.List;
import java.util.UUID;

public record ProductDeletedEvent(UUID productId, List<UUID> deletedOptionIds) {
}
