#!/bin/bash

echo "🎨 프론트엔드 배포 시작..."

# 1. React 빌드
npm run build

# 2. S3 업로드
BUCKET_NAME=$(terraform output -raw frontend_bucket_name)
aws s3 sync build/ s3://$BUCKET_NAME/ --delete

# 3. CloudFront 캐시 무효화
DISTRIBUTION_ID=$(terraform output -raw frontend_cloudfront_distribution_id)
aws cloudfront create-invalidation \
  --distribution-id $DISTRIBUTION_ID \
  --paths "/*"

# 4. 배포 완료
FRONTEND_URL=$(terraform output -raw frontend_url)
echo "✅ 배포 완료: $FRONTEND_URL"
