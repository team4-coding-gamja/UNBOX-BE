package com.example.unbox_be.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInspection is a Querydsl query type for Inspection
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInspection extends EntityPathBase<Inspection> {

    private static final long serialVersionUID = -1359516062L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInspection inspection = new QInspection("inspection");

    public final com.example.unbox_be.domain.common.QBaseEntity _super = new com.example.unbox_be.domain.common.QBaseEntity(this);

    public final DateTimePath<java.time.LocalDateTime> completedAt = createDateTime("completedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    //inherited
    public final StringPath deletedBy = _super.deletedBy;

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final com.example.unbox_be.domain.user.entity.QUser inspector;

    public final EnumPath<InspectionStatus> inspectStatus = createEnum("inspectStatus", InspectionStatus.class);

    public final QOrder order;

    public final StringPath reason = createString("reason");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public QInspection(String variable) {
        this(Inspection.class, forVariable(variable), INITS);
    }

    public QInspection(Path<? extends Inspection> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QInspection(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QInspection(PathMetadata metadata, PathInits inits) {
        this(Inspection.class, metadata, inits);
    }

    public QInspection(Class<? extends Inspection> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.inspector = inits.isInitialized("inspector") ? new com.example.unbox_be.domain.user.entity.QUser(forProperty("inspector")) : null;
        this.order = inits.isInitialized("order") ? new QOrder(forProperty("order"), inits.get("order")) : null;
    }

}

