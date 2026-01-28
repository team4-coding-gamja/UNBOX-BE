# 🛠 Scripts 디렉토리

프로젝트 관리 및 자동화를 위한 유틸리티 스크립트 모음입니다.

## 📋 스크립트 목록

### 데이터베이스 관리
- `connect_to_rds.sh` - Bastion을 통한 RDS 직접 접속
- `run_sql_on_rds.sh` - Bastion을 통한 SQL 파일 실행

### CI/CD 관리
- `generate-service-cicd.sh` - 새 서비스의 CI/CD 파일 자동 생성
- `trigger-common-rebuild.sh` - Common 모듈 변경 시 모든 서비스 재빌드

---

## 🗄 데이터베이스 관리 스크립트

### 사전 요구사항

1. **Bastion Host가 생성되어 있어야 함**
   ```bash
   cd terraform/environments/dev
   terraform apply -target=aws_instance.bastion -auto-approve
   ```

2. **SSH 키가 존재해야 함**
   - 위치: `~/.ssh/unbox-bastion-aws.pem`
   - 키가 없다면 새로 생성:
     ```bash
     aws ec2 create-key-pair --key-name unbox-bastion-temp \
       --query 'KeyMaterial' --output text > ~/.ssh/unbox-bastion-aws.pem
     chmod 400 ~/.ssh/unbox-bastion-aws.pem
     ```

### 1. RDS 직접 접속 (`connect_to_rds.sh`)

```bash
# postgres 데이터베이스에 접속 (기본값)
./scripts/connect_to_rds.sh

# 특정 데이터베이스에 접속
./scripts/connect_to_rds.sh unbox_user
./scripts/connect_to_rds.sh unbox_product
```

접속 후 일반적인 psql 명령어 사용 가능:
```sql
-- 데이터베이스 목록 확인
\l

-- 사용자 목록 확인
\du

-- 테이블 목록 확인
\dt

-- 특정 데이터베이스로 전환
\c unbox_product

-- 종료
\q
```

### 2. SQL 파일 실행 (`run_sql_on_rds.sh`)

```bash
# SQL 파일을 RDS에서 실행
./scripts/run_sql_on_rds.sh your_script.sql

# 예시
./scripts/run_sql_on_rds.sh create_remaining_users.sql
```

### 3. 데이터베이스 사용자 재생성

RDS를 재생성하거나 사용자가 삭제된 경우:

```bash
# 1. SSM에서 비밀번호 가져오기
aws ssm get-parameter --name /unbox/dev/user/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text

# 2. SQL 파일 생성 (비밀번호 입력)
cat > create_users.sql << 'EOF'
CREATE USER unbox_user WITH PASSWORD '여기에_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_user TO unbox_user;
-- 나머지 서비스도 동일하게...
EOF

# 3. SQL 실행
./scripts/run_sql_on_rds.sh create_users.sql
```

**참고**: 자세한 복구 절차는 [DATABASE_RECOVERY_GUIDE.md](../DATABASE_RECOVERY_GUIDE.md) 참조

---

## 🚀 CI/CD 관리 스크립트

### 1. 새 서비스 CI/CD 파일 생성 (`generate-service-cicd.sh`)

새로운 마이크로서비스를 추가할 때 CI/CD 파일을 자동으로 생성합니다.

```bash
# 사용법
./scripts/generate-service-cicd.sh <service-name> <port>

# 예시: notification 서비스 추가 (포트 8086)
./scripts/generate-service-cicd.sh notification 8086
```

**생성되는 파일**:
- `.github/workflows/notification-dev-ci.yml`
- `.github/workflows/notification-dev-cd.yml`
- `.github/workflows/notification-prod-ci.yml`
- `.github/workflows/notification-prod-cd.yml`

### 2. Common 모듈 변경 시 전체 재빌드 (`trigger-common-rebuild.sh`)

`unbox_common` 모듈을 수정한 후 모든 서비스를 재빌드해야 할 때 사용합니다.

```bash
# Dev 환경 전체 재빌드
./scripts/trigger-common-rebuild.sh dev

# Prod 환경 전체 재빌드
./scripts/trigger-common-rebuild.sh prod
```

**주의**: GitHub CLI (`gh`)가 설치되어 있어야 합니다.

---

## 🔧 Bastion Host 관리

### Bastion 생성
```bash
cd terraform/environments/dev
terraform apply -target=aws_instance.bastion -auto-approve
sleep 30  # PostgreSQL 클라이언트 설치 대기
```

### Bastion 삭제 (비용 절감)
```bash
cd terraform/environments/dev
terraform destroy \
  -target=aws_instance.bastion \
  -target=aws_security_group.bastion \
  -target=aws_security_group_rule.rds_from_bastion \
  -auto-approve
```

### Bastion 정보 확인
```bash
cd terraform/environments/dev
terraform output bastion_public_ip
```

---

## 🐛 트러블슈팅

### SSH 키 권한 오류
```bash
chmod 400 ~/.ssh/unbox-bastion-aws.pem
```

### Bastion에 PostgreSQL 클라이언트가 없는 경우
```bash
# Bastion에 SSH 접속
ssh -i ~/.ssh/unbox-bastion-aws.pem ec2-user@<BASTION_IP>

# PostgreSQL 클라이언트 설치
sudo yum install -y postgresql15
```

### 연결 타임아웃
- Bastion Host의 보안 그룹이 SSH(22번 포트)를 허용하는지 확인
- RDS 보안 그룹이 Bastion의 보안 그룹에서 5432 포트를 허용하는지 확인

### GitHub CLI 설치 (trigger-common-rebuild.sh 사용 시)
```bash
# macOS
brew install gh

# 인증
gh auth login
```

---

## 🔒 보안 주의사항

- Bastion Host는 개발 환경에서만 사용
- 프로덕션 환경에서는 AWS Systems Manager Session Manager 사용 권장
- 사용하지 않을 때는 Bastion Host를 삭제하여 비용 절감
- SSH 키는 절대 Git에 커밋하지 말 것 (`.gitignore`에 포함됨)
- 데이터베이스 비밀번호는 SSM Parameter Store에서 관리

---

## 📚 관련 문서

- [DATABASE_RECOVERY_GUIDE.md](../DATABASE_RECOVERY_GUIDE.md) - 데이터베이스 복구 상세 가이드
- [QUICK_DB_RECOVERY.md](../QUICK_DB_RECOVERY.md) - 빠른 복구 참조
- [DB_SETUP_INSTRUCTIONS.md](../DB_SETUP_INSTRUCTIONS.md) - 데이터베이스 초기 설정
