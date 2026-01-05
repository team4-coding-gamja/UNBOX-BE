resource "random_string" "bucket_suffix" {
  length  = 8
  special = false
  upper   = false
}

resource "aws_s3_bucket" "images" {
  bucket = "${var.project_name}-images-${random_string.bucket_suffix.result}"

  tags = {
    Name = "${var.project_name}-images"
  }
}

# S3 버킷 버전 관리 (비용 절약을 위해 비활성화)
# resource "aws_s3_bucket_versioning" "images" {
#   bucket = aws_s3_bucket.images.id
#   versioning_configuration {
#     status = "Enabled"
#   }
# }

resource "aws_s3_bucket" "logs" {
  bucket = "${var.project_name}-logs-${random_string.bucket_suffix.result}"

  tags = {
    Name = "${var.project_name}-logs"
  }
}

# S3 버킷 공개 액세스 설정
resource "aws_s3_bucket_public_access_block" "images" {
  bucket = aws_s3_bucket.images.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CloudFront Origin Access Control
resource "aws_cloudfront_origin_access_control" "images" {
  name                              = "${var.project_name}-images-oac"
  description                       = "OAC for ${var.project_name} images bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# CloudFront Distribution
resource "aws_cloudfront_distribution" "images" {
  origin {
    domain_name              = aws_s3_bucket.images.bucket_regional_domain_name
    origin_id                = "S3-${aws_s3_bucket.images.id}"
    origin_access_control_id = aws_cloudfront_origin_access_control.images.id
  }

  enabled = true

  default_cache_behavior {
    allowed_methods          = ["GET", "HEAD"]
    cached_methods           = ["GET", "HEAD"]
    target_origin_id         = "S3-${aws_s3_bucket.images.id}"
    compress                 = true
    viewer_protocol_policy   = "redirect-to-https"
    cache_policy_id          = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"  # AWS Managed CachingOptimized
    origin_request_policy_id = "88a5eaf4-2fd4-4709-b370-b4c650ea3fcf"  # AWS Managed CORS-S3Origin
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Name = "${var.project_name}-cloudfront"
  }
}

# S3 버킷 정책 (CloudFront만 접근 허용)
resource "aws_s3_bucket_policy" "images" {
  bucket = aws_s3_bucket.images.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontServicePrincipal"
        Effect = "Allow"
        Principal = {
          Service = "cloudfront.amazonaws.com"
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.images.arn}/*"
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = aws_cloudfront_distribution.images.arn
          }
        }
      }
    ]
  })
}

# =============================================================================
# 프론트엔드 정적 파일용 S3 + CloudFront
# =============================================================================

# React 빌드 결과물(HTML, CSS, JS)을 저장하는 S3 버킷
# 용도: React SPA의 정적 파일 호스팅
resource "aws_s3_bucket" "frontend" {
  bucket = "${var.project_name}-frontend-${random_string.bucket_suffix.result}"

  tags = {
    Name = "${var.project_name}-frontend"
    Type = "Frontend-Static-Files"
  }
}

# 프론트엔드 버킷의 퍼블릭 액세스 차단
# CloudFront를 통해서만 접근하도록 보안 설정
resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# 프론트엔드용 CloudFront Origin Access Control
resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "${var.project_name}-frontend-oac"
  description                       = "OAC for ${var.project_name} frontend bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# 프론트엔드용 CloudFront Distribution
# React SPA에 최적화된 설정으로 전 세계 사용자에게 빠른 웹사이트 제공
resource "aws_cloudfront_distribution" "frontend" {
  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "S3-${aws_s3_bucket.frontend.id}"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  enabled             = true
  is_ipv6_enabled     = true
  comment             = "${var.project_name} Frontend SPA Distribution"
  default_root_object = "index.html"  # 루트 접근 시 index.html 반환

  # React SPA에 최적화된 캐시 설정
  default_cache_behavior {
    allowed_methods          = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods           = ["GET", "HEAD"]
    target_origin_id         = "S3-${aws_s3_bucket.frontend.id}"
    compress                 = true
    viewer_protocol_policy   = "redirect-to-https"  # HTTPS 강제
    
    # SPA용 캐시 정책: HTML은 짧게, 정적 파일은 길게 캐시
    cache_policy_id          = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"  # CachingOptimized
    origin_request_policy_id = "88a5eaf4-2fd4-4709-b370-b4c650ea3fcf"  # CORS-S3Origin
  }

  # React Router를 위한 커스텀 에러 페이지 설정
  # 404 에러 시 index.html을 반환하여 클라이언트 사이드 라우팅 지원
  custom_error_response {
    error_caching_min_ttl = 0
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
  }

  # 403 에러도 index.html로 리다이렉트 (권한 문제 시)
  custom_error_response {
    error_caching_min_ttl = 0
    error_code            = 403
    response_code         = 200
    response_page_path    = "/index.html"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  # SSL 인증서 설정 (CloudFront 기본 인증서 사용)
  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Name = "${var.project_name}-frontend-cloudfront"
    Type = "Frontend-SPA-CDN"
  }
}

# 프론트엔드 S3 버킷 정책 (CloudFront만 접근 허용)
resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCloudFrontServicePrincipal"
        Effect = "Allow"
        Principal = {
          Service = "cloudfront.amazonaws.com"
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.frontend.arn}/*"
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = aws_cloudfront_distribution.frontend.arn
          }
        }
      }
    ]
  })
}