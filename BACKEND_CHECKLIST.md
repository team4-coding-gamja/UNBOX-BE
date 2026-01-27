# ✅ 백엔드 개발자 온보딩 체크리스트

> 새로운 백엔드 개발자가 개발을 시작하기 전에 완료해야 할 항목

---

## 📋 1단계: 계정 및 권한 설정

### AWS 접근 권한
- [ ] AWS 계정 생성 및 IAM 사용자 추가
- [ ] AWS CLI 설치 및 설정
  ```bash
  aws configure
  # Access Key ID: [관리자에게 받기]
  # Secret Access Key: [관리자에게 받기]
  # Region: ap-northeast-2
  ```
- [ ] AWS 접근 테스트
  ```bash
  aws sts get-caller-identity
  ```

### GitHub 접근 권한
- [ ] Repository 접근 권한 확인
- [ ] GitHub CLI 설치 및 인증
  ```bash
  brew install gh  # macOS
  gh auth login
  ```

### Discord
- [ ] 필수 채널 확인:
  - #CodingPotato

---

## 💻 2단계: 로컬 개발 환경 설정

### 필수 소프트웨어 설치
- [ ] **Java 17** 설치
  ```bash
  # macOS
  brew install openjdk@17
  
  # 버전 확인
  java -version  # 17.x.x 확인
  ```

- [ ] **Docker Desktop** 설치
  ```bash
  # macOS
  brew install --cask docker
  
  # 버전 확인
  docker --version
  docker-compose --version
  ```

- [ ] **IntelliJ IDEA** 설치 (권장)
  - Ultimate Edition (학생/회사 라이선스)
  - 또는 Community Edition

- [ ] **PostgreSQL 클라이언트** 설치 (선택)
  ```bash
  # macOS
  brew install postgresql@15
  ```

### 프로젝트 클론 및 설정
- [ ] 프로젝트 클론
  ```bash
  git clone https://github.com/team4-coding-gamja/UNBOX-BE.git
  cd UNBOX-BE
  ```

- [ ] Common 모듈 빌드
  ```bash
  cd unbox_common
  ./gradlew clean build publishToMavenLocal
  ```

- [ ] Docker Compose 실행
  ```bash
  cd ..
  docker-compose up -d
  ```

- [ ] 서비스 정상 동작 확인
  ```bash
  # User 서비스 헬스체크
  curl http://localhost:8081/actuator/health
  
  # 응답: {"status":"UP"}
  ```

### IntelliJ IDEA 설정
- [ ] 프로젝트 열기: `File > Open` → `UNBOX-BE` 폴더
- [ ] Gradle 동기화 완료 대기
- [ ] Java 17 SDK 설정 확인
  - `File > Project Structure > Project SDK`
- [ ] Lombok 플러그인 설치 및 활성화
  - `Preferences > Plugins > Lombok`
  - `Preferences > Build > Compiler > Annotation Processors` 체크
- [ ] Run Configuration 생성 (User 서비스 예시)
  - Main class: `com.unbox.user.UnboxUserApplication`
  - Active profiles: `local`

---

## 🗄 3단계: 데이터베이스 접근 확인

### 로컬 DB 접근
- [ ] PostgreSQL 접속 테스트
  ```bash
  docker exec -it unbox-postgres psql -U unbox_admin -d postgres
  ```
- [ ] 데이터베이스 목록 확인
  ```sql
  \l
  -- unbox_user, unbox_product 등 확인
  ```

### Dev 환경 DB 접근 (선택)
- [ ] SSH 키 받기 (관리자에게 요청)
  - 위치: `~/.ssh/unbox-bastion-aws.pem`
  - 권한 설정: `chmod 400 ~/.ssh/unbox-bastion-aws.pem`

- [ ] Bastion Host 생성 (필요시)
  ```bash
  cd terraform/environments/dev
  terraform apply -target=aws_instance.bastion -auto-approve
  ```

- [ ] RDS 접속 테스트
  ```bash
  ./scripts/connect_to_rds.sh
  ```

---

## 🔄 4단계: CI/CD 파이프라인 이해

### GitHub Actions 확인
- [ ] `.github/workflows/` 폴더 구조 확인
- [ ] 본인이 담당할 서비스의 워크플로우 파일 읽어보기
  - 예: `user-dev-ci.yml`, `user-dev-cd.yml`

### 배포 프로세스 이해
- [ ] Dev 배포 플로우 이해:
  1. `develop` 브랜치에 푸시
  2. CI: 빌드 → 테스트 → ECR 푸시
  3. CD: ECS 서비스 업데이트
  4. Discord 알림

- [ ] GitHub Actions 로그 확인 방법 숙지
  - Repository → Actions 탭

---

## 📚 5단계: 문서 읽기

### 필수 문서
- [ ] [README.md](./README.md) - 프로젝트 개요 및 아키텍처
- [ ] [BACKEND_ONBOARDING.md](./BACKEND_ONBOARDING.md) - 상세 온보딩 가이드
- [ ] [scripts/README.md](./scripts/README.md) - 유틸리티 스크립트 사용법

### 참고 문서
- [ ] [DATABASE_RECOVERY_GUIDE.md](./DATABASE_RECOVERY_GUIDE.md) - DB 복구 가이드
- [ ] [트러블슈팅 모음](./트러블슈팅_모음_2026-01-26.md) - 주요 이슈 해결 사례

### 코드 구조 파악
- [ ] `unbox_common` 모듈 구조 확인
  - Global Exception Handler
  - Security Filter
  - 공통 DTO
  - 분산 락 AOP

- [ ] 담당 서비스 코드 구조 파악
  - Controller
  - Service
  - Repository
  - Entity

---

## 🛠 6단계: 첫 번째 작업

### 간단한 테스트 작업
- [ ] 새 브랜치 생성
  ```bash
  git checkout -b test/my-first-branch
  ```

- [ ] 간단한 코드 수정 (예: 주석 추가)

- [ ] 로컬 테스트
  ```bash
  ./gradlew :unbox_user:test
  ```

- [ ] 커밋 및 푸시
  ```bash
  git add .
  git commit -m "test: verify development environment"
  git push origin test/my-first-branch
  ```

- [ ] Pull Request 생성 (연습용)
  - GitHub에서 `develop` 브랜치로 PR 생성
  - 팀원에게 리뷰 요청

---

## 🤝 7단계: 팀 소개 및 커뮤니케이션

### 팀 미팅
- [ ] 팀원들과 인사
- [ ] 데일리 스탠드업 시간 확인
- [ ] 스프린트 일정 확인

### 커뮤니케이션 채널
- [ ] Discord 채널 활용법 숙지
- [ ] 질문하는 방법 이해
  1. 문서 먼저 확인
  2. Discord에 질문
  3. 필요시 1:1 문의

### 코드 리뷰 프로세스
- [ ] 코드 리뷰 가이드라인 확인
- [ ] PR 템플릿 확인
- [ ] 리뷰 요청 방법 숙지

---

## ✨ 완료!

모든 항목을 체크했다면 개발을 시작할 준비가 되었습니다! 🎉

### 다음 단계
1. 팀 리더에게 온보딩 완료 보고
2. 첫 번째 이슈 할당 받기
3. 개발 시작!

### 도움이 필요하면?
- Discord #dev-backend 채널에 질문
- 팀 리더 또는 멘토에게 문의
- [BACKEND_ONBOARDING.md](./BACKEND_ONBOARDING.md) 참고

---

**체크리스트 완료 날짜**: _______________

**담당자 서명**: _______________

**멘토 확인**: _______________
