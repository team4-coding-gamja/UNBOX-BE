#!/bin/bash

# RDS 접속 스크립트 (Bastion Host 경유)
# 사용법: ./scripts/connect_to_rds.sh [database_name]

set -e

BASTION_IP=$(cd terraform/environments/dev && terraform output -raw bastion_public_ip)
RDS_ENDPOINT=$(cd terraform/environments/dev && terraform output -raw rds_endpoints | jq -r '.common' | cut -d: -f1)
SSH_KEY="$HOME/.ssh/unbox-bastion-aws.pem"
DB_NAME="${1:-postgres}"

echo "🔗 Connecting to RDS via Bastion Host..."
echo "   Bastion IP: $BASTION_IP"
echo "   RDS Endpoint: $RDS_ENDPOINT"
echo "   Database: $DB_NAME"
echo ""

# SSH 키 권한 확인
if [ ! -f "$SSH_KEY" ]; then
    echo "❌ SSH key not found: $SSH_KEY"
    echo "   Please ensure the Bastion Host is created with the key pair."
    exit 1
fi

chmod 400 "$SSH_KEY"

# RDS 접속
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ec2-user@$BASTION_IP \
    "PGPASSWORD='l[:9+q01Roc4cqM?' psql -h $RDS_ENDPOINT -U unbox_admin -d $DB_NAME"
