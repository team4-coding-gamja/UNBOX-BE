# 데이터베이스 복구 가이드

RDS를 재생성하거나 데이터베이스/사용자가 삭제된 경우 복구하는 방법입니다.

## 📋 상황별 복구 시나리오

### 시나리오 1: RDS를 완전히 재생성한 경우
- 데이터베이스도 없고, 사용자도 없는 상태
- **필요한 작업**: 데이터베이스 생성 + 사용자 생성

### 시나리오 2: 데이터베이스는 있지만 사용자만 삭제된 경우
- 데이터베이스는 존재하지만 접근 권한이 없는 상태
- **필요한 작업**: 사용자 생성만

---

## 🚀 복구 절차

### STEP 1: SSH 키 확인

```bash
# SSH 키가 있는지 확인
ls -la ~/.ssh/unbox-bastion-aws.pem
```

**키가 없다면:**

```bash
# AWS에서 키 페어 생성 및 다운로드
aws ec2 create-key-pair \
  --key-name unbox-bastion-temp \
  --query 'KeyMaterial' \
  --output text > ~/.ssh/unbox-bastion-aws.pem

# 권한 설정
chmod 400 ~/.ssh/unbox-bastion-aws.pem
```

---

### STEP 2: Bastion Host 확인 및 생성

```bash
# Bastion Host가 실행 중인지 확인
cd terraform/environments/dev
terraform output bastion_public_ip
```

**Bastion이 없거나 중지된 경우:**

```bash
# Bastion Host 생성
cd terraform/environments/dev
terraform apply -target=aws_instance.bastion \
                -target=aws_security_group.bastion \
                -target=aws_iam_role.bastion_role \
                -target=aws_iam_role_policy_attachment.bastion_ssm \
                -target=aws_iam_instance_profile.bastion_profile \
                -target=aws_security_group_rule.rds_from_bastion \
                -auto-approve

# 생성 완료 후 PostgreSQL 설치 대기 (약 30초)
sleep 30
```

---

### STEP 3: 데이터베이스 상태 확인

```bash
# RDS에 접속해서 현재 상태 확인
./scripts/connect_to_rds.sh
```

접속 후 다음 명령어로 확인:

```sql
-- 데이터베이스 목록 확인
\l

-- 사용자 목록 확인
\du

-- 종료
\q
```

**확인 사항:**
- `unbox_user`, `unbox_product`, `unbox_trade`, `unbox_order`, `unbox_payment` 데이터베이스가 있는가?
- `unbox_user`, `unbox_product`, `unbox_trade`, `unbox_order`, `unbox_payment` 사용자가 있는가?

---

### STEP 4-A: 데이터베이스가 없는 경우 (완전 재생성)

#### 4-A-1. 데이터베이스 생성 SQL 작성

```bash
cat > create_all_databases.sql << 'EOF'
-- 데이터베이스 생성
CREATE DATABASE unbox_user;
CREATE DATABASE unbox_product;
CREATE DATABASE unbox_trade;
CREATE DATABASE unbox_order;
CREATE DATABASE unbox_payment;

-- User 사용자 생성 및 권한
CREATE USER unbox_user WITH PASSWORD 'SSM에서_가져온_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_user TO unbox_user;

-- Product 사용자 생성 및 권한
CREATE USER unbox_product WITH PASSWORD 'SSM에서_가져온_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_product TO unbox_product;

-- Trade 사용자 생성 및 권한
CREATE USER unbox_trade WITH PASSWORD 'SSM에서_가져온_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_trade TO unbox_trade;

-- Order 사용자 생성 및 권한
CREATE USER unbox_order WITH PASSWORD 'SSM에서_가져온_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_order TO unbox_order;

-- Payment 사용자 생성 및 권한
CREATE USER unbox_payment WITH PASSWORD 'SSM에서_가져온_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_payment TO unbox_payment;

-- User 스키마 권한
\c unbox_user
GRANT ALL ON SCHEMA public TO unbox_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_user;

-- Product 스키마 권한
\c unbox_product
GRANT ALL ON SCHEMA public TO unbox_product;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_product;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_product;

-- Trade 스키마 권한
\c unbox_trade
GRANT ALL ON SCHEMA public TO unbox_trade;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_trade;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_trade;

-- Order 스키마 권한
\c unbox_order
GRANT ALL ON SCHEMA public TO unbox_order;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_order;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_order;

-- Payment 스키마 권한
\c unbox_payment
GRANT ALL ON SCHEMA public TO unbox_payment;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_payment;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_payment;
EOF
```

