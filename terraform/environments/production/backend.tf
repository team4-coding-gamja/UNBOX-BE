# terraform/environments/production/backend.tf
terraform {
  backend "s3" {
    bucket         = "unbox-terraform-state-bucket-1"
    key            = "production/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
    dynamodb_table = "unbox-terraform-locks"
  }
}
