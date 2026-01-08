# =============================================================================
# Terraform Backend 리소스 생성
# 이 파일은 Terraform State 저장용 S3 버킷과 Lock용 DynamoDB 테이블을 생성합니다.
# 한 번만 실행하면 됩니다.
# =============================================================================

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-northeast-2"
}

# S3 버킷: Terraform State 파일 저장용
resource "aws_s3_bucket" "terraform_state" {
  bucket = "unbox-terraform-state-bucket"
  
  tags = {
    Name        = "Terraform State Bucket"
    Environment = "dev"
    Project     = "unbox"
  }
}

# S3 버킷 버전 관리 활성화
resource "aws_s3_bucket_versioning" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id
  versioning_configuration {
    status = "Enabled"
  }
}

# S3 버킷 암호화 설정
resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# S3 버킷 퍼블릭 액세스 차단
resource "aws_s3_bucket_public_access_block" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# DynamoDB 테이블: Terraform State Lock 관리용
resource "aws_dynamodb_table" "terraform_locks" {
  name           = "unbox-terraform-locks"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  tags = {
    Name        = "Terraform State Lock Table"
    Environment = "dev"
    Project     = "unbox"
  }
}

# 출력값
output "s3_bucket_name" {
  description = "Name of the S3 bucket for Terraform state"
  value       = aws_s3_bucket.terraform_state.id
}

output "dynamodb_table_name" {
  description = "Name of the DynamoDB table for Terraform locks"
  value       = aws_dynamodb_table.terraform_locks.name
}