#### 4-A-2. SSM에서 비밀번호 가져오기

```bash
# 각 서비스의 비밀번호 확인
aws ssm get-parameter --name /unbox/dev/user/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
aws ssm get-parameter --name /unbox/dev/product/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
aws ssm get-parameter --name /unbox/dev/trade/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
aws ssm get-parameter --name /unbox/dev/order/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
aws ssm get-parameter --name /unbox/dev/payment/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
```

#### 4-A-3. SQL 파일에 비밀번호 입력

위에서 가져온 비밀번호를 `create_all_databases.sql` 파일의 해당 위치에 입력하세요.

#### 4-A-4. SQL 실행

```bash
./scripts/run_sql_on_rds.sh create_all_databases.sql
```

---

### STEP 4-B: 데이터베이스는 있지만 사용자만 없는 경우

이미 `create_remaining_users.sql` 파일이 있으므로 바로 실행:

```bash
./scripts/run_sql_on_rds.sh create_remaining_users.sql
```

**파일이 없다면 다시 생성:**

```bash
cat > create_remaining_users.sql << 'EOF'
-- Product 사용자 생성
CREATE USER unbox_product WITH PASSWORD 'v_%hHva&jt=_:aM';
GRANT ALL PRIVILEGES ON DATABASE unbox_product TO unbox_product;

-- Trade 사용자 생성
CREATE USER unbox_trade WITH PASSWORD 'HQWMgXIS{B+Ql+Uh';
GRANT ALL PRIVILEGES ON DATABASE unbox_trade TO unbox_trade;

-- Order 사용자 생성
CREATE USER unbox_order WITH PASSWORD 'v[qyB{ahI!Ql+Uh';
GRANT ALL PRIVILEGES ON DATABASE unbox_order TO unbox_order;

-- Payment 사용자 생성
CREATE USER unbox_payment WITH PASSWORD 'BPandb4Stq(z>Q07';
GRANT ALL PRIVILEGES ON DATABASE unbox_payment TO unbox_payment;

-- Product 스키마 권한
\c unbox_product
GRANT ALL ON SCHEMA public TO unbox_product;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_product;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_product;

-- Trade 스키마 권한
\c unbox_trade
GRANT ALL ON SCHEMA public TO unbox_trade;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_trade;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_trade;

-- Order 스키마 권한
\c unbox_order
GRANT ALL ON SCHEMA public TO unbox_order;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_order;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_order;

-- Payment 스키마 권한
\c unbox_payment
GRANT ALL ON SCHEMA public TO unbox_payment;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_payment;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_payment;
EOF

./scripts/run_sql_on_rds.sh create_remaining_users.sql
```

---

### STEP 5: ECS 서비스 재시작

데이터베이스 복구 후 애플리케이션이 다시 연결되도록 ECS 서비스를 재시작:

```bash
cd terraform/environments/dev

# 모든 서비스 강제 재배포
aws ecs update-service \
  --cluster unbox-dev-cluster \
  --service unbox-dev-user \
  --force-new-deployment \
  --region ap-northeast-2

aws ecs update-service \
  --cluster unbox-dev-cluster \
  --service unbox-dev-product \
  --force-new-deployment \
  --region ap-northeast-2

aws ecs update-service \
  --cluster unbox-dev-cluster \
  --service unbox-dev-trade \
  --force-new-deployment \
  --region ap-northeast-2

aws ecs update-service \
  --cluster unbox-dev-cluster \
  --service unbox-dev-order \
  --force-new-deployment \
  --region ap-northeast-2

aws ecs update-service \
  --cluster unbox-dev-cluster \
  --service unbox-dev-payment \
  --force-new-deployment \
  --region ap-northeast-2
```

또는 한 번에:

```bash
for service in user product trade order payment; do
  aws ecs update-service \
    --cluster unbox-dev-cluster \
    --service unbox-dev-$service \
    --force-new-deployment \
    --region ap-northeast-2
done
```

---

### STEP 6: 서비스 상태 확인

