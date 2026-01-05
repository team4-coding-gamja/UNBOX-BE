output "instance_id" {
  description = "EC2 인스턴스 ID"
  value       = aws_instance.web.id
}

# EC2의 고정 퍼블릭 IP 주소 (EIP)
# 프론트엔드에서 백엔드 API 호출 시 사용
output "public_ip" {
  description = "EC2 고정 퍼블릭 IP (EIP)"
  value       = aws_eip.web.public_ip
}

# EC2의 내부 네트워크 IP 주소
# VPC 내부 통신용 (RDS 연결)
output "private_ip" {
  description = "EC2 프라이빗 IP"
  value       = aws_instance.web.private_ip
}

# EIP Allocation ID
# AWS CLI나 다른 리소스에서 EIP를 참조할 때 사용
output "eip_allocation_id" {
  description = "EIP Allocation ID"
  value       = aws_eip.web.allocation_id
}

# EIP Association ID
# EIP와 EC2 인스턴스 간의 연결 ID
output "eip_association_id" {
  description = "EIP Association ID"
  value       = aws_eip.web.association_id
}