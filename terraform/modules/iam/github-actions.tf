# GitHub Actions용 IAM 사용자 및 권한 설정

# GitHub Actions용 IAM 사용자
resource "aws_iam_user" "github_actions" {
  name = "github-actions-${var.environment}"
  path = "/ci-cd/"

  tags = merge(var.common_tags, {
    Name        = "github-actions-${var.environment}"
    Purpose     = "CI/CD"
    Environment = var.environment
  })
}

# GitHub Actions용 Access Key
resource "aws_iam_access_key" "github_actions" {
  user = aws_iam_user.github_actions.name
}

# ECR 전용 정책
resource "aws_iam_policy" "ecr_policy" {
  name        = "ECRPolicy-${var.environment}"
  description = "Policy for ECR access in ${var.environment} environment"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload"
        ]
        Resource = var.ecr_repository_arns
      }
    ]
  })
}

# 정책을 사용자에게 연결
resource "aws_iam_user_policy_attachment" "github_actions_ecr" {
  user       = aws_iam_user.github_actions.name
  policy_arn = aws_iam_policy.ecr_policy.arn
}

# 추가 권한 (필요시)
resource "aws_iam_policy" "additional_policy" {
  count = var.enable_additional_permissions ? 1 : 0
  
  name        = "AdditionalPolicy-${var.environment}"
  description = "Additional permissions for GitHub Actions"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecs:UpdateService",
          "ecs:DescribeServices",
          "ecs:DescribeTasks"
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "aws:RequestedRegion" = var.aws_region
          }
        }
      }
    ]
  })
}

resource "aws_iam_user_policy_attachment" "github_actions_additional" {
  count = var.enable_additional_permissions ? 1 : 0
  
  user       = aws_iam_user.github_actions.name
  policy_arn = aws_iam_policy.additional_policy[0].arn
}