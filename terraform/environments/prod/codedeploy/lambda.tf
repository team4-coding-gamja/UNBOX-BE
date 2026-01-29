# ============================================
# Lambda Deployment Validator
# ============================================

# Lambda 함수 코드 압축
data "archive_file" "lambda_zip" {
  type        = "zip"
  source_file = "${path.module}/../../../../lambda/deployment-validator/handler.py"
  output_path = "${path.module}/lambda-function.zip"
}

# ============================================
# Lambda IAM Role
# ============================================

resource "aws_iam_role" "lambda_validator" {
  name = "${var.project_name}-${var.env}-lambda-validator-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = {
    Name        = "${var.project_name}-${var.env}-lambda-validator-role"
    Environment = var.env
    Project     = var.project_name
  }
}

# ============================================
# Lambda IAM Policy
# ============================================

resource "aws_iam_role_policy" "lambda_validator_policy" {
  name = "${var.project_name}-${var.env}-lambda-validator-policy"
  role = aws_iam_role.lambda_validator.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "codedeploy:PutLifecycleEventHookExecutionStatus"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "elasticloadbalancing:DescribeTargetGroups",
          "elasticloadbalancing:DescribeTargetHealth",
          "elasticloadbalancing:DescribeLoadBalancers"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "codedeploy:GetDeployment",
          "codedeploy:GetDeploymentGroup",
          "codedeploy:GetApplication"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

# ============================================
# CloudWatch Log Group
# ============================================

resource "aws_cloudwatch_log_group" "lambda_validator" {
  name              = "/aws/lambda/${var.project_name}-${var.env}-deployment-validator"
  retention_in_days = 30

  tags = {
    Name        = "${var.project_name}-${var.env}-lambda-validator-logs"
    Environment = var.env
    Project     = var.project_name
  }
}

# ============================================
# Lambda Function
# ============================================

resource "aws_lambda_function" "deployment_validator" {
  filename         = data.archive_file.lambda_zip.output_path
  function_name    = "${var.project_name}-${var.env}-deployment-validator"
  role            = aws_iam_role.lambda_validator.arn
  handler         = "handler.lambda_handler"
  source_code_hash = data.archive_file.lambda_zip.output_base64sha256
  runtime         = "python3.11"
  timeout         = 300
  memory_size     = 256

  environment {
    variables = {
      ALB_DNS     = var.alb_dns_name
      ENVIRONMENT = var.env
      PROJECT     = var.project_name
    }
  }

  depends_on = [
    aws_cloudwatch_log_group.lambda_validator,
    aws_iam_role_policy.lambda_validator_policy
  ]

  tags = {
    Name        = "${var.project_name}-${var.env}-deployment-validator"
    Environment = var.env
    Project     = var.project_name
  }
}

# ============================================
# Lambda Permission for CodeDeploy
# ============================================

resource "aws_lambda_permission" "allow_codedeploy" {
  statement_id  = "AllowExecutionFromCodeDeploy"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.deployment_validator.function_name
  principal     = "codedeploy.amazonaws.com"
}
