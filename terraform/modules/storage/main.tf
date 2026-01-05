# =============================================================================
# S3 버킷 및 CloudFront 구성
# 정적 파일 저장 및 CDN 서비스를 위한 스토리지 인프라
# =============================================================================

# 랜덤 문자열 생성기
# - S3 버킷명은 전 세계에서 유일해야 하므로 랜덤 접미사 추가
# - 버킷명 충돌 방지를 위한 안전 장치
resource "random_string" "bucket_suffix" {
  length  = 8      # 8자리 랜덤 문자열
  special = false  # 특수문자 사용 안 함
  upper   = false  # 대문자 사용 안 함 (S3 버킷명 규칙)
}

# =============================================================================
# 이미지 저장용 S3 버킷 및 CDN
# =============================================================================

# 상품 이미지 저장용 S3 버킷
# - UNBOX 애플리케이션에서 사용하는 상품 이미지 저장
# - 사용자 업로드 이미지, 상품 사진 등을 저장
resource "aws_s3_bucket" "images" {
  bucket = "${var.project_name}-images-${random_string.bucket_suffix.result}"  # 유니크한 버킷명

  tags = {
    Name = "${var.project_name}-images"
    Type = "Image-Storage"
    Purpose = "Product-Images"
  }
}

# S3 버킷 버전 관리 (비용 절약을 위해 비활성화)
# 버전 관리를 활성화하면 더 많은 스토리지 비용 발생
# resource "aws_s3_bucket_versioning" "images" {
#   bucket = aws_s3_bucket.images.id
#   versioning_configuration {
#     status = "Enabled"
#   }
# }

# 애플리케이션 로그 저장용 S3 버킷
# - Spring Boot 애플리케이션 로그 파일 저장
# - 에러 로그, 액세스 로그, 애플리케이션 로그 등을 중앙 집중 관리
resource "aws_s3_bucket" "logs" {
  bucket = "${var.project_name}-logs-${random_string.bucket_suffix.result}"

  tags = {
    Name = "${var.project_name}-logs"
    Type = "Log-Storage"
    Purpose = "Application-Logs"
  }
}

# =============================================================================
# S3 보안 설정 - 퍼블릭 액세스 차단
# =============================================================================

# 이미지 버킷 퍼블릭 액세스 차단
# - S3 버킷에 직접 접근을 차단하고 CloudFront를 통해서만 접근 가능
# - 보안 강화 및 비용 절약 (직접 S3 액세스 비용 > CloudFront 비용)
resource "aws_s3_bucket_public_access_block" "images" {
  bucket = aws_s3_bucket.images.id

  block_public_acls       = true  # 퍼블릭 ACL 차단
  block_public_policy     = true  # 퍼블릭 버킷 정책 차단
  ignore_public_acls      = true  # 기존 퍼블릭 ACL 무시
  restrict_public_buckets = true  # 퍼블릭 버킷 접근 제한
}

# =============================================================================
# CloudFront 구성 - 이미지 CDN
# =============================================================================

# CloudFront Origin Access Control (OAC) 생성
# - S3 버킷에 CloudFront만 접근할 수 있도록 하는 보안 설정
# - 기존 OAI(Origin Access Identity)를 대체하는 새로운 보안 기능
resource "aws_cloudfront_origin_access_control" "images" {
  name                              = "${var.project_name}-images-oac"
  description                       = "OAC for ${var.project_name} images bucket"
  origin_access_control_origin_type = "s3"      # S3 오리진 타입
  signing_behavior                  = "always"  # 항상 서명 요구
  signing_protocol                  = "sigv4"   # AWS Signature Version 4 사용
}

