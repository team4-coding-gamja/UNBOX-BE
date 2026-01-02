package com.example.unbox_be.domain.trade.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSellingBid is a Querydsl query type for SellingBid
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSellingBid extends EntityPathBase<SellingBid> {

    private static final long serialVersionUID = 1408828689L;

    public static final QSellingBid sellingBid = new QSellingBid("sellingBid");

    public final com.example.unbox_be.domain.common.QBaseEntity _super = new com.example.unbox_be.domain.common.QBaseEntity(this);

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

    public final ComparablePath<java.util.UUID> sellingId = createComparable("sellingId", java.util.UUID.class);

    public final EnumPath<SellingStatus> status = createEnum("status", SellingStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QSellingBid(String variable) {
        super(SellingBid.class, forVariable(variable));
    }

    public QSellingBid(Path<? extends SellingBid> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSellingBid(PathMetadata metadata) {
        super(SellingBid.class, metadata);
    }

}

