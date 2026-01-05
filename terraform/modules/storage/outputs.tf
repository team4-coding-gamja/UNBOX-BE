output "images_bucket_name" {
  value = aws_s3_bucket.images.bucket
}

output "logs_bucket_name" {
  value = aws_s3_bucket.logs.bucket
}

output "cloudfront_domain_name" {
  description = "CloudFront 도메인 이름 (이미지용)"
  value       = aws_cloudfront_distribution.images.domain_name
}

output "cloudfront_distribution_id" {
  description = "CloudFront Distribution ID (이미지용)"
  value       = aws_cloudfront_distribution.images.id
}

# =============================================================================
# 프론트엔드 관련 출력값
# =============================================================================

output "frontend_bucket_name" {
  description = "프론트엔드 S3 버킷 이름"
  value       = aws_s3_bucket.frontend.bucket
}

output "frontend_cloudfront_domain" {
  description = "프론트엔드 CloudFront 도메인 이름"
  value       = aws_cloudfront_distribution.frontend.domain_name
}

output "frontend_cloudfront_distribution_id" {
  description = "프론트엔드 CloudFront Distribution ID (캐시 무효화용)"
  value       = aws_cloudfront_distribution.frontend.id
}

output "frontend_url" {
  description = "프론트엔드 접속 URL"
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}