# 📦 Unbox Workspace (MSA E-commerce Platform)

> **"Unbox"**는 한정판 거래 플랫폼(KREAM, StockX 등)을 모티브로 한 **마이크로서비스 아키텍처(MSA) 기반의 이커머스 시스템**입니다.  
> 대규모 트래픽과 확장성을 고려하여 서비스별로 도메인을 분리하고, 최신 기술 스택을 도입하여 엔지니어링 챌린지를 해결하는 것을 목표로 합니다.

---

## 🛠 기술 스택 (Tech Stack)

이 프로젝트는 안정성, 확장성, 그리고 생산성을 고려하여 다음과 같은 기술을 사용했습니다.

### ☕ Backend & Framework
- **Java 17**: 최신 LTS 버전으로, 레코드(Record) 등 모던 자바 문법을 활용하여 간결하고 견고한 코드를 작성합니다.
- **Spring Boot 3.5.9**: 스프링 부트 생태계를 기반으로 빠른 개발과 손쉬운 설정을 지원합니다.
- **Spring Cloud 2025.0.1**: MSA 환경에서의 서비스 간 통신(OpenFeign) 및 구성 관리를 담당합니다.
- **Gradle**: 멀티 모듈 프로젝트의 의존성을 효율적으로 관리하기 위해 사용합니다.

### 💾 Data & Persistence
- **PostgreSQL 16.11**: 메인 관계형 데이터베이스(RDBMS)로, 서비스별로 스키마를 논리적으로 분리(Database-per-service)하여 사용합니다.
- **Spring Data JPA & QueryDSL 5.0**: 복잡한 동적 쿼리 처리를 위해 Type-Safe한 QueryDSL을 도입하여 유지보수성을 높였습니다.
- **Redis (Alpine)**: 글로벌 캐싱 및 **분산 락(Distributed Lock)** 구현을 위해 사용합니다.

### 🔧 Utilities & Libraries
- **MapStruct 1.5.5**: 엔티티(Entity)와 DTO 간의 변환을 컴파일 시점에 처리하여 런타임 오버헤드를 줄였습니다.
- **Lombok**: 반복적인 보일러플레이트 코드를 줄여 비즈니스 로직에 집중하도록 돕습니다.
- **Redisson**: Redis 기반의 안정적인 분산 락 구현을 위해 사용합니다.

### ☁️ Infrastructure & DevOps
- **Docker & Docker Compose**: 로컬 개발 환경에서 데이터베이스, Redis, 5개의 마이크로서비스를 한 번에 실행하고 오케스트레이션합니다.
- **Terraform**: AWS 인프라(VPC, ECS, RDS 등)를 코드로 관리(IaC)하여 배포 환경의 일관성을 유지합니다.
- **GitHub Actions**: 개발(Dev) 및 운영(Prod) 환경에 맞춘 CI/CD 파이프라인을 구축하여 자동화된 테스트와 배포를 수행합니다.

---

## 🏗 시스템 아키텍처 (System Architecture)

이 프로젝트는 **Monorepo** 구조 안에 **Multi-repo** 스타일의 독립적인 서비스 모듈을 포함하고 있습니다.

### 🧩 서비스 구성 (Microservices Map)

각 서비스는 독립적인 배포가 가능하며, 도메인별로 명확한 역할과 책임을 가집니다.

| 서비스 명 | 디렉토리 | 포트 | 주요 역할 및 기능 |
| :--- | :--- | :--- | :--- |
| **Common Module** | `unbox_common` | - | **공통 모듈**: 모든 서비스에서 공유하는 Global Exception, Security Filter, 공통 DTO, 유틸리티, **분산 락(AOP)** 등을 관리합니다. |
| **User Service** | `unbox_user` | 8081 | **사용자/인증**: JWT 기반 로그인/회원가입, 관리자(Admin) 기능, 사용자 프로필 및 배송지 관리, 장바구니(Cart) 기능을 담당합니다. |
| **Product Service** | `unbox_product` | 8082 | **상품 카탈로그**: 브랜드(Brand), 상품(Product), 옵션(Option) 관리 및 AI 기반 리뷰 요약(Review Summary) 기능을 제공합니다. |
| **Trade Service** | `unbox_trade` | 8083 | **거래 체결**: 판매 입찰(Selling Bid) 및 구매 입찰(Buying Bid)을 처리하며, 가격 매칭 로직을 담당합니다. (현재 판매 입찰 API 우선 구현) |
| **Order Service** | `unbox_order` | 8084 | **주문/정산**: 체결된 거래의 주문 생성, 상태 추적, 그리고 정산(Settlement) 데이터를 처리합니다. |
| **Payment Service** | `unbox_payment` | 8085 | **결제**: PG사 연동 및 결제 승인/취소 로직을 처리하며, 결제 이력을 관리합니다. |

