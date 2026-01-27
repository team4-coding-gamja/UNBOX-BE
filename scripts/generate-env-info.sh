#!/bin/bash

# 환경 정보 문서 자동 생성 스크립트
# 사용법: ./scripts/generate-env-info.sh [dev|prod]

set -e

ENV=${1:-dev}
OUTPUT_FILE="DEV_ENVIRONMENT_INFO.md"

if [[ "$ENV" != "dev" && "$ENV" != "prod" ]]; then
    echo "❌ 잘못된 환경입니다. 'dev' 또는 'prod'를 입력하세요."
    exit 1
fi

if [[ "$ENV" == "prod" ]]; then
    OUTPUT_FILE="PROD_ENVIRONMENT_INFO.md"
fi

echo "🔍 $ENV 환경 정보 수집 중..."

# Terraform 출력 가져오기
cd terraform/environments/$ENV

ALB_ADDRESS=$(terraform output -raw alb_address 2>/dev/null || echo "N/A")
BASTION_IP=$(terraform output -raw bastion_public_ip 2>/dev/null || echo "N/A")
ECS_CLUSTER=$(terraform output -raw ecs_cluster_name 2>/dev/null || echo "N/A")
RDS_ENDPOINT=$(terraform output -json rds_endpoints 2>/dev/null | jq -r '.common' | cut -d: -f1 || echo "N/A")

cd ../../..

# AWS Account ID
AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text 2>/dev/null || echo "N/A")

echo "📝 환경 정보 문서 생성 중: $OUTPUT_FILE"

cat > "$OUTPUT_FILE" << EOF
# 🌐 ${ENV^^} 환경 접근 정보

> **주의**: 이 문서는 민감한 정보를 포함하고 있습니다. Git에 커밋하지 마세요!

**마지막 업데이트**: $(date +%Y-%m-%d)

---

## 📍 AWS 기본 정보

\`\`\`bash
AWS_ACCOUNT_ID="$AWS_ACCOUNT"
AWS_REGION="ap-northeast-2"
ENVIRONMENT="$ENV"
\`\`\`

---

## 🌍 서비스 엔드포인트

### ALB (Application Load Balancer)

\`\`\`bash
ALB_URL="http://$ALB_ADDRESS"
\`\`\`

### 서비스별 접근 URL

| 서비스 | URL | 헬스체크 |
|--------|-----|----------|
| **User** | http://$ALB_ADDRESS/user | \`/user/actuator/health\` |
| **Product** | http://$ALB_ADDRESS/product | \`/product/actuator/health\` |
| **Trade** | http://$ALB_ADDRESS/trade | \`/trade/actuator/health\` |
| **Order** | http://$ALB_ADDRESS/order | \`/order/actuator/health\` |
| **Payment** | http://$ALB_ADDRESS/payment | \`/payment/actuator/health\` |

---

## 🐳 ECR (Docker Registry)

### ECR 로그인

\`\`\`bash
aws ecr get-login-password --region ap-northeast-2 | \\
  docker login --username AWS --password-stdin \\
  $AWS_ACCOUNT.dkr.ecr.ap-northeast-2.amazonaws.com
\`\`\`

### Repository URLs

\`\`\`bash
ECR_USER="$AWS_ACCOUNT.dkr.ecr.ap-northeast-2.amazonaws.com/unbox-$ENV-user-repo"
ECR_PRODUCT="$AWS_ACCOUNT.dkr.ecr.ap-northeast-2.amazonaws.com/unbox-$ENV-product-repo"
ECR_TRADE="$AWS_ACCOUNT.dkr.ecr.ap-northeast-2.amazonaws.com/unbox-$ENV-trade-repo"
ECR_ORDER="$AWS_ACCOUNT.dkr.ecr.ap-northeast-2.amazonaws.com/unbox-$ENV-order-repo"
ECR_PAYMENT="$AWS_ACCOUNT.dkr.ecr.ap-northeast-2.amazonaws.com/unbox-$ENV-payment-repo"
\`\`\`

---

## 🚀 ECS (Container Service)

### 클러스터 정보

\`\`\`bash
ECS_CLUSTER="$ECS_CLUSTER"
\`\`\`

### 서비스 상태 확인

\`\`\`bash
aws ecs describe-services \\
  --cluster $ECS_CLUSTER \\
  --services unbox-$ENV-user unbox-$ENV-product unbox-$ENV-trade unbox-$ENV-order unbox-$ENV-payment \\
  --region ap-northeast-2 \\
  --query 'services[*].[serviceName,runningCount,desiredCount]' \\
  --output table
\`\`\`

---

## 📊 CloudWatch Logs

### 로그 확인

\`\`\`bash
# User 서비스 로그 실시간 확인
aws logs tail /ecs/unbox-$ENV/user --follow --region ap-northeast-2

# Product 서비스 로그
aws logs tail /ecs/unbox-$ENV/product --follow --region ap-northeast-2

# 나머지 서비스도 동일한 패턴
\`\`\`

### CloudWatch Console 링크

\`\`\`
https://ap-northeast-2.console.aws.amazon.com/cloudwatch/home?region=ap-northeast-2#logsV2:log-groups
\`\`\`

---

## 🗄 RDS (PostgreSQL Database)

### 데이터베이스 정보

\`\`\`bash
RDS_ENDPOINT="$RDS_ENDPOINT"
RDS_PORT="5432"
MASTER_USER="unbox_admin"
\`\`\`

### 서비스별 비밀번호 확인

\`\`\`bash
# User 서비스 DB 비밀번호
aws ssm get-parameter \\
  --name /unbox/$ENV/user/DB_PASSWORD \\
  --with-decryption \\
  --query 'Parameter.Value' \\
  --output text \\
  --region ap-northeast-2
\`\`\`

---

## 🔐 Bastion Host

### Bastion 정보

\`\`\`bash
BASTION_IP="$BASTION_IP"
BASTION_USER="ec2-user"
SSH_KEY_PATH="~/.ssh/unbox-bastion-aws.pem"
\`\`\`

### Bastion 접속

\`\`\`bash
ssh -i ~/.ssh/unbox-bastion-aws.pem ec2-user@$BASTION_IP
\`\`\`

---

## 🛠 유용한 명령어

### 전체 서비스 상태 확인

\`\`\`bash
aws ecs describe-services \\
  --cluster $ECS_CLUSTER \\
  --services unbox-$ENV-user unbox-$ENV-product unbox-$ENV-trade unbox-$ENV-order unbox-$ENV-payment \\
  --region ap-northeast-2 \\
  --query 'services[*].[serviceName,runningCount,desiredCount]' \\
  --output table
\`\`\`

### 서비스 재시작

\`\`\`bash
aws ecs update-service \\
  --cluster $ECS_CLUSTER \\
  --service unbox-$ENV-user \\
  --force-new-deployment \\
  --region ap-northeast-2
\`\`\`

---

**⚠️ 보안 주의사항**:
- 이 문서는 절대 Git에 커밋하지 마세요
- 비밀번호는 안전하게 보관하세요
- AWS Access Key는 주기적으로 로테이션하세요

EOF

echo "✅ 환경 정보 문서 생성 완료: $OUTPUT_FILE"
echo ""
echo "⚠️  주의: 이 파일은 .gitignore에 포함되어 있습니다."
echo "   Git에 커밋되지 않도록 주의하세요!"
