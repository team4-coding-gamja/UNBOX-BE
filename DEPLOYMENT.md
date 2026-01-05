# 📦 UNBOX MVP 배포 가이드

> **안전하고 신뢰할 수 있는 한정판 C2C 거래 플랫폼, UNBOX**  
> Team 4. 코딩감자

## 🎯 실행 환경

- **테스트 환경**: H2 인메모리 DB + Redis (로컬 개발/테스트용)
- **실전 환경**: RDS PostgreSQL + Redis (MVP 발표용)

---

## 🧪 테스트 환경 실행 (H2 DB)

### 📋 사전 준비

- Docker & Docker Compose 설치
- Java 17 설치
- Git 설치
- jq 설치 (JSON 파싱용): `brew install jq`

### 1단계: 프로젝트 클론 및 이동

```bash
git clone https://github.com/your-repo/UNBOX-BE.git
cd UNBOX-BE
```

### 2단계: 환경변수 설정

```bash
# 테스트용 환경변수 적용
cp .env.test .env

# 환경변수 로드 (터미널에서 사용하기 위해)
source .env

# 설정 확인
echo "Base URL: $BASE_URL"
echo "Test Email: $TEST_EMAIL"
```

### 3단계: 애플리케이션 빌드

```bash
# Gradle로 JAR 파일 생성
./gradlew clean build

# 빌드 성공 확인
ls -la build/libs/
# *.jar 파일이 생성되었는지 확인
```

### 4단계: Docker 이미지 빌드

```bash
# Docker 이미지 빌드
docker build -t unbox-app .

# 이미지 생성 확인
docker images | grep unbox-app
```

### 5단계: 테스트 환경 실행

```bash
# 테스트용 Docker Compose 실행 (H2 + Redis)
docker-compose -f docker-compose-test.yml up -d

# 컨테이너 상태 확인
docker ps
# unbox-test-app, unbox-test-redis 2개 컨테이너 실행 확인
```

### 6단계: 애플리케이션 확인

#### **기본 헬스체크**
```bash
# 헬스체크
curl $BASE_URL/actuator/health
# 응답: {"status":"UP"}
```

#### **환경변수를 활용한 API 테스트**
```bash
# 회원가입 테스트
curl -X POST $BASE_URL/api/auth/signup \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\",\"name\":\"$TEST_NAME\"}"

# 로그인 및 토큰 저장
TOKEN=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}" \
  | jq -r '.data.accessToken')

echo "JWT Token: ${TOKEN:0:20}..."

# 인증이 필요한 API 테스트
curl -X GET $BASE_URL/api/users/me \
  -H "Authorization: Bearer $TOKEN"

curl -X GET $BASE_URL/api/products?page=0&size=10 \
  -H "Authorization: Bearer $TOKEN"

curl -X GET $BASE_URL/api/orders \
  -H "Authorization: Bearer $TOKEN"

curl -X GET $BASE_URL/api/reviews \
  -H "Authorization: Bearer $TOKEN"

curl -X GET $BASE_URL/api/selling-bids \
  -H "Authorization: Bearer $TOKEN"

curl -X GET $BASE_URL/api/wishlist \
  -H "Authorization: Bearer $TOKEN"

# 관리자 API 테스트
curl -X GET $BASE_URL/api/admin/products \
  -H "Authorization: Bearer $TOKEN"

curl -X GET $BASE_URL/api/admin/users \
  -H "Authorization: Bearer $TOKEN"
```

### 7단계: 자동화 테스트 스크립트 (선택사항)

```bash
# 테스트 스크립트 생성
cat > test_api.sh << 'EOF'
#!/bin/bash

# 환경변수 로드
source .env

echo "🧪 UNBOX API 테스트 시작"
echo "Base URL: $BASE_URL"

# 헬스체크
echo "📋 헬스체크..."
health_response=$(curl -s $BASE_URL/actuator/health)
echo $health_response | jq '.'

# 회원가입
echo "👤 회원가입..."
signup_response=$(curl -s -X POST $BASE_URL/api/auth/signup \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\",\"name\":\"$TEST_NAME\"}")
echo $signup_response | jq '.'

# 로그인
echo "🔐 로그인..."
login_response=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")

TOKEN=$(echo $login_response | jq -r '.data.accessToken')

if [ "$TOKEN" != "null" ] && [ "$TOKEN" != "" ]; then
    echo "✅ 로그인 성공! 토큰: ${TOKEN:0:20}..."
    
    # 사용자 정보 조회
    echo "👤 사용자 정보 조회..."
    curl -s -X GET $BASE_URL/api/users/me \
      -H "Authorization: Bearer $TOKEN" | jq '.'
    
    # 상품 목록 조회
    echo "📦 상품 목록 조회..."
    curl -s -X GET "$BASE_URL/api/products?page=0&size=5" \
      -H "Authorization: Bearer $TOKEN" | jq '.'
    
    echo "✅ 모든 테스트 완료!"
else
    echo "❌ 로그인 실패"
    echo $login_response | jq '.'
fi
EOF

# 실행 권한 부여 및 실행
chmod +x test_api.sh
./test_api.sh
```

