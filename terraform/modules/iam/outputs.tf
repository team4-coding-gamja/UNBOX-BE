# IAM 모듈 출력값

output "github_actions_user_name" {
  description = "GitHub Actions IAM user name"
  value       = aws_iam_user.github_actions.name
}

output "github_actions_user_arn" {
  description = "GitHub Actions IAM user ARN"
  value       = aws_iam_user.github_actions.arn
}

output "github_actions_access_key_id" {
  description = "GitHub Actions access key ID"
  value       = aws_iam_access_key.github_actions.id
  sensitive   = true
}

output "github_actions_secret_access_key" {
  description = "GitHub Actions secret access key"
  value       = aws_iam_access_key.github_actions.secret
  sensitive   = true
}

output "ecr_policy_arn" {
  description = "ECR policy ARN"
  value       = aws_iam_policy.ecr_policy.arn
}

# 팀 관련 출력값
output "team_groups" {
  description = "Created IAM groups for teams"
  value = var.enable_team_access ? {
    cicd_admin = aws_iam_group.cicd_admin[0].name
    developers = aws_iam_group.developers[0].name
  } : {}
}

output "team_member_users" {
  description = "Created team member users"
  value = var.enable_team_access ? {
    for name, user in aws_iam_user.team_members : name => {
      name = user.name
      arn  = user.arn
    }
  } : {}
}

output "team_member_access_keys" {
  description = "Access keys for team members (sensitive)"
  value = var.enable_team_access ? {
    for name, key in aws_iam_access_key.team_member_keys : name => {
      access_key_id     = key.id
      secret_access_key = key.secret
    }
  } : {}
  sensitive = true
}