# 데이터베이스 관리 스크립트

## 개요

Bastion Host를 통해 RDS에 접속하고 SQL을 실행하는 스크립트 모음입니다.

## 사전 요구사항

1. **Bastion Host가 생성되어 있어야 함**
   ```bash
   cd terraform/environments/dev
   terraform apply  # bastion.tf가 포함되어 있음
   ```

2. **SSH 키가 존재해야 함**
   - 위치: `~/.ssh/unbox-bastion-aws.pem`
   - 키가 없다면 AWS 콘솔에서 `unbox-bastion-temp` 키 페어를 다운로드하거나 재생성

## 스크립트 사용법

### 1. RDS 직접 접속

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

### 2. SQL 파일 실행

```bash
# SQL 파일을 RDS에서 실행
./scripts/run_sql_on_rds.sh create_remaining_users.sql
./scripts/run_sql_on_rds.sh create_databases.sql
```

## 데이터베이스 사용자 재생성

RDS를 재생성하거나 사용자가 삭제된 경우:

```bash
# 1. 데이터베이스 사용자 생성 스크립트 실행
./scripts/run_sql_on_rds.sh create_remaining_users.sql

# 2. 또는 직접 접속해서 수동으로 생성
./scripts/connect_to_rds.sh
```

## Bastion Host 관리

### Bastion 생성
```bash
cd terraform/environments/dev
terraform apply -target=aws_instance.bastion
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
terraform output bastion_connect_command
```

## 트러블슈팅

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

## 보안 주의사항

- Bastion Host는 개발 환경에서만 사용
- 프로덕션 환경에서는 AWS Systems Manager Session Manager 사용 권장
- 사용하지 않을 때는 Bastion Host를 삭제하여 비용 절감
- SSH 키는 절대 Git에 커밋하지 말 것 (`.gitignore`에 포함됨)
