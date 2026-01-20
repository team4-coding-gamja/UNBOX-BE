# 🛍️ UNBOX Product Service

UNBOX 플랫폼의 상품 관리 및 카탈로그 서비스입니다.

## 🚀 빠른 시작

### 로컬 개발 환경 설정

```bash
# 1. 환경 설정
cp docker/local/.env.example docker/local/.env
# .env 파일을 열어서 필요한 값들을 수정하세요

# 2. 로컬 환경 시작
./scripts/local-setup.sh
```

### 서비스 접속

- **애플리케이션**: http://localhost:8082
- **Health Check**: http://localhost:8082/actuator/health
- **API 문서**: http://localhost:8082/swagger-ui.html

## 🔧 개발 환경

### 필수 요구사항

- Java 17+
- Docker & Docker Compose
- Gradle 7.0+

### 데이터베이스

- **PostgreSQL**: localhost:5434
- **Redis**: localhost:6381

```bash
# PostgreSQL 접속
docker exec -it unbox-product-postgres psql -U unbox_user -d unbox_product_local

# Redis 접속
docker exec -it unbox-product-redis redis-cli
```

## 📋 주요 기능

- 상품 등록/수정/삭제
- 상품 카탈로그 관리
- 상품 검색 및 필터링
- 상품 이미지 관리
- 재고 관리

## 🛠️ 유용한 명령어

```bash
# 애플리케이션 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 로컬 환경 시작
./scripts/local-setup.sh

# 로그 확인
docker-compose -f docker/local/docker-compose.yml logs -f

# 서비스 중지
docker-compose -f docker/local/docker-compose.yml down

# 데이터까지 완전 삭제
docker-compose -f docker/local/docker-compose.yml down -v
```

## ✅ 푸시 전 체크리스트

코드를 푸시하기 전에 다음 사항들을 확인하세요:

- [ ] `./gradlew test` - 모든 테스트 통과
- [ ] `./gradlew build` - 빌드 성공  
- [ ] `./scripts/local-setup.sh` - 로컬 환경에서 정상 동작 확인
- [ ] http://localhost:8082/actuator/health - 헬스 체크 통과

## 🌐 API 엔드포인트

### 상품 관리
- `GET /api/products` - 상품 목록 조회
- `GET /api/products/{id}` - 상품 상세 조회
- `POST /api/products` - 상품 등록
- `PUT /api/products/{id}` - 상품 수정
- `DELETE /api/products/{id}` - 상품 삭제

### 카테고리 관리
- `GET /api/categories` - 카테고리 목록 조회
- `POST /api/categories` - 카테고리 등록

## 🔍 문제 해결

### 포트 충돌 시
```bash
# 포트 사용 중인 프로세스 확인
lsof -i :8082
lsof -i :5434
lsof -i :6381

# .env 파일에서 포트 변경 후 재시작
```

### 데이터베이스 연결 실패 시
```bash
# 컨테이너 상태 확인
docker ps

# PostgreSQL 로그 확인
docker logs unbox-product-postgres

# 컨테이너 재시작
docker-compose -f docker/local/docker-compose.yml restart postgres
```

## 📞 지원

문제가 발생하면 다음을 확인해주세요:
1. Docker가 실행 중인지 확인
2. .env 파일의 설정값 확인
3. 포트 충돌 여부 확인
4. 로그 메시지 확인