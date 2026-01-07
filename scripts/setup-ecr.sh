#!/bin/bash

# UNBOX ECR Repository Setup Script

set -e

AWS_REGION="ap-northeast-2"
REPOSITORY_NAME="unbox-backend"

echo "🚀 Setting up ECR repository for UNBOX backend..."

# Check if repository exists
if aws ecr describe-repositories --repository-names $REPOSITORY_NAME --region $AWS_REGION >/dev/null 2>&1; then
    echo "✅ ECR repository '$REPOSITORY_NAME' already exists"
else
    echo "📦 Creating ECR repository '$REPOSITORY_NAME'..."
    aws ecr create-repository \
        --repository-name $REPOSITORY_NAME \
        --region $AWS_REGION \
        --image-scanning-configuration scanOnPush=true
    echo "✅ ECR repository created successfully"
fi

# Get repository URI
REPOSITORY_URI=$(aws ecr describe-repositories --repository-names $REPOSITORY_NAME --region $AWS_REGION --query 'repositories[0].repositoryUri' --output text)

echo "📋 Repository URI: $REPOSITORY_URI"
echo ""
echo "🔧 Next steps:"
echo "1. Add the following secrets to your GitHub repository:"
echo "   - AWS_ACCESS_KEY_ID"
echo "   - AWS_SECRET_ACCESS_KEY"
echo "   - ECR_REGISTRY: $REPOSITORY_URI"
echo "   - SSH_PRIVATE_KEY"
echo "   - SSH_PUBLIC_KEY"
echo "   - DB_PASSWORD"
echo "   - DB_HOST"
echo ""
echo "2. Push your code to trigger the deployment workflow"