# 🚀 UNBOX MSA 로컬 개발 환경 설정 가이드

## 📋 개요
이 가이드는 UNBOX 서비스를 MSA로 분리할 때 각 서비스별 로컬 개발 환경을 설정하는 방법을 설명합니다.

## 🎯 대상 독자
- 서비스 분리 작업을 진행하는 백엔드 개발자
- 분리된 서비스의 로컬 환경을 설정해야 하는 개발자

## 📦 분리 예정 서비스 목록

| 서비스명 | 포트 | 담당 도메인 | 상태 |
|----------|------|-------------|------|
| Core Business | 8080 | API Gateway, 핵심 비즈니스 로직 | 기본 유지 |
| User Service | 8081 | 사용자 관리, 인증/인가 | 분리 예정 |
| Product Service | 8082 | 상품 관리, 카탈로그 | 분리 진행 중 |
| Trade Service | 8083 | 거래 관리, 중고거래 | 분리 예정 |
| Order Service | 8084 | 주문 처리, 주문 이력 | 분리 예정 |
| Payment Service | 8085 | 결제 처리, 정산 | 분리 예정 |

## 🔄 서비스 분리 시 로컬 환경 설정 절차

### 1단계: 서비스 분리 완료 후
```bash
# 새로운 서비스 리포지토리로 이동
cd /path/to/your-new-service-repo
```

### 2단계: 템플릿 가져오기
```bash
# UNBOX-BE 리포지토리에서 템플릿 가져오기
git clone https://github.com/your-org/UNBOX-BE.git temp-templates
cd temp-templates
git checkout feat/cicd-pipeline
```

### 3단계: 해당 서비스 템플릿 배포
```bash
# 예시: User Service인 경우
./scripts/deploy-templates.sh user-service ../

# 예시: Product Service인 경우  
./scripts/deploy-templates.sh product-service ../
```

**📝 자동 생성되는 파일들:**
- `docker/local/.env` 파일이 `.env.example`에서 자동으로 복사됩니다
- 기존 `.env` 파일이 있으면 백업 후 새로 생성됩니다
- 스크립트 실행 권한이 자동으로 부여됩니다

### 4단계: 임시 파일 정리
```bash
cd ..
rm -rf temp-templates
```

### 5단계: 환경 설정
```bash
# .env 파일 수정 (3단계에서 자동 생성됨)
vim docker/local/.env

# 필수 수정 항목:
# - POSTGRES_PASSWORD: 데이터베이스 비밀번호 (기본값: ChangeHERE!)
# - JWT_SECRET: JWT 시크릿 키 (32자 이상, 기본값: ChangeHERE!)
```

**⚠️ 중요**: 3단계에서 `.env` 파일이 자동으로 생성되지만, 보안을 위해 반드시 비밀번호와 시크릿 키를 변경해야 합니다.

### 6단계: 로컬 환경 시작
```bash
# 로컬 개발 환경 시작
./scripts/local-setup.sh
```

## 🔧 생성되는 파일 구조

템플릿 배포 후 다음과 같은 구조가 생성됩니다:

```
your-service-repo/
├── docker/
│   └── local/
│       ├── docker-compose.yml    # Docker Compose 설정
│       ├── .env.example         # 환경변수 예시 (템플릿)
│       └── .env                 # 실제 환경변수 (자동 생성, 수정 필요)
├── scripts/
│   └── local-setup.sh          # 로컬 환경 시작 스크립트 (실행권한 자동 부여)
└── logs/                       # 애플리케이션 로그 디렉토리
```

## 🌐 서비스 접속 정보

로컬 환경 시작 후 다음 URL로 접속 가능합니다:

- **애플리케이션**: http://localhost:{포트번호}
- **Health Check**: http://localhost:{포트번호}/actuator/health
- **데이터베이스**: localhost:{PostgreSQL포트} (기본값은 각 서비스별로 다름)
- **Redis**: localhost:{Redis포트} (기본값은 각 서비스별로 다름)

## 🛠️ 유용한 명령어들

### 로그 확인
```bash
# 실시간 로그 확인
docker-compose -f docker/local/docker-compose.yml logs -f

# 특정 서비스 로그만 확인
docker-compose -f docker/local/docker-compose.yml logs -f [서비스명]
```

### 서비스 제어
```bash
# 서비스 중지
docker-compose -f docker/local/docker-compose.yml down

# 서비스 재시작
docker-compose -f docker/local/docker-compose.yml restart

# 데이터까지 완전 삭제 후 중지
docker-compose -f docker/local/docker-compose.yml down -v
```

### 데이터베이스 접속
```bash
# PostgreSQL 접속 (서비스별로 컨테이너명이 다름)
docker exec -it unbox-user-postgres psql -U unbox_user -d unbox_user_local
docker exec -it unbox-product-postgres psql -U unbox_user -d unbox_product_local
```

## ❗ 주의사항

1. **포트 충돌**: 다른 서비스와 포트가 겹치지 않도록 주의
2. **환경변수**: `.env` 파일의 비밀번호와 시크릿 키는 반드시 수정
3. **데이터 분리**: 각 서비스는 독립적인 데이터베이스 사용
4. **네트워크**: 서비스 간 통신은 HTTP API로 진행

## 🆘 문제 해결

### .env 파일이 생성되지 않는 경우
```bash
# 1. 템플릿 배포 스크립트 실행 시 상세 로그 확인
./scripts/deploy-templates.sh product-service . 2>&1 | tee deploy.log

# 2. 수동으로 .env 파일 생성
cp docker/local/.env.example docker/local/.env

# 3. 파일 권한 확인 및 수정
ls -la docker/local/
chmod 644 docker/local/.env.example
chmod 644 docker/local/.env

# 4. 디렉토리 권한 확인
ls -la docker/
chmod 755 docker/local/
```

### 포트 충돌 시
```bash
# 포트 사용 중인 프로세스 확인
lsof -i :8081

# 해당 프로세스 종료 후 다시 시도
```

### Docker 관련 문제
```bash
# Docker 데몬 상태 확인
docker ps

# Docker Compose 파일 문법 확인
docker-compose -f docker/local/docker-compose.yml config
```

### 서비스 시작 실패 시
```bash
# 상세 로그 확인
docker-compose -f docker/local/docker-compose.yml logs

# 개별 컨테이너 상태 확인
docker ps -a
```

## 문제발생 시 확인사항

문제가 발생하면 다음을 확인해주세요:
1. 이 가이드의 문제 해결 섹션
2. `scripts/README.md` 파일
3. 각 서비스 템플릿의 로그 출력