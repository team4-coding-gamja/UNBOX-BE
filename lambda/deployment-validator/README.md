# CodeDeploy Deployment Validator Lambda

## 📋 개요

이 Lambda 함수는 CodeDeploy Blue/Green 배포 중 **AfterAllowTestTraffic** 단계에서 실행되어 Green 환경의 헬스를 검증합니다.

## 🎯 검증 항목

1. **Health Check**: `/actuator/health` 엔드포인트 응답 확인
2. **Response Time**: 2초 이내 응답 확인
3. **HTTP Status**: 200 OK 확인

## 🚀 배포 방법

### 1. Lambda 함수 생성

```bash
# 1. 코드 압축
cd lambda/deployment-validator
zip -r function.zip handler.py

# 2. Lambda 함수 생성
aws lambda create-function \
  --function-name unbox-prod-deployment-validator \
  --runtime python3.11 \
  --role arn:aws:iam::ACCOUNT_ID:role/lambda-codedeploy-role \
  --handler handler.lambda_handler \
  --zip-file fileb://function.zip \
  --timeout 300 \
  --memory-size 256 \
  --region ap-northeast-2
```

### 2. IAM Role 생성

Lambda 함수가 필요로 하는 권한:
- CodeDeploy: `PutLifecycleEventHookExecutionStatus`
- ELB: `DescribeTargetGroups`, `DescribeTargetHealth`
- CloudWatch Logs: `CreateLogGroup`, `CreateLogStream`, `PutLogEvents`

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "codedeploy:PutLifecycleEventHookExecutionStatus"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "elasticloadbalancing:DescribeTargetGroups",
        "elasticloadbalancing:DescribeTargetHealth"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

### 3. 환경 변수 설정

```bash
aws lambda update-function-configuration \
  --function-name unbox-prod-deployment-validator \
  --environment Variables={ALB_DNS=unbox-prod-alb-xxx.ap-northeast-2.elb.amazonaws.com} \
  --region ap-northeast-2
```

## 🔗 CodeDeploy 연결

AppSpec 파일에 Lambda Hook 추가:

```yaml
Hooks:
  - AfterAllowTestTraffic: "arn:aws:lambda:ap-northeast-2:ACCOUNT_ID:function:unbox-prod-deployment-validator"
```

## 🧪 테스트 방법

### 로컬 테스트

```python
# test_event.json
{
  "DeploymentId": "d-XXXXXXXXX",
  "LifecycleEventHookExecutionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

```bash
# Lambda 함수 테스트
aws lambda invoke \
  --function-name unbox-prod-deployment-validator \
  --payload file://test_event.json \
  --region ap-northeast-2 \
  response.json

cat response.json
```

### CloudWatch Logs 확인

```bash
# 최근 로그 확인
aws logs tail /aws/lambda/unbox-prod-deployment-validator --follow
```

## 📊 검증 결과

### 성공 시
- CodeDeploy에 `Succeeded` 상태 보고
- 배포 계속 진행 (Canary → 100%)

### 실패 시
- CodeDeploy에 `Failed` 상태 보고
- 배포 중단 및 자동 롤백
- Green 환경 종료

## 🔧 커스터마이징

### 추가 검증 항목

`handler.py`의 `run_validation_tests` 함수에 추가:

```python
def run_validation_tests(endpoint):
    results = {}
    
    # 기존 테스트
    results['health_check'] = test_health_endpoint(endpoint)
    results['response_time'] = test_response_time(endpoint)
    
    # 추가 테스트
    results['database_check'] = test_database_connection(endpoint)
    results['redis_check'] = test_redis_connection(endpoint)
    results['api_check'] = test_critical_apis(endpoint)
    
    return results
```

### 타임아웃 조정

```bash
aws lambda update-function-configuration \
  --function-name unbox-prod-deployment-validator \
  --timeout 600 \
  --region ap-northeast-2
```

## 📝 로그 예시

### 성공 로그
```
Received event: {"DeploymentId": "d-XXX", ...}
Green endpoint: http://unbox-prod-alb-xxx.elb.amazonaws.com
Testing health endpoint: http://unbox-prod-alb-xxx.elb.amazonaws.com/actuator/health
Health check status: 200
Testing response time: http://unbox-prod-alb-xxx.elb.amazonaws.com/actuator/health
Response time: 0.45 seconds
Validation results: {"health_check": true, "response_time": true}
Overall status: Succeeded
Reported status 'Succeeded' to CodeDeploy
```

### 실패 로그
```
Received event: {"DeploymentId": "d-XXX", ...}
Green endpoint: http://unbox-prod-alb-xxx.elb.amazonaws.com
Testing health endpoint: http://unbox-prod-alb-xxx.elb.amazonaws.com/actuator/health
Health check status: 503
Health check failed: Service Unavailable
Validation results: {"health_check": false, "response_time": false}
Overall status: Failed
Reported status 'Failed' to CodeDeploy
```

## 🆘 트러블슈팅

### Lambda 타임아웃
- 타임아웃 시간 증가 (기본 300초)
- 네트워크 연결 확인

### 권한 오류
- IAM Role 권한 확인
- CodeDeploy 리소스 ARN 확인

### 엔드포인트 연결 실패
- ALB DNS 환경 변수 확인
- Security Group 설정 확인
- VPC 설정 확인 (Lambda가 VPC 내부에 있는 경우)

## 📚 참고 문서

- [AWS CodeDeploy Lambda Hooks](https://docs.aws.amazon.com/codedeploy/latest/userguide/reference-appspec-file-structure-hooks.html)
- [Lambda Python Runtime](https://docs.aws.amazon.com/lambda/latest/dg/lambda-python.html)
