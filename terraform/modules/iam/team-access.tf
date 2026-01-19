# 팀원용 IAM 그룹 및 권한 관리 (단순화 버전)

# ========================================
# IAM 그룹 생성 (2개 그룹만)
# ========================================

# CI/CD 관리자 그룹
resource "aws_iam_group" "cicd_admin" {
  count = var.enable_team_access ? 1 : 0
  name  = "cicd-admin-${var.environment}"
  path  = "/teams/"
}

# 개발자 그룹
resource "aws_iam_group" "developers" {
  count = var.enable_team_access ? 1 : 0
  name  = "developers-${var.environment}"
  path  = "/teams/"
}

# ========================================
# 정책 생성 (단순화)
# ========================================

# CI/CD 관리자 정책 (거의 모든 권한)
resource "aws_iam_policy" "cicd_admin_policy" {
  count       = var.enable_team_access ? 1 : 0
  name        = "CICDAdminPolicy-${var.environment}"
  description = "Full access policy for CI/CD admin in ${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:*",
          "ecs:*",
          "logs:*",
          "cloudwatch:*",
          "s3:*",
          "rds:*",
          "elasticache:*",
          "iam:ListUsers",
          "iam:ListGroups",
          "iam:GetUser",
          "iam:GetGroup",
          "ssm:*",
          "secretsmanager:*"
        ]
        Resource = "*"
      }
    ]
  })
}

# 개발자 정책 (CI/CD 제외한 대부분 권한)
resource "aws_iam_policy" "developer_policy" {
  count       = var.enable_team_access ? 1 : 0
  name        = "DeveloperPolicy-${var.environment}"
  description = "Developer access policy for ${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:*",
          "ecs:DescribeServices",
          "ecs:DescribeTasks",
          "ecs:DescribeTaskDefinition",
          "ecs:ListTasks",
          "ecs:UpdateService",
          "ecs:StopTask",
          "logs:*",
          "cloudwatch:*"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "rds:DescribeDBInstances",
          "rds:DescribeDBClusters",
          "rds:ListTagsForResource"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "elasticache:DescribeCacheClusters",
          "elasticache:DescribeReplicationGroups"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:ListBucket",
          "s3:DeleteObject"
        ]
        Resource = [
          "arn:aws:s3:::unbox-*",
          "arn:aws:s3:::unbox-*/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParameter",
          "ssm:GetParameters",
          "ssm:GetParametersByPath"
        ]
        Resource = "arn:aws:ssm:${var.aws_region}:*:parameter/unbox/*"
      },
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ]
        Resource = "arn:aws:secretsmanager:${var.aws_region}:*:secret:unbox/*"
      }
    ]
  })
}

# ========================================
# 정책을 그룹에 연결
# ========================================

resource "aws_iam_group_policy_attachment" "cicd_admin_attachment" {
  count      = var.enable_team_access ? 1 : 0
  group      = aws_iam_group.cicd_admin[0].name
  policy_arn = aws_iam_policy.cicd_admin_policy[0].arn
}

resource "aws_iam_group_policy_attachment" "developer_attachment" {
  count      = var.enable_team_access ? 1 : 0
  group      = aws_iam_group.developers[0].name
  policy_arn = aws_iam_policy.developer_policy[0].arn
}

# ========================================
# 팀원 사용자 생성
# ========================================

# 팀원 목록 변수 기반으로 사용자 생성
resource "aws_iam_user" "team_members" {
  for_each = var.enable_team_access ? var.team_members : {}
  
  name = each.key
  path = "/team/"
  
  tags = merge(var.common_tags, {
    Name        = each.key
    Role        = each.value.role
    Team        = each.value.team
    Environment = var.environment
  })
}

# 사용자를 적절한 그룹에 추가
resource "aws_iam_user_group_membership" "team_member_groups" {
  for_each = var.enable_team_access ? var.team_members : {}
  
  user = aws_iam_user.team_members[each.key].name
  
  groups = [
    each.value.role == "cicd-admin" ? aws_iam_group.cicd_admin[0].name : aws_iam_group.developers[0].name
  ]
}

# 팀원용 Access Key 생성 
resource "aws_iam_access_key" "team_member_keys" {
  for_each = {
    for name, member in var.team_members : name => member
    if var.enable_team_access && member.create_access_key == true
  }
  
  user = aws_iam_user.team_members[each.key].name
}