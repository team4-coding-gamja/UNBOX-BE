# =============================================================================
# Terraform S3 Backend 설정
# 팀 협업을 위한 State 파일 원격 저장
# =============================================================================

terraform {
  backend "s3" {
    bucket         = "unbox-terraform-state-coding-potato"
    key            = "dev/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
    
    # DynamoDB 테이블 (State lock 사용)
    dynamodb_table = "unbox-terraform-locks"
  }
}