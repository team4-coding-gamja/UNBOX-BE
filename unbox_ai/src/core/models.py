import uuid
from datetime import datetime
from sqlalchemy import Column, DateTime, Boolean
from sqlalchemy.dialects.postgresql import UUID  # Postgres 전용 UUID 타입
from src.core.database import Base

class BaseEntity(Base):
    """
    Java의 @MappedSuperclass BaseEntity와 동일한 역할
    모든 엔티티가 상속받을 공통 부모 클래스
    """
    __abstract__ = True  # 이 클래스는 테이블로 생성되지 않음 (Java의 abstract class)

    # 1. UUID PK 설정
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)

    # 2. Audit Fields (생성일, 수정일)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    # 3. Soft Delete Fields
    deleted_at = Column(DateTime, nullable=True) # 삭제된 시간 (null이면 삭제 안 됨)
    is_deleted = Column(Boolean, default=False, nullable=False)

    def soft_delete(self):
        """삭제 처리 메서드"""
        self.is_deleted = True
        self.deleted_at = datetime.utcnow()