### 8단계: H2 데이터베이스 콘솔 접속

**브라우저에서 접속:**
```
URL: http://localhost:8080/h2-console

설정값:
- JDBC URL: jdbc:h2:mem:testdb
- User Name: sa  
- Password: (비워둠)
```

### 9단계: 로그 확인

```bash
# 애플리케이션 로그 확인
docker logs unbox-test-app

# 실시간 로그 모니터링
docker logs -f unbox-test-app

# 로그 파일 확인
cat logs/test-application.log
```

### 10단계: 테스트 완료 후 정리

```bash
# 컨테이너 중지 및 삭제
docker-compose -f docker-compose-test.yml down

# 볼륨까지 완전 삭제 (선택사항)
docker-compose -f docker-compose-test.yml down -v

# 테스트 스크립트 정리
rm -f test_api.sh
```

---

## 🚀 실전 환경 실행 (RDS PostgreSQL)

### 📋 사전 준비

- AWS CLI 설치 및 자격증명 설정
- Terraform 설치
- SSH 키페어 생성
- jq 설치: `brew install jq`

### 1단계: SSH 키페어 생성

```bash
# SSH 키 생성
ssh-keygen -t rsa -b 4096 -f ~/.ssh/unbox-key

# 공개키 확인
cat ~/.ssh/unbox-key.pub
```

### 2단계: Terraform 설정

```bash
cd terraform/environments/dev

# terraform.tfvars 파일 생성
cp terraform.tfvars.example terraform.tfvars

# terraform.tfvars 파일 편집
# public_key = "ssh-rsa AAAAB3NzaC1yc2E... (위에서 확인한 공개키 내용)"
```

### 3단계: AWS 인프라 생성

```bash
# Terraform 초기화
terraform init

# 실행 계획 확인
terraform plan -var="use_rds=true"

# RDS 포함 인프라 생성
terraform apply -var="use_rds=true"
# yes 입력하여 승인
```

### 4단계: 인프라 정보 확인 및 환경변수 업데이트

```bash
# RDS 엔드포인트 확인
RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
echo "RDS Endpoint: $RDS_ENDPOINT"

# EC2 퍼블릭 IP 확인
EC2_IP=$(terraform output -raw ec2_public_ip)
echo "EC2 Public IP: $EC2_IP"

# 프로젝트 루트로 이동
cd ../../../

# .env.mvp 파일의 플레이스홀더를 실제 값으로 교체
sed -i "s/RDS_ENDPOINT_HERE/$RDS_ENDPOINT/g" .env.mvp
sed -i "s/your-rds-endpoint.ap-northeast-2.rds.amazonaws.com/$RDS_ENDPOINT/g" .env.mvp
sed -i "s/EC2-PUBLIC-IP/$EC2_IP/g" .env.mvp

# 업데이트된 내용 확인
echo "업데이트된 .env.mvp 파일:"
cat .env.mvp
```

### 5단계: 로컬에서 애플리케이션 빌드

```bash
# JAR 파일 빌드
./gradlew clean build

# Docker 이미지 빌드 (선택사항 - EC2에서도 가능)
docker build -t unbox-app .
```

### 6단계: EC2에 배포

```bash
# EC2 접속
ssh -i ~/.ssh/unbox-key.pem ec2-user@$EC2_IP

# 프로젝트 클론 (EC2에서 실행)
git clone https://github.com/team4-coding-gamja/UNBOX-BE.git
cd UNBOX-BE

# 환경변수 파일 업로드 (로컬에서 실행)
scp -i ~/.ssh/unbox-key.pem .env.mvp ec2-user@$EC2_IP:~/UNBOX-BE/.env

# JAR 파일 업로드 (로컬에서 실행)
scp -i ~/.ssh/unbox-key.pem build/libs/*.jar ec2-user@$EC2_IP:~/UNBOX-BE/build/libs/

# Docker 이미지 빌드 (EC2에서 실행)
docker build -t unbox-app .
```