```bash
# 모든 서비스 상태 확인
aws ecs describe-services \
  --cluster unbox-dev-cluster \
  --services unbox-dev-user unbox-dev-product unbox-dev-trade unbox-dev-order unbox-dev-payment \
  --query 'services[*].[serviceName,runningCount,desiredCount]' \
  --output table
```

**정상 상태:**
- `runningCount` = `desiredCount` = 1

**문제가 있다면:**

```bash
# 특정 서비스의 태스크 로그 확인
aws ecs list-tasks --cluster unbox-dev-cluster --service-name unbox-dev-product --query 'taskArns[0]' --output text

# 태스크 ARN을 복사해서 로그 확인
aws logs tail /ecs/unbox-dev/product --follow
```

---

### STEP 7: 정리 (선택사항)

비용 절감을 위해 Bastion Host를 삭제할 수 있습니다:

```bash
cd terraform/environments/dev

terraform destroy \
  -target=aws_instance.bastion \
  -target=aws_security_group.bastion \
  -target=aws_security_group_rule.rds_from_bastion \
  -auto-approve
```

**주의:** Bastion을 삭제하면 다음에 다시 생성해야 합니다.

---

## 🔍 트러블슈팅

### 문제 1: SSH 접속 실패

```bash
# SSH 키 권한 확인
chmod 400 ~/.ssh/unbox-bastion-aws.pem

# Bastion IP 확인
cd terraform/environments/dev
terraform output bastion_public_ip

# 보안 그룹 확인 (SSH 22번 포트가 열려있는지)
aws ec2 describe-security-groups \
  --group-ids $(terraform output -raw bastion_security_group_id) \
  --query 'SecurityGroups[0].IpPermissions'
```

### 문제 2: PostgreSQL 클라이언트가 없음

```bash
# Bastion에 접속
ssh -i ~/.ssh/unbox-bastion-aws.pem ec2-user@<BASTION_IP>

# PostgreSQL 설치
sudo yum install -y postgresql15

# 설치 확인
psql --version
```

### 문제 3: RDS 연결 타임아웃

```bash
# RDS 보안 그룹이 Bastion에서의 접근을 허용하는지 확인
cd terraform/environments/dev
terraform state show aws_security_group_rule.rds_from_bastion
```

### 문제 4: 비밀번호가 틀림

```bash
# SSM에서 최신 비밀번호 확인
aws ssm get-parameter \
  --name /unbox/dev/user/DB_PASSWORD \
  --with-decryption \
  --query 'Parameter.Value' \
  --output text
```

---

## 📝 중요 정보

### RDS 마스터 비밀번호
```
l[:9+q01Roc4cqM?
```

### 서비스별 데이터베이스 비밀번호 (SSM에 저장됨)
```bash
# User
aws ssm get-parameter --name /unbox/dev/user/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text

# Product
aws ssm get-parameter --name /unbox/dev/product/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text

# Trade
aws ssm get-parameter --name /unbox/dev/trade/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text

# Order
aws ssm get-parameter --name /unbox/dev/order/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text

# Payment
aws ssm get-parameter --name /unbox/dev/payment/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
```

### RDS 엔드포인트
```bash
cd terraform/environments/dev
terraform output rds_endpoints
```

---

## ✅ 체크리스트

복구 작업 전에 확인:

- [ ] SSH 키가 있는가? (`~/.ssh/unbox-bastion-aws.pem`)
- [ ] Bastion Host가 실행 중인가?
- [ ] RDS가 실행 중인가?
- [ ] SSM에서 비밀번호를 가져왔는가?
- [ ] SQL 파일에 올바른 비밀번호를 입력했는가?

복구 작업 후 확인:

- [ ] 데이터베이스가 생성되었는가? (`\l`)
- [ ] 사용자가 생성되었는가? (`\du`)
- [ ] ECS 서비스가 정상 실행 중인가?
- [ ] 애플리케이션 로그에 DB 연결 오류가 없는가?
- [ ] Health Check가 통과하는가?

---

## 📞 도움이 필요하면

1. 이 가이드를 처음부터 다시 따라해보세요
2. 각 단계의 출력 결과를 확인하세요
3. 트러블슈팅 섹션을 참고하세요
4. 그래도 안 되면 Kiro에게 물어보세요!
