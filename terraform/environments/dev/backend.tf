# terraform/environments/dev/backend.tf
terraform {
  backend "s3" {
    bucket         = "unbox-terraform-state-bucket-1"
    key            = "dev/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
    dynamodb_table = "unbox-terraform-locks"
  }
}