# 이미지용 CloudFront Distribution
# - 전 세계 사용자에게 빠른 이미지 제공
# - S3에서 직접 다운로드보다 빠르고 비용 효율적
resource "aws_cloudfront_distribution" "images" {
  # S3 버킷을 오리진으로 설정
  origin {
    domain_name              = aws_s3_bucket.images.bucket_regional_domain_name  # S3 버킷 도메인
    origin_id                = "S3-${aws_s3_bucket.images.id}"                  # 오리진 식별자
    origin_access_control_id = aws_cloudfront_origin_access_control.images.id    # OAC 연결
  }

  enabled = true  # CloudFront 배포 활성화

  # 기본 캐시 동작 설정
  default_cache_behavior {
    allowed_methods          = ["GET", "HEAD"]                                    # 허용되는 HTTP 메소드
    cached_methods           = ["GET", "HEAD"]                                    # 캐시되는 메소드
    target_origin_id         = "S3-${aws_s3_bucket.images.id}"                   # 대상 오리진
    compress                 = true                                              # 자동 압축 활성화
    viewer_protocol_policy   = "redirect-to-https"                               # HTTP를 HTTPS로 리다이렉트
    cache_policy_id          = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"            # AWS 관리형 CachingOptimized 정책
    origin_request_policy_id = "88a5eaf4-2fd4-4709-b370-b4c650ea3fcf"            # AWS 관리형 CORS-S3Origin 정책
  }

  # 지리적 제한 설정
  restrictions {
    geo_restriction {
      restriction_type = "none"  # 지리적 제한 없음 (전 세계 접근 가능)
    }
  }

  # SSL 인증서 설정
  viewer_certificate {
    cloudfront_default_certificate = true  # CloudFront 기본 인증서 사용
  }

  tags = {
    Name = "${var.project_name}-cloudfront"
    Type = "Image-CDN"
  }
}

# S3 버킷 정책 (이미지 버킷 - CloudFront만 접근 허용)
# - S3 버킷에 직접 접근을 차단하고 CloudFront를 통해서만 접근 가능
# - 보안 강화 및 비용 절약 효과
resource "aws_s3_bucket_policy" "images" {
  bucket = aws_s3_bucket.images.id

  # JSON 형식의 IAM 정책 정의
  policy = jsonencode({
    Version = "2012-10-17"  # IAM 정책 버전
    Statement = [
      {
        Sid    = "AllowCloudFrontServicePrincipal"  # 정책 식별자
        Effect = "Allow"                            # 허용 규칙
        Principal = {
          Service = "cloudfront.amazonaws.com"       # CloudFront 서비스만 허용
        }
        Action   = "s3:GetObject"                   # 객체 읽기 권한만 허용
        Resource = "${aws_s3_bucket.images.arn}/*"  # 버킷 내 모든 객체
        Condition = {
          StringEquals = {
            # 특정 CloudFront Distribution에서만 접근 허용
            "AWS:SourceArn" = aws_cloudfront_distribution.images.arn
          }
        }
      }
    ]
  })
}

# =============================================================================
# 프론트엔드 정적 파일용 S3 + CloudFront
# React SPA 호스팅을 위한 인프라
# =============================================================================

# React 빌드 결과물(HTML, CSS, JS)을 저장하는 S3 버킷
# 용도: React SPA의 정적 파일 호스팅
# - index.html, 번들링된 JS/CSS 파일, 이미지 에셋 등을 저장
resource "aws_s3_bucket" "frontend" {
  bucket = "${var.project_name}-frontend-${random_string.bucket_suffix.result}"

  tags = {
    Name = "${var.project_name}-frontend"
    Type = "Frontend-Static-Files"
    Purpose = "React-SPA-Hosting"
  }
}

# 프론트엔드 버킷의 퍼블릭 액세스 차단
# CloudFront를 통해서만 접근하도록 보안 설정
# - 직접 S3 URL 접근 차단
# - CloudFront URL로만 웹사이트 접근 가능
resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = true  # 퍼블릭 ACL 차단
  block_public_policy     = true  # 퍼블릭 버킷 정책 차단
  ignore_public_acls      = true  # 기존 퍼블릭 ACL 무시
  restrict_public_buckets = true  # 퍼블릭 버킷 접근 제한
}

# 프론트엔드용 CloudFront Origin Access Control
# - S3 버킷에 CloudFront만 접근할 수 있도록 하는 보안 설정
resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "${var.project_name}-frontend-oac"
  description                       = "OAC for ${var.project_name} frontend bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# 프론트엔드용 CloudFront Distribution
