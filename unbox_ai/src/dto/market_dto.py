from pydantic import BaseModel, ConfigDict, Field
from uuid import UUID
from datetime import datetime
from typing import Optional

class MarketPriceResponse(BaseModel):
    """상품 시세 응답 DTO"""
    id: UUID
    product_name: str
    price: int
    
    # [New] 상세 정보
    size: Optional[str] = None
    model_number: Optional[str] = None
    platform: str
    
    updated_at: datetime 
    
    # SQLAlchemy 모델(Entity)에서 DTO로 자동 변환을 위해 필요 (Java의 ModelMapper 역할)
    model_config = ConfigDict(from_attributes=True)

from typing import Optional, List

# ... (기존 코드)

class OptionPriceInfo(BaseModel):
    size: str
    price: int
    updated_at: datetime

class MarketDetailResponse(BaseModel):
    model_number: str
    name: str
    options: List[OptionPriceInfo]

class MarketPriceCreateRequest(BaseModel):
    """시세 수집 요청 DTO"""
    product_name: str = Field(..., example="나이키 덩크 로우 범고래")