#!/bin/bash

# RDS에서 SQL 파일 실행 스크립트 (Bastion Host 경유)
# 사용법: ./scripts/run_sql_on_rds.sh <sql_file>

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <sql_file>"
    echo "Example: $0 create_remaining_users.sql"
    exit 1
fi

SQL_FILE="$1"

if [ ! -f "$SQL_FILE" ]; then
    echo "❌ SQL file not found: $SQL_FILE"
    exit 1
fi

BASTION_IP=$(cd terraform/environments/dev && terraform output -raw bastion_public_ip)
RDS_ENDPOINT=$(cd terraform/environments/dev && terraform output -raw rds_endpoints | jq -r '.common' | cut -d: -f1)
SSH_KEY="$HOME/.ssh/unbox-bastion-aws.pem"

echo "🚀 Running SQL file on RDS via Bastion Host..."
echo "   SQL File: $SQL_FILE"
echo "   Bastion IP: $BASTION_IP"
echo "   RDS Endpoint: $RDS_ENDPOINT"
echo ""

# SSH 키 권한 확인
if [ ! -f "$SSH_KEY" ]; then
    echo "❌ SSH key not found: $SSH_KEY"
    echo "   Please ensure the Bastion Host is created with the key pair."
    exit 1
fi

chmod 400 "$SSH_KEY"

# SQL 파일을 Bastion에 복사
echo "📤 Uploading SQL file to Bastion..."
scp -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SQL_FILE" ec2-user@$BASTION_IP:/tmp/

# SQL 실행
echo "⚡ Executing SQL..."
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@$BASTION_IP \
    "PGPASSWORD='l[:9+q01Roc4cqM?' psql -h $RDS_ENDPOINT -U unbox_admin -d postgres -f /tmp/$(basename $SQL_FILE)"

echo ""
echo "✅ SQL execution completed!"
