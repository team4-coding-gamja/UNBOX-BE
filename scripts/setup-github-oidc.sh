#!/bin/bash
# GitHub OIDC Provider 생성 스크립트

set -e

echo "🔧 GitHub OIDC Provider 생성 중..."

# 1. OIDC Provider 생성
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 \
  --region ap-northeast-2

echo "✅ OIDC Provider 생성 완료!"

# 2. 생성된 Provider ARN 확인
PROVIDER_ARN=$(aws iam list-open-id-connect-providers --query "OpenIDConnectProviderList[?contains(Arn, 'token.actions.githubusercontent.com')].Arn" --output text)

echo "📋 Provider ARN: $PROVIDER_ARN"

# 3. IAM Role 확인 (DEV_IAM_ROLE_ARN에서 Role 이름 추출 필요)
echo ""
echo "⚠️  다음 단계:"
echo "1. AWS Console → IAM → Roles"
echo "2. GitHub Actions용 Role 찾기"
echo "3. Trust relationships 탭 → Edit trust policy"
echo "4. 다음 내용으로 업데이트:"
echo ""
cat << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::YOUR_ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:team4-coding-gamja/UNBOX-BE:*"
        }
      }
    }
  ]
}
EOF

echo ""
echo "✅ 완료!"
