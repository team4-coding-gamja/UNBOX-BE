output "ec2_public_ip" {
  description = "EC2 퍼블릭 IP"
  value       = module.compute.public_ip
}

output "rds_endpoint" {
  description = "RDS 엔드포인트 (RDS 사용 시만)"
  value       = var.use_rds ? module.database[0].db_endpoint : "H2 인메모리 DB 사용"
}

output "s3_images_bucket" {
  description = "이미지 S3 버킷"
  value       = module.storage.images_bucket_name
}

output "cloudfront_url" {
  description = "CloudFront URL"
  value       = "https://${module.storage.cloudfront_domain_name}"
}