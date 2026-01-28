# 🔌 RDS 데이터베이스 접속 가이드

개발자들이 RDS PostgreSQL 데이터베이스에 직접 접속하여 데이터를 확인하는 방법입니다.

## 📋 접속 정보

### Dev 환경
- **Endpoint**: `unbox-dev-common-db.crmw2cqokxc4.ap-northeast-2.rds.amazonaws.com`
- **Port**: `5432`
- **Database**: `dev_db`
- **Username**: `unbox_admin`
- **Password**: AWS Secrets Manager에서 확인 (아래 참조)

### NAT 인스턴스 (Bastion Host)
- **Public IP**: `43.203.2.31`
- **Private IP**: `10.1.1.232`
- **접속 방법**: AWS Systems Manager Session Manager

> ⚠️ **중요**: RDS는 private 서브넷에 있어서 외부에서 직접 접속할 수 없습니다. NAT 인스턴스를 경유해야 합니다.

---

## 🔑 1. DB 비밀번호 확인

### AWS Console에서 확인
1. AWS Console 로그인
2. Secrets Manager 서비스로 이동
3. `unbox-dev-db-password` 시크릿 선택
4. "Retrieve secret value" 클릭
5. `password` 필드 값 복사

### AWS CLI로 확인
```bash
aws secretsmanager get-secret-value \
  --secret-id unbox-dev-db-password \
  --region ap-northeast-2 \
  --query 'SecretString' \
  --output text | jq -r '.password'
```

---

## 🚀 2. 접속 방법

### 방법 1: AWS Systems Manager Session Manager (권장)

Session Manager를 사용하면 SSH 키 없이도 NAT 인스턴스에 접속할 수 있습니다.

#### 사전 준비
```bash
# Session Manager 플러그인 설치 (Mac)
brew install --cask session-manager-plugin

# 설치 확인
session-manager-plugin --version
```

#### SSH 터널 생성
```bash
# NAT 인스턴스 ID 확인
NAT_INSTANCE_ID=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=unbox-dev-nat-instance" \
            "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text \
  --region ap-northeast-2)

echo "NAT Instance ID: $NAT_INSTANCE_ID"

# SSH 터널 생성 (로컬 5432 포트 → RDS 5432 포트)
aws ssm start-session \
  --target $NAT_INSTANCE_ID \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{
    \"host\":[\"unbox-dev-common-db.crmw2cqokxc4.ap-northeast-2.rds.amazonaws.com\"],
    \"portNumber\":[\"5432\"],
    \"localPortNumber\":[\"5432\"]
  }" \
  --region ap-northeast-2
```

> 💡 **터널이 열리면 새 터미널 창을 열어서 DB 클라이언트로 접속하세요.**

#### DB 접속
터널이 열린 상태에서 새 터미널에서:

```bash
# psql로 접속
psql -h localhost -p 5432 -U unbox_admin -d dev_db

# 비밀번호 입력 후 접속 완료
```

---

### 방법 2: DBeaver 사용 (GUI)

#### 1. SSH 터널 설정
위의 Session Manager 명령어로 터널을 먼저 생성합니다.

#### 2. DBeaver 연결 설정
1. **New Database Connection** 클릭
2. **PostgreSQL** 선택
3. **Main 탭 설정**:
   - Host: `localhost`
   - Port: `5432`
   - Database: `dev_db`
   - Username: `unbox_admin`
   - Password: (Secrets Manager에서 확인한 값)
4. **Test Connection** 클릭
5. **Finish** 클릭

---

### 방법 3: DataGrip / IntelliJ 사용

#### 1. SSH 터널 설정
위의 Session Manager 명령어로 터널을 먼저 생성합니다.

#### 2. DataGrip 연결 설정
1. **Database** 패널에서 **+** 클릭
2. **Data Source** → **PostgreSQL** 선택
3. **General 탭**:
   - Host: `localhost`
   - Port: `5432`
   - Database: `dev_db`
   - User: `unbox_admin`
   - Password: (Secrets Manager에서 확인한 값)
4. **Test Connection** 클릭
5. **OK** 클릭

---

### 방법 4: VS Code PostgreSQL Extension

#### 1. Extension 설치
- Extension: "PostgreSQL" by Chris Kolkman

#### 2. SSH 터널 설정
위의 Session Manager 명령어로 터널을 먼저 생성합니다.

#### 3. 연결 추가
1. PostgreSQL 아이콘 클릭
2. **Add Connection** 클릭
3. 정보 입력:
   - Hostname: `localhost`
   - Port: `5432`
   - Database: `dev_db`
   - Username: `unbox_admin`
   - Password: (Secrets Manager에서 확인한 값)
4. 연결 테스트

---

## 📊 3. 유용한 SQL 쿼리

### 데이터베이스 정보 확인
```sql
-- 현재 데이터베이스
SELECT current_database();

-- 모든 테이블 목록
SELECT table_schema, table_name 
FROM information_schema.tables 
WHERE table_schema NOT IN ('pg_catalog', 'information_schema')
ORDER BY table_schema, table_name;

-- 테이블별 레코드 수
SELECT 
  schemaname,
  tablename,
  n_live_tup as row_count
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC;
```

### 서비스별 데이터 확인
```sql
-- User 서비스
SELECT * FROM users LIMIT 10;
SELECT COUNT(*) FROM users;

-- Product 서비스
SELECT * FROM products LIMIT 10;
SELECT COUNT(*) FROM products;

-- Trade 서비스
SELECT * FROM trades LIMIT 10;
SELECT COUNT(*) FROM trades;

-- Order 서비스
SELECT * FROM orders LIMIT 10;
SELECT COUNT(*) FROM orders;

-- Payment 서비스
SELECT * FROM payments LIMIT 10;
SELECT COUNT(*) FROM payments;
```

