# terraform/environments/prod/backend.tf
terraform {
  backend "s3" {
    bucket         = "unbox-terraform-state-bucket-1"
    key            = "prod/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
    dynamodb_table = "unbox-terraform-locks"
  }
}