# React SPA에 최적화된 설정으로 전 세계 사용자에게 빠른 웹사이트 제공
# - 전 세계 CDN 엣지 서버를 통해 빠른 로딩 속도 제공
# - React Router 지원으로 SPA 라우팅 문제 해결
resource "aws_cloudfront_distribution" "frontend" {
  # S3 버킷을 오리진으로 설정
  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "S3-${aws_s3_bucket.frontend.id}"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  enabled             = true                # CloudFront 배포 활성화
  is_ipv6_enabled     = true                # IPv6 지원 활성화
  comment             = "${var.project_name} Frontend SPA Distribution"
  default_root_object = "index.html"        # 루트 접근 시 index.html 반환

  # React SPA에 최적화된 캐시 설정
  default_cache_behavior {
    # 모든 HTTP 메소드 허용 (SPA에서 API 호출 등에 필요)
    allowed_methods          = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods           = ["GET", "HEAD"]                                    # 읽기 전용 메소드만 캐시
    target_origin_id         = "S3-${aws_s3_bucket.frontend.id}"
    compress                 = true                                              # 자동 압축 활성화 (빠른 로딩)
    viewer_protocol_policy   = "redirect-to-https"                               # HTTPS 강제 리다이렉트
    
    # SPA용 캐시 정책: HTML은 짧게, 정적 파일은 길게 캐시
    cache_policy_id          = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"            # CachingOptimized
    origin_request_policy_id = "88a5eaf4-2fd4-4709-b370-b4c650ea3fcf"            # CORS-S3Origin
  }

  # React Router를 위한 커스텀 에러 페이지 설정
  # 404 에러 시 index.html을 반환하여 클라이언트 사이드 라우팅 지원
  # 예: /products/123 URL에 직접 접근 시 React Router가 처리하도록 함
  custom_error_response {
    error_caching_min_ttl = 0           # 에러 캐시 시간 0초 (즉시 리다이렉트)
    error_code            = 404         # 404 Not Found 에러
    response_code         = 200         # 200 OK로 변경
    response_page_path    = "/index.html" # index.html 반환
  }

  # 403 에러도 index.html로 리다이렉트 (권한 문제 시)
  custom_error_response {
    error_caching_min_ttl = 0
    error_code            = 403         # 403 Forbidden 에러
    response_code         = 200
    response_page_path    = "/index.html"
  }

  # 지리적 제한 설정
  restrictions {
    geo_restriction {
      restriction_type = "none"  # 전 세계 접근 허용
    }
  }

  # SSL 인증서 설정 (CloudFront 기본 인증서 사용)
  # 커스텀 도메인 사용 시 ACM 인증서로 변경 가능
  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Name = "${var.project_name}-frontend-cloudfront"
    Type = "Frontend-SPA-CDN"
  }
}

# 프론트엔드 S3 버킷 정책 (CloudFront만 접근 허용)
# - S3 버킷에 직접 접근을 차단하고 CloudFront를 통해서만 접근 가능
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
        Action   = "s3:GetObject"                    # 읽기 전용 권한
        Resource = "${aws_s3_bucket.frontend.arn}/*" # 버킷 내 모든 파일
        Condition = {
          StringEquals = {
            # 특정 CloudFront Distribution에서만 접근 허용
            "AWS:SourceArn" = aws_cloudfront_distribution.frontend.arn
          }
        }
      }
    ]
  })
}

# =============================================================================
# 인프라 사용 가이드
# =============================================================================

# 배포 방법:
# 1. 프론트엔드: React 빌드 결과물을 frontend S3 버킷에 업로드
# 2. 백엔드: Docker 이미지를 EC2에 배포
# 3. 이미지: 상품 이미지를 images S3 버킷에 업로드

# 비용 최적화 팁:
# 1. 개발 시 use_rds=false로 H2 DB 사용
# 2. CloudFront 캐시 설정 최적화
# 3. S3 수명주기 정책 설정으로 오래된 파일 자동 삭제
# 4. CloudWatch 로그 모니터링으로 비용 추적