### 연결 정보 확인
```sql
-- 현재 활성 연결 수
SELECT 
  datname,
  count(*) as connections
FROM pg_stat_activity
WHERE datname IS NOT NULL
GROUP BY datname;

-- 연결 상세 정보
SELECT 
  pid,
  usename,
  application_name,
  client_addr,
  state,
  query_start,
  state_change
FROM pg_stat_activity
WHERE datname = 'dev_db'
ORDER BY query_start DESC;
```

### 성능 모니터링
```sql
-- 느린 쿼리 확인
SELECT 
  pid,
  now() - query_start as duration,
  query,
  state
FROM pg_stat_activity
WHERE state != 'idle'
  AND now() - query_start > interval '1 second'
ORDER BY duration DESC;

-- 테이블 크기 확인
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 20;
```

---

## 🛠️ 4. 트러블슈팅

### 터널 연결 실패
```bash
# NAT 인스턴스 상태 확인
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=unbox-dev-nat-instance" \
  --query 'Reservations[0].Instances[0].[InstanceId,State.Name,PublicIpAddress]' \
  --output table \
  --region ap-northeast-2

# SSM Agent 상태 확인
aws ssm describe-instance-information \
  --filters "Key=InstanceIds,Values=$NAT_INSTANCE_ID" \
  --region ap-northeast-2
```

### DB 연결 실패
```bash
# RDS 상태 확인
aws rds describe-db-instances \
  --db-instance-identifier unbox-dev-common-db \
  --query 'DBInstances[0].[DBInstanceStatus,Endpoint.Address,Endpoint.Port]' \
  --output table \
  --region ap-northeast-2

# Security Group 확인
aws rds describe-db-instances \
  --db-instance-identifier unbox-dev-common-db \
  --query 'DBInstances[0].VpcSecurityGroups' \
  --region ap-northeast-2
```

### 비밀번호 오류
```bash
# 비밀번호 재확인
aws secretsmanager get-secret-value \
  --secret-id unbox-dev-db-password \
  --region ap-northeast-2 \
  --query 'SecretString' \
  --output text | jq -r '.password'
```

### 포트 충돌 (5432 포트가 이미 사용 중)
```bash
# 다른 포트 사용 (예: 15432)
aws ssm start-session \
  --target $NAT_INSTANCE_ID \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{
    \"host\":[\"unbox-dev-common-db.crmw2cqokxc4.ap-northeast-2.rds.amazonaws.com\"],
    \"portNumber\":[\"5432\"],
    \"localPortNumber\":[\"15432\"]
  }" \
  --region ap-northeast-2

# 접속 시 포트 변경
psql -h localhost -p 15432 -U unbox_admin -d dev_db
```

---

## ⚠️ 주의사항

### 1. 읽기 전용 권한
- 개발자 계정은 **읽기 전용** 권한만 있습니다
- `SELECT` 쿼리만 실행 가능
- `INSERT`, `UPDATE`, `DELETE`는 불가능

### 2. 성능 영향
- 무거운 쿼리는 서비스 성능에 영향을 줄 수 있습니다
- `LIMIT`을 사용하여 결과 수 제한
- 피크 시간대에는 조회 자제

### 3. 보안
- 비밀번호는 절대 공유하지 마세요
- 터널 사용 후 반드시 종료 (Ctrl+C)
- 로컬 환경에 비밀번호 저장 금지

### 4. 비용
- Session Manager는 무료
- 데이터 전송량에 따라 약간의 비용 발생 가능

---

## 📝 빠른 접속 스크립트

편의를 위해 스크립트를 만들어 사용할 수 있습니다:

```bash
# ~/bin/connect-dev-db.sh
#!/bin/bash

echo "🔍 NAT 인스턴스 ID 확인 중..."
NAT_INSTANCE_ID=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=unbox-dev-nat-instance" \
            "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text \
  --region ap-northeast-2)

if [ -z "$NAT_INSTANCE_ID" ]; then
  echo "❌ NAT 인스턴스를 찾을 수 없습니다."
  exit 1
fi

echo "✅ NAT Instance: $NAT_INSTANCE_ID"
echo "🚀 SSH 터널 생성 중..."
echo "💡 터널이 열리면 새 터미널에서 다음 명령어로 접속하세요:"
echo "   psql -h localhost -p 5432 -U unbox_admin -d dev_db"
echo ""

aws ssm start-session \
  --target $NAT_INSTANCE_ID \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{
    \"host\":[\"unbox-dev-common-db.crmw2cqokxc4.ap-northeast-2.rds.amazonaws.com\"],
    \"portNumber\":[\"5432\"],
    \"localPortNumber\":[\"5432\"]
  }" \
  --region ap-northeast-2
```

**사용법:**
```bash
# 실행 권한 부여
chmod +x ~/bin/connect-dev-db.sh

# 실행
~/bin/connect-dev-db.sh
```

---

## 🔗 관련 문서

- [DB 로그 확인 가이드](./DB_LOGS_GUIDE.md) - CloudWatch Logs에서 RDS 로그 확인
- [AWS Systems Manager Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html)
- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)

---

## 💬 문의

DB 접속 관련 문제가 있으면 DevOps 팀에 문의하세요.