### 7단계: 실전 환경 실행

```bash
# EC2에서 실행
# MVP용 Docker Compose 실행
docker-compose -f docker-compose-mvp.yml up -d

# 컨테이너 상태 확인
docker ps

# 애플리케이션 로그 확인
docker logs unbox-mvp-app
```

### 8단계: 로컬에서 API 테스트

```bash
# 로컬에서 실행
# MVP 환경변수 로드
cp .env.mvp .env
source .env

echo "🚀 MVP 환경 API 테스트 시작"
echo "Base URL: $BASE_URL"

# 헬스체크
echo "📋 헬스체크..."
curl $BASE_URL/actuator/health | jq '.'

# 회원가입
echo "👤 회원가입..."
signup_response=$(curl -s -X POST $BASE_URL/api/auth/signup \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$DEMO_EMAIL\",\"password\":\"$DEMO_PASSWORD\",\"name\":\"$DEMO_NAME\"}")
echo $signup_response | jq '.'

# 로그인 및 토큰 저장
echo "🔐 로그인..."
TOKEN=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$DEMO_EMAIL\",\"password\":\"$DEMO_PASSWORD\"}" \
  | jq -r '.data.accessToken')

if [ "$TOKEN" != "null" ] && [ "$TOKEN" != "" ]; then
    echo "✅ 로그인 성공! 토큰: ${TOKEN:0:20}..."
    
    # 사용자 정보 조회
    echo "👤 사용자 정보 조회..."
    curl -s -X GET $BASE_URL/api/users/me \
      -H "Authorization: Bearer $TOKEN" | jq '.'
    
    # 상품 목록 조회
    echo "📦 상품 목록 조회..."
    curl -s -X GET "$BASE_URL/api/products?page=0&size=10" \
      -H "Authorization: Bearer $TOKEN" | jq '.'
    
    # 주문 목록 조회
    echo "📋 주문 목록 조회..."
    curl -s -X GET $BASE_URL/api/orders \
      -H "Authorization: Bearer $TOKEN" | jq '.'
    
    # 위시리스트 조회
    echo "❤️ 위시리스트 조회..."
    curl -s -X GET $BASE_URL/api/wishlist \
      -H "Authorization: Bearer $TOKEN" | jq '.'
    
    echo "✅ MVP 환경 테스트 완료!"
else
    echo "❌ 로그인 실패"
    echo $signup_response | jq '.'
fi
```

### 9단계: 자동화 MVP 테스트 스크립트

```bash
# MVP 테스트 스크립트 생성
cat > mvp_test.sh << 'EOF'
#!/bin/bash

# 색상 출력 함수
print_success() { echo -e "\033[32m✅ $1\033[0m"; }
print_error() { echo -e "\033[31m❌ $1\033[0m"; }
print_info() { echo -e "\033[34mℹ️  $1\033[0m"; }

# 환경변수 로드
source .env

print_info "🚀 UNBOX MVP API 테스트 시작"
print_info "Base URL: $BASE_URL"
print_info "Demo Email: $DEMO_EMAIL"

# 헬스체크
print_info "헬스체크 실행 중..."
health_response=$(curl -s -w "%{http_code}" $BASE_URL/actuator/health)
http_code="${health_response: -3}"
body="${health_response%???}"

if [ "$http_code" == "200" ]; then
    print_success "헬스체크 성공"
    echo $body | jq '.'
else
    print_error "헬스체크 실패 (HTTP $http_code)"
    echo $body
    exit 1
fi

# 회원가입
print_info "회원가입 시도 중..."
signup_response=$(curl -s -X POST $BASE_URL/api/auth/signup \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$DEMO_EMAIL\",\"password\":\"$DEMO_PASSWORD\",\"name\":\"$DEMO_NAME\"}")

echo "회원가입 응답:"
echo $signup_response | jq '.'

# 로그인
print_info "로그인 시도 중..."
login_response=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$DEMO_EMAIL\",\"password\":\"$DEMO_PASSWORD\"}")

TOKEN=$(echo $login_response | jq -r '.data.accessToken')

if [ "$TOKEN" != "null" ] && [ "$TOKEN" != "" ]; then
    print_success "로그인 성공!"
    print_info "토큰: ${TOKEN:0:20}..."
    
    # API 테스트 배열
    declare -a apis=(
        "GET:/api/users/me:사용자 정보"
        "GET:/api/products?page=0&size=5:상품 목록"
        "GET:/api/orders:주문 목록"
        "GET:/api/reviews:리뷰 목록"
        "GET:/api/selling-bids:거래 목록"
        "GET:/api/wishlist:위시리스트"
        "GET:/api/admin/products:관리자 상품"
        "GET:/api/admin/users:관리자 사용자"
    )
    
    # API 테스트 실행
    for api in "${apis[@]}"; do
        IFS=':' read -r method endpoint description <<< "$api"
        print_info "$description 조회 중..."
        
        response=$(curl -s -X $method "$BASE_URL$endpoint" \
          -H "Authorization: Bearer $TOKEN")
        
        echo $response | jq '.' 2>/dev/null || echo $response
        echo "---"
    done
    
    print_success "모든 API 테스트 완료!"
else
    print_error "로그인 실패"
    echo "로그인 응답:"
    echo $login_response | jq '.'
    exit 1
fi
EOF

# 실행 권한 부여 및 실행
chmod +x mvp_test.sh
./mvp_test.sh
```

