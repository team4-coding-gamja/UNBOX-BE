package com.example.unbox_be.domain.trade.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBuyingBid is a Querydsl query type for BuyingBid
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBuyingBid extends EntityPathBase<BuyingBid> {

    private static final long serialVersionUID = 839506365L;

    public static final QBuyingBid buyingBid = new QBuyingBid("buyingBid");

    public final com.example.unbox_be.domain.common.QBaseEntity _super = new com.example.unbox_be.domain.common.QBaseEntity(this);

    public final ComparablePath<java.util.UUID> buyingId = createComparable("buyingId", java.util.UUID.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final DateTimePath<java.time.LocalDateTime> deadline = createDateTime("deadline", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    //inherited
    public final StringPath deletedBy = _super.deletedBy;

    public final ComparablePath<java.util.UUID> optionId = createComparable("optionId", java.util.UUID.class);

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public final EnumPath<BuyingStatus> status = createEnum("status", BuyingStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QBuyingBid(String variable) {
        super(BuyingBid.class, forVariable(variable));
    }

    public QBuyingBid(Path<? extends BuyingBid> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBuyingBid(PathMetadata metadata) {
        super(BuyingBid.class, metadata);
    }

}

