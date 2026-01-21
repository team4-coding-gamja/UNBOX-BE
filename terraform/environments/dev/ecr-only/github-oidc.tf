# ============================================
# GitHub OIDC Provider
# ============================================

# GitHub OIDC Provider 생성
resource "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"
  
  client_id_list = [
    "sts.amazonaws.com"
  ]
  
  # GitHub의 thumbprint (고정값)
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd"
  ]
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-github-oidc"
  })
}

# ============================================
# GitHub Actions용 IAM Role
# ============================================

# Trust Policy - GitHub Actions가 이 Role을 assume할 수 있도록 허용
data "aws_iam_policy_document" "github_actions_assume_role" {
  statement {
    effect = "Allow"
    
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }
    
    actions = ["sts:AssumeRoleWithWebIdentity"]
    
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_org}/${var.github_repo}:*"]
    }
  }
}

# IAM Role 생성
resource "aws_iam_role" "github_actions" {
  name               = "${local.project}-${local.environment}-github-actions-role"
  assume_role_policy = data.aws_iam_policy_document.github_actions_assume_role.json
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-github-actions-role"
  })
}

# ============================================
# ECR 권한 Policy
# ============================================

# ECR 접근 권한 Policy Document
data "aws_iam_policy_document" "ecr_access" {
  # ECR 로그인 권한
  statement {
    effect = "Allow"
    actions = [
      "ecr:GetAuthorizationToken"
    ]
    resources = ["*"]
  }
  
  # ECR 이미지 푸시/풀 권한
  statement {
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:PutImage",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeRepositories",
      "ecr:DescribeImages",
      "ecr:ListImages"
    ]
    resources = [
      aws_ecr_repository.product_service.arn,
      aws_ecr_repository.order_service.arn,
      aws_ecr_repository.payment_service.arn,
      aws_ecr_repository.trade_service.arn,
      aws_ecr_repository.user_service.arn
    ]
  }
}

# ECR Policy 생성
resource "aws_iam_policy" "ecr_access" {
  name        = "${local.project}-${local.environment}-ecr-access"
  description = "Allow GitHub Actions to push/pull images to ECR"
  policy      = data.aws_iam_policy_document.ecr_access.json
  
  tags = merge(local.common_tags, {
    Name = "${local.project}-${local.environment}-ecr-access"
  })
}

# Policy를 Role에 연결
resource "aws_iam_role_policy_attachment" "github_actions_ecr" {
  role       = aws_iam_role.github_actions.name
  policy_arn = aws_iam_policy.ecr_access.arn
}
