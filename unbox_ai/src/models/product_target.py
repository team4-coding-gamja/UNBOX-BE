from sqlalchemy import Column, String, Boolean, ForeignKey, DateTime
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
import uuid
from datetime import datetime
from src.core.database import Base

class ProductTarget(Base):
    """
    크롤링 대상 상품 관리 (Java: Product 대응)
    - unbox-product 서비스의 Product와 1:1 또는 N:1 매핑될 수 있음
    - model_number(스타일 코드)를 기준으로 식별
    """
    __tablename__ = "ai_products"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    
    # unbox-product의 Product ID (나중에 데이터 연동 시 사용)
    ref_product_id = Column(UUID(as_uuid=True), nullable=True, index=True)
    
    name = Column(String, nullable=False) # 상품명 (예: Jordan 1 Chicago)
    model_number = Column(String, unique=True, nullable=False, index=True) # 모델번호 (예: DZ5485-612)
    brand_name = Column(String, nullable=True) # 브랜드명 (단순화를 위해 String으로 관리)
    
    is_active = Column(Boolean, default=True) # 크롤링 활성화 여부
    
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    options = relationship("ProductOptionTarget", back_populates="product", cascade="all, delete-orphan")
    market_prices = relationship("MarketData", back_populates="product")

class ProductOptionTarget(Base):
    """
    크롤링 대상 상품의 옵션/사이즈 (Java: ProductOption 대응)
    - 사이즈별 시세 관리를 위해 필요
    """
    __tablename__ = "ai_product_options"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    product_id = Column(UUID(as_uuid=True), ForeignKey("ai_products.id"), nullable=False)
    
    size = Column(String, nullable=False) # 사이즈 (예: 260, 270, L, XL)
    
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    product = relationship("ProductTarget", back_populates="options")
    market_prices = relationship("MarketData", back_populates="option")
