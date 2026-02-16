from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import text
from src.core.database import get_db
from src.dto.common_dto import BaseResponse
import time

router = APIRouter(prefix="/health", tags=["System Health"])

@router.get("", response_model=BaseResponse[dict])
def health_check(db: Session = Depends(get_db)):
    """
    서버 및 데이터베이스 연결 상태를 점검합니다.
    """
    start_time = time.time()
    
    try:
        # 1. DB 연결 테스트 (SELECT 1)
        db.execute(text("SELECT 1"))
        db_status = "connected"
    except Exception as e:
        db_status = f"disconnected: {str(e)}"
    
    latency = round((time.time() - start_time) * 1000, 2) # ms 단위
    
    status_data = {
        "status": "up",
        "database": db_status,
        "latency_ms": latency
    }
    
    return BaseResponse(
        success=True if db_status == "connected" else False,
        message="System is healthy" if db_status == "connected" else "System has issues",
        data=status_data
    )