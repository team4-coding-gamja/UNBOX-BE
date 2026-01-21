# ============================================
# ECR Outputs
# ============================================

output "ecr_repositories" {
  description = "ECR repository URLs"
  value = {
    product = aws_ecr_repository.product_service.repository_url
    order   = aws_ecr_repository.order_service.repository_url
    payment = aws_ecr_repository.payment_service.repository_url
    trade   = aws_ecr_repository.trade_service.repository_url
    user    = aws_ecr_repository.user_service.repository_url
  }
}

output "ecr_repository_arns" {
  description = "ECR repository ARNs"
  value = {
    product = aws_ecr_repository.product_service.arn
    order   = aws_ecr_repository.order_service.arn
    payment = aws_ecr_repository.payment_service.arn
    trade   = aws_ecr_repository.trade_service.arn
    user    = aws_ecr_repository.user_service.arn
  }
}

output "ecr_repository_names" {
  description = "ECR repository names"
  value = {
    product = aws_ecr_repository.product_service.name
    order   = aws_ecr_repository.order_service.name
    payment = aws_ecr_repository.payment_service.name
    trade   = aws_ecr_repository.trade_service.name
    user    = aws_ecr_repository.user_service.name
  }
}

# ============================================
# GitHub Actions Outputs
# ============================================

output "github_actions_role_arn" {
  description = "IAM Role ARN for GitHub Actions (DEV_IAM_ROLE_ARN)"
  value       = aws_iam_role.github_actions.arn
}

output "github_actions_role_name" {
  description = "IAM Role name for GitHub Actions"
  value       = aws_iam_role.github_actions.name
}

output "github_oidc_provider_arn" {
  description = "GitHub OIDC Provider ARN"
  value       = aws_iam_openid_connect_provider.github.arn
}

# ============================================
# 사용 가이드
# ============================================

output "next_steps" {
  description = "다음 단계 안내"
  value = <<-EOT
  
  ✅ ECR 리포지토리와 GitHub OIDC 설정이 완료되었습니다!
  
  📋 다음 단계:
  
  1. GitHub Secrets 설정:
     - Repository Settings → Secrets and variables → Actions
     - Secret 이름: DEV_IAM_ROLE_ARN
     - Secret 값: ${aws_iam_role.github_actions.arn}
  
  2. 로컬에서 Docker 이미지 푸시 테스트:
     
     # ECR 로그인
     aws ecr get-login-password --region ap-northeast-2 | \
       docker login --username AWS --password-stdin ${split("/", aws_ecr_repository.product_service.repository_url)[0]}
     
     # 이미지 빌드 (루트 디렉토리에서)
     docker build -t ${aws_ecr_repository.product_service.repository_url}:test \
       -f unbox_product/Dockerfile .
     
     # 이미지 푸시
     docker push ${aws_ecr_repository.product_service.repository_url}:test
  
  3. GitHub Actions 테스트:
     - develop 브랜치에 푸시하면 자동으로 CI 실행
     - 또는 Actions 탭에서 수동 실행
  
  EOT
}
