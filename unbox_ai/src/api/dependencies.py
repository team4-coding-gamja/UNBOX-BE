from fastapi import Depends
from sqlalchemy.orm import Session
from src.core.database import get_db
from src.repository.market_repo import MarketRepository
from src.service.market_service import MarketService

def get_market_service(db: Session = Depends(get_db)) -> MarketService:
    # Repository 생성 시 DB 세션 주입
    repository = MarketRepository(db)
    # Service 생성 시 Repository 주입
    return MarketService(repository)