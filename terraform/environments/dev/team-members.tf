# DEV 환경 팀원 관리

# 실제 팀 구성에 맞는 팀원 목록 정의 (5명 팀)
locals {
  team_members = {
    # CI/CD 관리자 (Gahun Song - Backend + CI/CD 담당)
    "gahyun.song" = {
      role              = "cicd-admin"
      team              = "infrastructure"
      create_access_key = true
    }
    
    # 백엔드 개발자들 (4명)
    "backend.dev1" = {
      role              = "developer"
      team              = "backend"
      create_access_key = true
    }
    
    "backend.dev2" = {
      role              = "developer"
      team              = "backend"
      create_access_key = true
    }
    
    "backend.dev3" = {
      role              = "developer"
      team              = "backend"
      create_access_key = true
    }
    
    "backend.dev4" = {
      role              = "developer"
      team              = "backend"
      create_access_key = true
    }
  }
}

# IAM 모듈에 팀원 정보 전달
module "github_actions_iam" {
  source = "../../modules/iam"
  
  environment           = "dev"
  ecr_repository_arns   = values(module.ecr.repository_arns)
  aws_region           = "ap-northeast-2"
  
  # 팀 접근 관리 활성화
  enable_team_access = true
  team_members      = local.team_members
  
  # DEV 환경에서는 추가 권한 비활성화
  enable_additional_permissions = false
  
  common_tags = {
    Project     = "unbox"
    Environment = "dev"
    ManagedBy   = "terraform"
    Team        = "devops"
  }
}

# 팀원 자격증명 출력 (민감 정보)
output "team_member_credentials" {
  description = "Team member AWS credentials"
  value       = module.github_actions_iam.team_member_access_keys
  sensitive   = true
}

# 팀 그룹 정보 출력
output "team_groups_info" {
  description = "Created team groups"
  value       = module.github_actions_iam.team_groups
}