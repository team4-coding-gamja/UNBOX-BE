import logging
import asyncio
from typing import List
from sqlalchemy.orm import Session
from src.core.database import SessionLocal
from src.repository.market_repo import MarketRepository
from src.service.market_service import MarketService

logger = logging.getLogger(__name__)

async def refresh_target_shoes_price(target_shoes: List[str]):
    """
    주어진 신발 리스트의 시세를 갱신하는 주기적 작업(Job)입니다.
    APScheduler에 의해 호출됩니다.
    
    Args:
        target_shoes (List[str]): 시세를 조회할 신발 이름 목록
    """
    logger.info(f"[Job Start] {len(target_shoes)}개 품목 시세 업데이트 시작")
    
    # 1. 스케줄러는 백그라운드 작업이므로 수동으로 DB 세션 생성/관리 필수
    db: Session = SessionLocal()
    
    try:
        # 2. 의존성 주입 (수동 구성)
        # FastAPI의 Depends를 사용할 수 없으므로 직접 클래스를 인스턴스화합니다.
        market_repo = MarketRepository(db)
        market_service = MarketService(market_repo)
        
        # 3. 비동기 루프로 각 상품 시세 업데이트
        # (순차 처리 방식 - 병렬 처리가 필요하면 asyncio.gather 고려)
        success_count = 0
        fail_count = 0
        
        for name in target_shoes:
            try:
                # Service 메서드 호출
                await market_service.refresh_market_price(name)
                logger.info(f"업데이트 성공: {name}")
                success_count += 1
            except Exception as e:
                logger.error(f"업데이트 실패: {name} - {e}")
                fail_count += 1
                
        logger.info(f"[Job Finish] 성공: {success_count}, 실패: {fail_count}")
        
    except Exception as e:
        logger.critical(f"[Job Critical Error] 스케줄러 실행 중 예외 발생: {e}")
    finally:
        # 세션 반환 (필수)
        db.close()
