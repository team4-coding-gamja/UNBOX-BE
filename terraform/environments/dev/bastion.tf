# 임시 Bastion Host (DB 설정용)

# 1. Bastion Host용 보안 그룹
resource "aws_security_group" "bastion" {
  name        = "unbox-dev-bastion-sg"
  description = "Security group for bastion host"
  vpc_id      = module.vpc.vpc_id

  # SSH 접속 허용 (내 IP만)
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # 보안을 위해 나중에 내 IP로 제한
  }

  # 모든 아웃바운드 허용
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "unbox-dev-bastion-sg"
  }
}

# 2. RDS 보안 그룹에 Bastion에서의 접근 허용
resource "aws_security_group_rule" "rds_from_bastion" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  security_group_id        = module.security_group.rds_sg_ids["user"]
  source_security_group_id = aws_security_group.bastion.id
  description              = "Allow PostgreSQL from Bastion"
}

# 3. 최신 Amazon Linux 2023 AMI
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# 3.5. IAM Role for SSM
resource "aws_iam_role" "bastion_role" {
  name = "unbox-dev-bastion-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "bastion_ssm" {
  role       = aws_iam_role.bastion_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "bastion_profile" {
  name = "unbox-dev-bastion-profile"
  role = aws_iam_role.bastion_role.name
}

# 4. Bastion Host EC2 인스턴스
resource "aws_instance" "bastion" {
  ami           = data.aws_ami.amazon_linux_2023.id
  instance_type = "t3.micro"
  key_name      = "unbox-bastion-temp"
  
  subnet_id                   = module.vpc.public_subnet_ids[0]
  vpc_security_group_ids      = [aws_security_group.bastion.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.bastion_profile.name

  # PostgreSQL 클라이언트 자동 설치
  user_data = <<-EOF
              #!/bin/bash
              yum update -y
              yum install -y postgresql15
              EOF

  tags = {
    Name = "unbox-dev-bastion-temp"
  }
}

# 5. Outputs
output "bastion_public_ip" {
  value       = aws_instance.bastion.public_ip
  description = "Bastion Host Public IP"
}

output "bastion_connect_command" {
  value       = "ssh -i ~/.ssh/your-key.pem ec2-user@${aws_instance.bastion.public_ip}"
  description = "SSH command to connect to bastion"
}

output "psql_command" {
  value       = "psql -h ${module.rds.db_endpoints["common"]} -U unbox_admin -d postgres"
  description = "PostgreSQL connection command from bastion"
}
