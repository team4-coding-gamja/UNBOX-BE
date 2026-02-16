import os
from pydantic_settings import BaseSettings
from dotenv import load_dotenv

# 로컬 개발용 .env 로딩
dotenv_path = os.path.join(os.path.dirname(__file__), '..', '..', '..', '.env')
load_dotenv(dotenv_path)

class Settings(BaseSettings):
    PROJECT_NAME: str = "UNBOX-AI"

    # =========================================
    # 1. Docker / Cloud 환경 변수 (우선순위 높음)
    # =========================================
    POSTGRES_SERVER: str = os.getenv("POSTGRES_SERVER")
    POSTGRES_PORT: str = os.getenv("POSTGRES_PORT", "5432")
    POSTGRES_DB: str = os.getenv("POSTGRES_DB", "unbox_ai")
    POSTGRES_USER: str = os.getenv("POSTGRES_USER")
    POSTGRES_PASSWORD: str = os.getenv("POSTGRES_PASSWORD")

    # =========================================
    # 2. 로컬 / Java 공유 환경 변수 (Fallback)
    # =========================================
    DB_URL: str = os.getenv("DB_URL")
    # 로컬에서만 쓰는 변수들도 안전하게 가져오기 위해 getenv 사용
    LOCAL_DB_USER: str = os.getenv("DB_USERNAME", "postgres") 
    LOCAL_DB_PW: str = os.getenv("DB_PASSWORD", "password")

    SPRING_JWT_SECRET: str = os.getenv("SPRING_JWT_SECRET")

    @property
    def DATABASE_URL(self) -> str:
        """
        Docker 환경 변수가 있으면 그걸로 URL을 만들고,
        없으면 로컬 .env의 JDBC URL을 파싱해서 만듭니다.
        """
        # Case A: Docker (POSTGRES_SERVER가 설정된 경우)
        if self.POSTGRES_SERVER:
            user = self.POSTGRES_USER or self.LOCAL_DB_USER
            pw = self.POSTGRES_PASSWORD or self.LOCAL_DB_PW
            return f"postgresql://{user}:{pw}@{self.POSTGRES_SERVER}:{self.POSTGRES_PORT}/{self.POSTGRES_DB}"

        # Case B: Local (JDBC URL 파싱)
        if self.DB_URL and self.DB_URL.startswith("jdbc:"):
            pure_url = self.DB_URL.replace("jdbc:", "")
            # jdbc:postgresql://localhost:5432/test_db
            # -> postgresql://user:pw@localhost:5432/test_db 로 변환
            clean_url = pure_url.replace("//", f"//{self.LOCAL_DB_USER}:{self.LOCAL_DB_PW}@")
            return clean_url

        # Case C: 그 외 (직접 설정 등)
        return self.DB_URL or "postgresql://postgres:password@localhost:5432/unbox_ai"

    # 보안 설정
    JWT_SECRET_KEY: str = SPRING_JWT_SECRET
    ALGORITHM: str = "HS256"

settings = Settings()