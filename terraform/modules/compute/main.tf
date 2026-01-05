resource "aws_key_pair" "main" {
  key_name   = "${var.project_name}-key"
  public_key = var.public_key
}

resource "aws_instance" "web" {
  ami                    = "ami-0c2d3e23e757b5d84"
  instance_type          = "t3.micro"
  key_name               = aws_key_pair.main.key_name
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [var.security_group_id]

  user_data = base64encode(<<-EOF
    #!/bin/bash
    yum update -y
    yum install -y docker
    systemctl start docker
    systemctl enable docker
    usermod -a -G docker ec2-user
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    yum install -y git
    
    # PostgreSQL 대신 H2 데이터베이스 사용 설정
    echo "export USE_H2_DATABASE=true" >> /home/ec2-user/.bashrc
    echo "export REDIS_HOST=localhost" >> /home/ec2-user/.bashrc
  EOF
  )

  tags = {
    Name = "${var.project_name}-web"
  }
}

# =============================================================================
# Elastic IP (EIP) - 고정 퍼블릭 IP 주소
# =============================================================================

# EC2 인스턴스에 고정 퍼블릭 IP를 할당하는 Elastic IP
# 용도: 
# 1. EC2 재시작/교체 시에도 동일한 IP 주소 유지
# 2. 프론트엔드에서 백엔드 API 호출 시 고정 엔드포인트 사용
# 3. DNS 설정 또는 외부 서비스 연동 시 IP 변경 걱정 없음
resource "aws_eip" "web" {
  instance = aws_instance.web.id  # EC2 인스턴스에 연결
  domain   = "vpc"                # VPC 내에서 사용
  
  tags = {
    Name = "${var.project_name}-web-eip"
    Type = "Web-Server-EIP"
  }
  
  # EC2 인스턴스가 완전히 시작된 후 EIP 연결
  # 이렇게 하지 않으면 EC2 생성 중에 EIP 연결을 시도하여 에러 발생 가능
  depends_on = [aws_instance.web]
}

# EIP 사용 시 주의사항:
# 1. EIP가 EC2에 연결되어 사용 중일 때: 무료
# 2. EIP가 생성되었지만 사용하지 않을 때: 시간당 과금
# 3. EC2 삭제 시 EIP도 함께 삭제해야 과금 방지