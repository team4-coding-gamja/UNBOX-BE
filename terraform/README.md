# UNBOX Infrastructure as Code

MSA 환경을 위한 Terraform 인프라 구성

## 디렉토리 구조

```
terraform/
├── backend/                    # Terraform 상태 파일 원격 저장소
├── environments/               # 환경별 설정
│   ├── dev/                   # 개발 환경 (비용 최소화)
│   ├── staging/               # 스테이징 환경 (운영 유사)
│   └── prod/                  # 운영 환경 (고가용성)
├── modules/                   # 재사용 가능한 모듈들
│   ├── networking/            # VPC, 서브넷, 라우팅
│   ├── security/              # 보안 그룹, IAM, WAF
│   ├── compute/               # ECS, Fargate, EC2
│   ├── database/              # RDS, DynamoDB
│   ├── storage/               # S3, EFS
│   ├── messaging/             # Kafka, SQS, SNS
│   ├── monitoring/            # CloudWatch, X-Ray
│   └── cicd/                  # CodePipeline, CodeBuild
└── shared/                    # 공통 설정
    ├── locals.tf              # 공통 변수
    └── data.tf               # 공통 데이터 소스
```

## 환경별 특징

### Dev 환경
- 비용 최소화 우선
- 단일 AZ 배포
- 작은 인스턴스 타입
- 백업 최소화

### Staging 환경  
- 운영과 유사한 구성
- Multi-AZ 배포
- 성능 테스트 가능

### Prod 환경
- 고가용성 우선
- Multi-AZ 배포
- 자동 확장
- 완전한 모니터링

### 모듈별 디렉토리 (modules/)
- networking/: VPC, 서브넷, 라우팅
- security/: 보안 그룹, IAM, WAF
- compute/: ECS, Fargate, ALB
- database/: RDS, DynamoDB, ElastiCache
- storage/: S3, CloudFront, EFS
- messaging/: Kafka, SQS, SNS
- monitoring/: CloudWatch, X-Ray, 알람
- cicd/: ECR, CodePipeline, CodeBuild