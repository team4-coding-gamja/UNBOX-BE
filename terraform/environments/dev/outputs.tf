# =============================================================================
# UNBOX 백엔드 인프라 출력값
# 팀원들이 로컬에서 접속할 때 필요한 정보들
# =============================================================================

# EC2 퍼블릭 IP 주소
# - 팀원들이 브라우저에서 Swagger UI 접속 시 사용
# - 형식: http://{EC2_PUBLIC_IP}:8080/swagger-ui/index.html
output "ec2_public_ip" {
  description = "EC2 퍼블릭 IP (Swagger UI 접속용)"
  value       = module.compute.public_ip
}

# Swagger UI 접속 URL
# - 팀원들이 바로 복사해서 사용할 수 있는 완전한 URL
# - 기능 테스트 시 이 URL로 접속하여 API 테스트 가능
output "swagger_ui_url" {
  description = "Swagger UI 접속 URL (팀원용)"
  value       = "http://${module.compute.public_ip}:8080/swagger-ui/index.html"
}

# RDS 데이터베이스 엔드포인트
# - use_rds=true일 때만 표시
# - 로컬 개발 시 DB 연결 정보로 사용 가능
output "database_info" {
  description = "데이터베이스 연결 정보"
  value = var.use_rds ? {
    endpoint = module.database[0].db_endpoint
    database = "unboxdb"
    username = "admin"
    note     = "비밀번호는 별도 공유"
  } : {
    type = "H2 인메모리 DB"
    note = "로컬 테스트용 - 별도 설정 불필요"
  }
}

# 로그 확인 방법 안내 (Spring Boot Logback)
# - EC2에서 직접 로그 파일 확인
# - Docker 컨테이너 로그 확인
output "log_access_guide" {
  description = "로그 확인 방법 가이드 (Logback)"
  value = {
    # EC2 접속
    ssh_command = "ssh -i ~/.ssh/unbox_key ec2-user@${module.compute.public_ip}"
    
    # Docker 로그 확인
    app_logs = "docker logs unbox-mvp-app"
    app_logs_tail = "docker logs -f unbox-mvp-app"
    redis_logs = "docker logs unbox-mvp-redis"
    
    # Spring Boot 로그 파일 확인
    log_directory = "/var/log/unbox/"
    view_logs = "tail -f /var/log/unbox/application.log"
    error_logs = "tail -f /var/log/unbox/error.log"
  }
}