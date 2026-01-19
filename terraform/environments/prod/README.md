# 운영 환경 (Production)

## 특징
- **고가용성**: Multi-AZ, 자동 확장
- **보안 강화**: WAF, 암호화, 모니터링
- **성능 최적화**: 고성능 인스턴스, 캐싱

## 리소스 구성
- ECS Fargate (고성능 스펙)
- RDS (Multi-AZ, 읽기 전용 복제본)
- ElastiCache (클러스터 모드)
- S3 (운영용 버킷, 암호화)
- DynamoDB (Provisioned, Global Tables)
- Kafka (고가용성 클러스터)
- CloudFront (CDN)
- WAF (웹 방화벽)

## 배포 명령어
```bash
cd terraform/environments/prod
terraform init
terraform plan
terraform apply
```