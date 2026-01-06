# =============================================================================
# Compute 모듈 출력값
# EC2 인스턴스 정보를 다른 모듈이나 환경에서 참조할 수 있도록 제공
# =============================================================================

# EC2 인스턴스 ID
# - AWS 콘솔에서 인스턴스 확인 시 사용
# - 다른 리소스에서 EC2 참조 시 사용
output "instance_id" {
  description = "EC2 인스턴스 ID"
  value       = aws_instance.web.id
}

# EC2 자동 할당 퍼블릭 IP 주소
# - 팀원들이 Swagger UI 접속 시 사용
# - SSH 접속 시 사용
# - 인스턴스 재시작 시 IP 변경될 수 있음 (개발용으로는 충분)
output "public_ip" {
  description = "EC2 자동 할당 퍼블릭 IP"
  value       = aws_instance.web.public_ip
}

# EC2 프라이빗 IP 주소
# - VPC 내부 통신용 (RDS 연결 등)
# - 보안 그룹 설정 시 참조 가능
output "private_ip" {
  description = "EC2 프라이빗 IP"
  value       = aws_instance.web.private_ip
}