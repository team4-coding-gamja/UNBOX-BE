# MSA 서비스별 로컬 개발 환경 템플릿

이 디렉토리는 각 마이크로서비스가 분리될 때 사용할 로컬 개발 환경 템플릿을 포함합니다.

## 📁 서비스별 템플릿 구조

```
templates/
├── core-business/     # 핵심 비즈니스 로직 (API Gateway 포함)
├── user-service/      # 사용자 관리 서비스
├── order-service/     # 주문 관리 서비스
├── payment-service/   # 결제 처리 서비스
├── trade-service/     # 거래 관리 서비스
└── product-service/   # 상품 관리 서비스
```

## 🚀 사용 방법

1. 서비스 분리 완료 후 해당 서비스 리포지토리로 이동
2. 해당 서비스 템플릿의 `docker/` 폴더를 복사
3. `.env.example`을 `.env`로 복사하고 환경변수 설정
4. `scripts/local-setup.sh` 실행

## 🔧 포트 할당

- Core Business: 8080 (API Gateway)
- User Service: 8081
- Product Service: 8082  
- Trade Service: 8083
- Order Service: 8084
- Payment Service: 8085

## 📊 데이터베이스 분리 전략

- **개발 환경**: 각 서비스별 독립 데이터베이스
- **운영 환경**: RDS 인스턴스 분리 (비용 고려)