---

## 🏛 핵심 엔지니어링 전략 (Engineering Decisions)

### 1. 분산 락 (Distributed Lock) 전략
동시성 이슈(재고 차감, 쿠폰 사용, 선착순 구매 등)를 해결하기 위해 **Redis + Redisson**을 활용한 커스텀 분산 락을 구현했습니다.
- **구현 방식**: `@DistributedLock` 어노테이션을 생성하고 AOP를 통해 비즈니스 로직 전후로 락을 제어합니다.
- **트랜잭션 분리**: 락의 해제 시점과 DB 트랜잭션 커밋 시점의 불일치로 인한 동시성 문제를 방지하기 위해, `AopForTransaction`을 사용하여 **락 범위가 트랜잭션 범위를 감싸도록(Lock -> Transaction -> Unlock)** 설계했습니다.

### 2. 공통 모듈 (Shared Library) 설계
MSA의 단점인 '중복 코드'를 최소화하기 위해 철저한 공통 모듈 전략을 취했습니다.
- **`global-utils` 성격**: 단순한 복사-붙여넣기가 아니라, `unbox_common`을 의존성으로 추가하여 표준화된 에러 코드(`ErrorCode`), 응답 포맷(`ApiResponse`), 보안 설정 등을 일관되게 적용합니다.
- **이벤트 객체 공유**: 서비스 간 통신에 사용되는 이벤트 DTO들도 이곳에서 관리하여,Producer와 Consumer 간의 계약(Contract)을 명확히 합니다.

### 3. 데이터베이스 전략 및 초기화
- **Database-per-Service**: 각 마이크로서비스는 자신만의 데이터베이스를 가집니다. 이를 통해 서비스 간 결합도를 낮추고 데이터 독립성을 보장합니다.
- **자동 초기화**: `postgres_init/init.sql` 스크립트를 통해 컨테이너 실행 시 5개의 데이터베이스(`unbox_user`, `unbox_product` 등)가 자동으로 생성되도록 구성했습니다.

### 4. 확장성을 고려한 통신 (Sync to Async)
- **현재 (As-Is)**: OpenFeign을 사용한 HTTP 동기 통신으로 빠른 개발과 직관적인 흐름을 제어하고 있습니다.
- **미래 (To-Be)**: 트래픽이 몰리는 주문 생성이나 알림 발송 등은 **Kafka**를 도입하여 비동기 이벤트 기반(Event-Driven)으로 전환할 예정입니다. 이미 `unbox_common/event` 패키지에 이를 위한 이벤트 객체 구조를 잡아두었습니다.

### 5. AI 기능 통합 (AI Integration)
- **리뷰 요약**: Product 서비스에는 **AI 컨트롤러**(`AiController`)가 내장되어 있어, 상품 리뷰들을 분석하고 요약해주는 기능을 제공합니다. 이는 단순 커머스를 넘어 지능형 서비스로 나아가는 발판입니다.

---

## 🚀 실행 가이드 (Getting Started)

로컬 환경에서 전체 서비스를 띄우는 방법입니다.

### 사전 요구사항 (Prerequisites)
- Docker & Docker Compose
- Java 17 (코드 수정 시)

### 서비스 실행
프로젝트 루트에서 다음 명령어를 실행하면 DB, Redis, 그리고 모든 마이크로서비스가 실행됩니다.

```bash
docker-compose up -d --build
```

### 접속 정보 (Endpoints)
| 서비스 | URL | 비고 |
| :--- | :--- | :--- |
| **User API** | `http://localhost:8081` | 인증, 장바구니 |
| **Product API** | `http://localhost:8082` | 상품 조회, AI 리뷰 |
| **Trade API** | `http://localhost:8083` | 입찰 관리 |
| **Order API** | `http://localhost:8084` | 주문 상태 확인 |
| **Payment API** | `http://localhost:8085` | 결제 처리 |

---

## 📂 디렉토리 구조 (Directory Layout)

```text
unbox_workspace/
├── unbox_common/       # [공통] 공유 라이브러리 (DTO, Utils, Error, Security)
├── unbox_user/         # [서비스] 사용자, 인증, 장바구니
├── unbox_product/      # [서비스] 상품, 브랜드, 리뷰, AI
├── unbox_trade/        # [서비스] 거래 입찰/체결 (Selling/Buying Bid)
├── unbox_order/        # [서비스] 주문, 정산 (Settlement)
├── unbox_payment/      # [서비스] 결제 (PG 연동)
├── docker-compose.yml  # [인프라] 로컬 통합 실행 설정
├── terraform/          # [인프라] AWS 클라우드 리소스 정의
├── .github/workflows/  # [DevOps] CI/CD 파이프라인 정의
└── postgres_init/      # [DB] 데이터베이스 초기화 스크립트
```
