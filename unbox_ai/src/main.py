# src/main.py
import logging
import uvicorn
from fastapi import FastAPI
from sqlalchemy import text
from src.api.v1 import health, simulator
from src.core.database import engine, Base
from src.core.exceptions import CustomException, custom_exception_handler
from contextlib import asynccontextmanager

# 모델 로딩 (Base.metadata에 등록되기 위해 임포트 필수)
from src.models.analytics_event import AnalyticsEvent

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 시작 시 실행
    logger.info("UNBOX-AI 서비스 시작")
    try:
        with engine.begin() as conn:
            conn.execute(text("CREATE SCHEMA IF NOT EXISTS analytics"))
        Base.metadata.create_all(bind=engine)
        logger.info("DB 테이블 초기화 완료")
    except Exception as e:
        logger.warning(f"DB 연결 실패 - 테이블 자동 생성 건너뜀: {e}")
    
    yield
    
    # 종료 시 실행
    logger.info("UNBOX-AI 서비스 종료")

# 앱 생성
app = FastAPI(
    title="UNBOX-AI Service",
    description="데이터 분석/실험 시뮬레이션 & AI 인사이트 엔진",
    lifespan=lifespan
)

app.add_exception_handler(CustomException, custom_exception_handler)

# 라우터 등록 (Java의 Controller 등록과 동일)
app.include_router(health.router, prefix="/api/v1")
app.include_router(simulator.router, prefix="/api/v1")

@app.get("/")
def read_root():
    return {"message": "Welcome to UNBOX-AI API"}

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
