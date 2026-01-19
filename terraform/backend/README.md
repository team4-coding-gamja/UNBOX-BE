# Terraform Backend 설정

Terraform 상태 파일을 S3에 원격 저장하고 DynamoDB로 잠금 관리

## 구성 요소

- **S3 버킷**: Terraform 상태 파일 저장
- **DynamoDB 테이블**: 상태 파일 잠금 관리
- **IAM 정책**: 백엔드 접근 권한

## 배포 순서

1. 이 디렉토리에서 먼저 백엔드 리소스 생성
2. 각 환경에서 백엔드 설정 참조