### 10단계: 발표 완료 후 정리 (선택사항)

```bash
# 비용 절약을 위해 RDS 삭제
cd terraform/environments/dev
terraform apply -var="use_rds=false"

# 또는 전체 인프라 삭제
terraform destroy

# 테스트 스크립트 정리
rm -f mvp_test.sh
```

---

## 🔧 문제 해결

### 환경변수 관련 문제
```bash
# 환경변수 확인
echo "BASE_URL: $BASE_URL"
echo "TEST_EMAIL: $TEST_EMAIL"
echo "TOKEN: ${TOKEN:0:20}..."

# 환경변수 재로드
source .env

# .env 파일 내용 확인
cat .env
```

### 포트 충돌 시
```bash
# 포트 사용 프로세스 확인
lsof -i :8080  # 테스트 환경
lsof -i :80    # 실전 환경

# 해당 프로세스 종료 후 재실행
```

### JWT 토큰 관련 문제
```bash
# 토큰 유효성 확인
if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo "토큰이 없습니다. 다시 로그인하세요."
    # 로그인 재시도
fi

# 토큰 만료 시 재로그인
TOKEN=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}" \
  | jq -r '.data.accessToken')
```

### Docker 실행 실패 시
```bash
# 기존 컨테이너 정리
docker-compose -f docker-compose-test.yml down  # 테스트
docker-compose -f docker-compose-mvp.yml down   # 실전

# Docker 시스템 정리
docker system prune -f

# 이미지 재빌드
docker build -t unbox-app . --no-cache
```

---

## 📊 환경별 비교

| 구분 | 테스트 환경 | 실전 환경 |
|------|-------------|-----------|
| **데이터베이스** | H2 인메모리 | RDS PostgreSQL |
| **포트** | 8080 | 80 |
| **접속 URL** | `localhost:8080` | `EC2-PUBLIC-IP` |
| **환경변수 파일** | `.env.test` | `.env.mvp` |
| **API 테스트 계정** | `test@example.com` | `demo@unbox.com` |
| **데이터 지속성** | ❌ (재시작 시 삭제) | ✅ (영구 저장) |
| **비용** | $0 | $8-10/월 |

---

## ✅ 체크리스트

### 테스트 환경
- [ ] `.env.test` → `.env` 복사 완료
- [ ] `source .env` 실행하여 환경변수 로드
- [ ] JAR 파일 빌드 성공
- [ ] Docker 이미지 빌드 성공
- [ ] 컨테이너 2개 실행 중
- [ ] `curl $BASE_URL/actuator/health` 응답 OK
- [ ] 환경변수를 활용한 API 테스트 성공
- [ ] H2 콘솔 접속 가능

### 실전 환경
- [ ] SSH 키페어 생성 완료
- [ ] Terraform 인프라 생성 완료
- [ ] `.env.mvp` 파일의 플레이스홀더 실제 값으로 교체
- [ ] EC2에 애플리케이션 배포 완료
- [ ] `source .env` 실행하여 환경변수 로드
- [ ] `curl $BASE_URL/actuator/health` 응답 OK
- [ ] 환경변수를 활용한 MVP API 테스트 성공

---

## 📞 지원

문제가 발생하면 팀 슬랙 채널 또는 GitHub Issues를 통해 문의해주세요.

**Team 4. 코딩감자** 🥔