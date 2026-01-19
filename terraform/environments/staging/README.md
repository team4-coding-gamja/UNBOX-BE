# 스테이징 환경 (Staging)

## 특징
- **운영 유사**: 운영과 거의 동일한 구성
- **성능 테스트**: 실제 부하 테스트 가능
- **QA 환경**: 최종 테스트 및 검증

## 리소스 구성
- ECS Fargate (중간 스펙)
- RDS (Multi-AZ, 백업 활성화)
- ElastiCache (중간 인스턴스)
- S3 (스테이징용 버킷)
- DynamoDB (Provisioned)
- Kafka (작은 클러스터)

## 배포 명령어
```bash
cd terraform/environments/staging
terraform init
terraform plan
terraform apply
```