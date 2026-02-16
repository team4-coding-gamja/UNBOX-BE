from sqlalchemy import Column, String, Integer, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from src.core.models import BaseEntity

class MarketData(BaseEntity):
    """
    수집된 시세 데이터를 저장하는 엔티티
    """
    __tablename__ = "market_data"

    product_name = Column(String, index=True, nullable=True) 
    
    price = Column(Integer, nullable=False)
    platform = Column(String, default="KREAM", nullable=False)
    
    target_id = Column(UUID(as_uuid=True), ForeignKey("ai_products.id"), nullable=True)
    option_id = Column(UUID(as_uuid=True), ForeignKey("ai_product_options.id"), nullable=True)

    # Relationships
    product = relationship("ProductTarget", back_populates="market_prices")
    option = relationship("ProductOptionTarget", back_populates="market_prices")

    # [New] DTO 변환을 위한 Helper Properties (Pydantic에서 읽을 수 있음)
    @property
    def size(self):
        return self.option.size if self.option else None

    @property
    def model_number(self):
        return self.product.model_number if self.product else None