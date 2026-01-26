# 데이터베이스 빠른 복구 (Quick Reference)

## 🚨 긴급 복구 (5분 안에)

### 1. Bastion 확인/생성
```bash
cd terraform/environments/dev
terraform apply -target=aws_instance.bastion -auto-approve
sleep 30  # PostgreSQL 설치 대기
```

### 2. 사용자만 재생성 (데이터베이스는 있는 경우)
```bash
./scripts/run_sql_on_rds.sh create_remaining_users.sql
```

### 3. 서비스 재시작
```bash
for service in user product trade order payment; do
  aws ecs update-service --cluster unbox-dev-cluster --service unbox-dev-$service --force-new-deployment --region ap-northeast-2
done
```

---

## 📋 완전 복구 (데이터베이스도 없는 경우)

### 1. 비밀번호 가져오기
```bash
aws ssm get-parameter --name /unbox/dev/user/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
aws ssm get-parameter --name /unbox/dev/product/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
aws ssm get-parameter --name /unbox/dev/trade/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
aws ssm get-parameter --name /unbox/dev/order/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
aws ssm get-parameter --name /unbox/dev/payment/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
```

### 2. SQL 파일 생성 (비밀번호 입력 필요)
```bash
cat > create_all_databases.sql << 'EOF'
CREATE DATABASE unbox_user;
CREATE DATABASE unbox_product;
CREATE DATABASE unbox_trade;
CREATE DATABASE unbox_order;
CREATE DATABASE unbox_payment;

CREATE USER unbox_user WITH PASSWORD '여기에_user_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_user TO unbox_user;

CREATE USER unbox_product WITH PASSWORD '여기에_product_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_product TO unbox_product;

CREATE USER unbox_trade WITH PASSWORD '여기에_trade_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_trade TO unbox_trade;

CREATE USER unbox_order WITH PASSWORD '여기에_order_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_order TO unbox_order;

CREATE USER unbox_payment WITH PASSWORD '여기에_payment_비밀번호';
GRANT ALL PRIVILEGES ON DATABASE unbox_payment TO unbox_payment;

\c unbox_user
GRANT ALL ON SCHEMA public TO unbox_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_user;

\c unbox_product
GRANT ALL ON SCHEMA public TO unbox_product;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_product;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_product;

\c unbox_trade
GRANT ALL ON SCHEMA public TO unbox_trade;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_trade;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_trade;

\c unbox_order
GRANT ALL ON SCHEMA public TO unbox_order;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_order;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_order;

\c unbox_payment
GRANT ALL ON SCHEMA public TO unbox_payment;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_payment;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_payment;
EOF
```

### 3. 실행
```bash
./scripts/run_sql_on_rds.sh create_all_databases.sql
```

### 4. 서비스 재시작
```bash
for service in user product trade order payment; do
  aws ecs update-service --cluster unbox-dev-cluster --service unbox-dev-$service --force-new-deployment --region ap-northeast-2
done
```

---

## 🔧 유용한 명령어

### 상태 확인
```bash
# 서비스 상태
aws ecs describe-services --cluster unbox-dev-cluster --services unbox-dev-user unbox-dev-product unbox-dev-trade unbox-dev-order unbox-dev-payment --query 'services[*].[serviceName,runningCount,desiredCount]' --output table

# RDS 접속
./scripts/connect_to_rds.sh

# 데이터베이스 목록
\l

# 사용자 목록
\du
```

### Bastion 관리
```bash
# 생성
cd terraform/environments/dev && terraform apply -target=aws_instance.bastion -auto-approve

# 삭제 (비용 절감)
cd terraform/environments/dev && terraform destroy -target=aws_instance.bastion -target=aws_security_group.bastion -target=aws_security_group_rule.rds_from_bastion -auto-approve

# IP 확인
cd terraform/environments/dev && terraform output bastion_public_ip
```

---

## 📞 문제 해결

### SSH 키가 없어요
```bash
aws ec2 create-key-pair --key-name unbox-bastion-temp --query 'KeyMaterial' --output text > ~/.ssh/unbox-bastion-aws.pem
chmod 400 ~/.ssh/unbox-bastion-aws.pem
```

### 연결이 안 돼요
```bash
# Bastion 재생성
cd terraform/environments/dev
terraform destroy -target=aws_instance.bastion -auto-approve
terraform apply -target=aws_instance.bastion -auto-approve
sleep 30
```

### 비밀번호를 모르겠어요
```bash
# RDS 마스터 비밀번호
echo "l[:9+q01Roc4cqM?"

# 서비스별 비밀번호
aws ssm get-parameter --name /unbox/dev/user/DB_PASSWORD --with-decryption --query 'Parameter.Value' --output text
```

---

**자세한 내용은 `DATABASE_RECOVERY_GUIDE.md`를 참고하세요!**
