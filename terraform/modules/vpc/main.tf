# =============================================================================
# VPC (Virtual Private Cloud) 구성
# AWS 내에서 격리된 네트워크 환경 생성
# =============================================================================

# 메인 VPC 생성
# - 전체 네트워크의 기반이 되는 가상 네트워크
# - 10.0.0.0/16 대역대로 65,536개 IP 주소 사용 가능
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"  # 사설 IP 대역대 설정
  enable_dns_hostnames = true            # DNS 호스트명 해석 활성화
  enable_dns_support   = true            # DNS 지원 활성화
  
  tags = {
    Name = "${var.project_name}-vpc"  # 리소스 이름 태그
  }
}

# 인터넷 게이트웨이 생성
# - VPC와 인터넷 간의 연결 지점
# - Public 서브넷에서 외부 인터넷에 접근하기 위해 필수
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id  # 위에서 생성한 VPC에 연결
  
  tags = {
    Name = "${var.project_name}-igw"
  }
}

# =============================================================================
# 서브넷 구성 - Public/Private 분리로 보안 강화
# =============================================================================

# Public 서브넷 생성
# - 인터넷에 직접 연결 가능한 서브넷
# - EC2 인스턴스(웹서버)를 배치하여 외부에서 접근 가능
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"        # 256개 IP 주소 (10.0.1.0 ~ 10.0.1.255)
  availability_zone       = "ap-northeast-2a"     # 서울 리전 AZ-A
  map_public_ip_on_launch = true                  # 인스턴스 생성 시 자동으로 공인 IP 할당
  
  tags = {
    Name = "${var.project_name}-public-subnet"
  }
}

# Private 서브넷 A 생성 (AZ-A)
# - 인터넷에 직접 연결되지 않는 서브넷
# - 데이터베이스 등 보안이 중요한 리소스 배치
resource "aws_subnet" "private_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.2.0/24"      # 256개 IP 주소 (10.0.2.0 ~ 10.0.2.255)
  availability_zone = "ap-northeast-2a"   # 서울 리전 AZ-A
  
  tags = {
    Name = "${var.project_name}-private-a"
  }
}

# Private 서브넷 C 생성 (AZ-C)
# - RDS 서브넷 그룹 최소 요구사항을 위한 추가 서브넷
# - 실제로는 사용하지 않지만 RDS 생성을 위해 2개 AZ 필요
# - 비용 절약: 실제 리소스는 AZ-A에만 배치
resource "aws_subnet" "private_c" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.3.0/24"      # 256개 IP 주소 (10.0.3.0 ~ 10.0.3.255)
  availability_zone = "ap-northeast-2c"   # 서울 리전 AZ-C
  
  tags = {
    Name = "${var.project_name}-private-c"
  }
}

# =============================================================================
# 라우팅 테이블 구성 - 네트워크 트래픽 제어
# =============================================================================

# Public 서브넷용 라우팅 테이블
# - Public 서브넷의 트래픽을 인터넷 게이트웨이로 라우팅
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  
  # 모든 외부 트래픽(0.0.0.0/0)을 인터넷 게이트웨이로 전송
  route {
    cidr_block = "0.0.0.0/0"                    # 모든 IP 대역
    gateway_id = aws_internet_gateway.main.id    # 인터넷 게이트웨이로 라우팅
  }
  
  tags = {
    Name = "${var.project_name}-public-rt"
  }
}

# 라우팅 테이블과 Public 서브넷 연결
# - Public 서브넷에 위에서 정의한 라우팅 규칙 적용
resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id      # Public 서브넷 ID
  route_table_id = aws_route_table.public.id # Public 라우팅 테이블 ID
}