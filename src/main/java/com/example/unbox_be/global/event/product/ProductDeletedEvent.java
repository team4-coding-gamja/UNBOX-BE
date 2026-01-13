package com.example.unbox_be.global.event.product;

import java.util.List;
import java.util.UUID;

public record ProductDeletedEvent(UUID productId, List<UUID> deletedOptionIds) {
}
