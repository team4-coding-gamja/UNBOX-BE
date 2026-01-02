package com.example.unbox_be.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrder is a Querydsl query type for Order
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrder extends EntityPathBase<Order> {

    private static final long serialVersionUID = -942945888L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrder order = new QOrder("order1");

    public final com.example.unbox_be.domain.common.QBaseEntity _super = new com.example.unbox_be.domain.common.QBaseEntity(this);

    public final com.example.unbox_be.domain.user.entity.QUser buyer;

    public final DateTimePath<java.time.LocalDateTime> cancelledAt = createDateTime("cancelledAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    //inherited
    public final StringPath deletedBy = _super.deletedBy;

    public final StringPath finalTrackingNumber = createString("finalTrackingNumber");

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final com.example.unbox_be.domain.product.entity.QProductOption productOption;

    public final StringPath receiverAddress = createString("receiverAddress");

    public final StringPath receiverName = createString("receiverName");

    public final StringPath receiverPhone = createString("receiverPhone");

    public final StringPath receiverZipCode = createString("receiverZipCode");

    public final com.example.unbox_be.domain.user.entity.QUser seller;

    public final EnumPath<OrderStatus> status = createEnum("status", OrderStatus.class);

    public final StringPath trackingNumber = createString("trackingNumber");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public QOrder(String variable) {
        this(Order.class, forVariable(variable), INITS);
    }

    public QOrder(Path<? extends Order> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrder(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrder(PathMetadata metadata, PathInits inits) {
        this(Order.class, metadata, inits);
    }

    public QOrder(Class<? extends Order> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.buyer = inits.isInitialized("buyer") ? new com.example.unbox_be.domain.user.entity.QUser(forProperty("buyer")) : null;
        this.productOption = inits.isInitialized("productOption") ? new com.example.unbox_be.domain.product.entity.QProductOption(forProperty("productOption"), inits.get("productOption")) : null;
        this.seller = inits.isInitialized("seller") ? new com.example.unbox_be.domain.user.entity.QUser(forProperty("seller")) : null;
    }

}

