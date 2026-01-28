#!/bin/bash
# GitHub Actions 역할에 iam:PassRole 권한 추가 (임시 수동 수정)

ROLE_NAME="github-actions-ecr-role"
POLICY_NAME="unbox-dev-github-ecs-cd-policy"
ACCOUNT_ID="632941626317"

# ECS Task Execution Role과 Task Role ARN
TASK_EXECUTION_ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/unbox-dev-ecs-task-execution-role"
TASK_ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/unbox-dev-ecs-task-role"

echo "Adding iam:PassRole permission to ${ROLE_NAME}..."

# 정책 생성/업데이트
aws iam put-role-policy \
  --role-name "${ROLE_NAME}" \
  --policy-name "${POLICY_NAME}" \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "ecs:DescribeServices",
          "ecs:DescribeTaskDefinition",
          "ecs:RegisterTaskDefinition",
          "ecs:UpdateService",
          "ecs:DescribeClusters"
        ],
        "Resource": "*"
      },
      {
        "Effect": "Allow",
        "Action": ["iam:PassRole"],
        "Resource": [
          "'"${TASK_EXECUTION_ROLE_ARN}"'",
          "'"${TASK_ROLE_ARN}"'"
        ]
      }
    ]
  }'

echo "✅ Permission added successfully!"
echo ""
echo "Verifying the policy..."
aws iam get-role-policy --role-name "${ROLE_NAME}" --policy-name "${POLICY_NAME}"
