# ============================================
# ECR Repositories
# ============================================

# Product Service ECR
resource "aws_ecr_repository" "product_service" {
  name                 = "${local.project}-${local.services.product}"
  image_tag_mutability = "MUTABLE"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  encryption_configuration {
    encryption_type = "AES256"
  }
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.services.product}"
    Service = local.services.product
  })
}

# Order Service ECR
resource "aws_ecr_repository" "order_service" {
  name                 = "${local.project}-${local.services.order}"
  image_tag_mutability = "MUTABLE"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  encryption_configuration {
    encryption_type = "AES256"
  }
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.services.order}"
    Service = local.services.order
  })
}

# Payment Service ECR
resource "aws_ecr_repository" "payment_service" {
  name                 = "${local.project}-${local.services.payment}"
  image_tag_mutability = "MUTABLE"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  encryption_configuration {
    encryption_type = "AES256"
  }
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.services.payment}"
    Service = local.services.payment
  })
}

# Trade Service ECR
resource "aws_ecr_repository" "trade_service" {
  name                 = "${local.project}-${local.services.trade}"
  image_tag_mutability = "MUTABLE"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  encryption_configuration {
    encryption_type = "AES256"
  }
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.services.trade}"
    Service = local.services.trade
  })
}

# User Service ECR
resource "aws_ecr_repository" "user_service" {
  name                 = "${local.project}-${local.services.user}"
  image_tag_mutability = "MUTABLE"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  encryption_configuration {
    encryption_type = "AES256"
  }
  
  tags = merge(local.common_tags, {
    Name    = "${local.project}-${local.services.user}"
    Service = local.services.user
  })
}

# ============================================
# ECR Lifecycle Policies
# ============================================

# Product Service - 최근 N개 이미지만 유지
resource "aws_ecr_lifecycle_policy" "product_service" {
  repository = aws_ecr_repository.product_service.name
  
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last ${var.ecr_image_retention_count} images"
        selection = {
          tagStatus     = "any"
          countType     = "imageCountMoreThan"
          countNumber   = var.ecr_image_retention_count
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# Order Service
resource "aws_ecr_lifecycle_policy" "order_service" {
  repository = aws_ecr_repository.order_service.name
  
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last ${var.ecr_image_retention_count} images"
        selection = {
          tagStatus     = "any"
          countType     = "imageCountMoreThan"
          countNumber   = var.ecr_image_retention_count
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# Payment Service
resource "aws_ecr_lifecycle_policy" "payment_service" {
  repository = aws_ecr_repository.payment_service.name
  
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last ${var.ecr_image_retention_count} images"
        selection = {
          tagStatus     = "any"
          countType     = "imageCountMoreThan"
          countNumber   = var.ecr_image_retention_count
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# Trade Service
resource "aws_ecr_lifecycle_policy" "trade_service" {
  repository = aws_ecr_repository.trade_service.name
  
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last ${var.ecr_image_retention_count} images"
        selection = {
          tagStatus     = "any"
          countType     = "imageCountMoreThan"
          countNumber   = var.ecr_image_retention_count
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# User Service
resource "aws_ecr_lifecycle_policy" "user_service" {
  repository = aws_ecr_repository.user_service.name
  
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last ${var.ecr_image_retention_count} images"
        selection = {
          tagStatus     = "any"
          countType     = "imageCountMoreThan"
          countNumber   = var.ecr_image_retention_count
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
