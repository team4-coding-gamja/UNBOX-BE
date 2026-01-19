# UNBOX MSA 개발 환경 스크립트 가이드

이 디렉토리는 UNBOX MSA 전환을 위한 개발 환경 관리 스크립트들을 포함합니다.

## 📁 스크립트 목록

### 1. `deploy-templates.sh` - 템플릿 배포 도구
각 서비스가 분리될 때 해당 서비스 리포지토리에 로컬 개발 환경을 설정하는 스크립트

**사용법:**
```bash
# 현재 UNBOX-BE 리포지토리에서 실행
./scripts/deploy-templates.sh <서비스명> <대상_디렉토리>

# 예시: User Service 리포지토리에 템플릿 배포
./scripts/deploy-templates.sh user-service /path/to/user-service-repo

# 예시: 현재 디렉토리에 Product Service 템플릿 배포  
./scripts/deploy-templates.sh product-service .
```

### 2. `setup-multi-repo.sh` - 멀티 리포 통합 관리
모든 MSA 서비스 리포지토리를 한 번에 클론하고 개발 환경을 설정하는 스크립트

**사용법:**
```bash
./scripts/setup-multi-repo.sh
```

## 🔄 개발자 워크플로우

### Phase 1: 서비스 분리 전 (현재)
1. **인프라 담당자 (당신)**: 
   - `feat/cicd-pipeline` 브랜치에 템플릿 push
   - 개발자들에게 템플릿 사용법 안내

### Phase 2: 서비스 분리 시
1. **백엔드 개발자**:
   ```bash
   # 1. 서비스 분리 완료 후 새 리포지토리로 이동
   cd /path/to/new-service-repo
   
   # 2. UNBOX-BE에서 템플릿 가져오기
   git clone https://github.com/your-org/UNBOX-BE.git temp-templates
   cd temp-templates
   git checkout feat/cicd-pipeline
   
   # 3. 해당 서비스 템플릿 배포
   ./scripts/deploy-templates.sh user-service ../
   
   # 4. 임시 디렉토리 정리
   cd ..
   rm -rf temp-templates
   
   # 5. 로컬 환경 설정
   vim docker/local/.env  # 환경변수 수정
   ./scripts/local-setup.sh  # 로컬 환경 시작
   ```

### Phase 3: 통합 개발 환경 (모든 서비스 분리 완료 후)
1. **개발자들**:
   ```bash
   # 새로운 워크스페이스에서 모든 서비스 클론
   ./scripts/setup-multi-repo.sh
   ```

## 🎯 각 서비스별 포트 정보

| 서비스 | 포트 | 역할 |
|--------|------|------|
| Core Business | 8080 | API Gateway |
| User Service | 8081 | 사용자 관리 |
| Product Service | 8082 | 상품 관리 |
| Trade Service | 8083 | 거래 관리 |
| Order Service | 8084 | 주문 관리 |
| Payment Service | 8085 | 결제 처리 |

## 🔧 문제 해결

### 템플릿 배포 실패 시
```bash
# 권한 확인
chmod +x scripts/deploy-templates.sh

# 경로 확인 (UNBOX-BE 루트에서 실행해야 함)
ls templates/  # 서비스 템플릿들이 보여야 함
```

### 로컬 환경 시작 실패 시
```bash
# Docker 상태 확인
docker ps
docker-compose -f docker/local/docker-compose.yml logs

# 포트 충돌 확인
lsof -i :8081  # 해당 포트 사용 중인 프로세스 확인
```