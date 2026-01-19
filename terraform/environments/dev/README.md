# 개발 환경 (Development)

## 특징
- **비용 최소화**: 작은 인스턴스, 단일 AZ
- **개발 편의성**: 빠른 배포, 상세한 로그
- **팀 공유**: 여러 개발자가 함께 사용

## 리소스 구성
- ECS Fargate (최소 스펙)
- RDS (Single-AZ, 백업 최소)
- ElastiCache (작은 인스턴스)
- S3 (개발용 버킷)
- DynamoDB (On-Demand)

## 배포 명령어
```bash
cd terraform/environments/dev
terraform init
terraform plan
terraform apply
```