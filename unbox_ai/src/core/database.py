from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from src.core.config import settings

# 1. DB 엔진 생성
engine = create_engine(settings.DATABASE_URL)

# 2. 세션 팩토리 생성 (Java의 SessionFactory)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# 3. 모델들의 부모 클래스
Base = declarative_base()

# 4. 의존성 주입을 위한 DB 세션 생성 함수
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()