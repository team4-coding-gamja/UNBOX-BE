# Design Document: CI/CD Pipeline

## Overview

UNBOX 백엔드 서비스를 위한 완전 자동화된 CI/CD 파이프라인. GitHub Actions를 사용하여 Dev 환경은 빠른 피드백을, Prod 환경은 무중단 배포와 3단계 Guardrail을 통한 안정성을 제공.

### 핵심 설계 원칙

1. **환경별 차별화**: Dev는 속도, Prod는 안정성
2. **보안 우선**: OIDC 기반 인증, 키 관리 불필요
3. **추적성**: Git SHA를 모든 단계에서 사용
4. **자동 복구**: 3단계 Guardrail로 자동 롤백
5. **가시성**: 모든 단계에서 Discord 알림

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      GitHub Repository                       │
│  ┌──────────────┐              ┌──────────────┐            │
│  │develop branch│              │ main branch  │            │
│  └──────┬───────┘              └──────┬───────┘            │
└─────────┼──────────────────────────────┼──────────────────┘
          │                              │
          │ (auto trigger)               │ (manual approval)
          ▼                              ▼
┌─────────────────────┐        ┌─────────────────────┐
│   Dev CI Workflow   │        │  Prod CI Workflow   │
│  ┌───────────────┐  │        │  ┌───────────────┐  │
│  │ Gradle Test   │  │        │  │ Gradle Test   │  │
│  │ Docker Build  │  │        │  │ Docker Build  │  │
│  │ ECR Push      │  │        │  │ ECR Push      │  │
│  └───────────────┘  │        │  └───────────────┘  │
└──────────┬──────────┘        └──────────┬──────────┘
           │                              │
           │ (auto trigger)               │ (manual approval)
           ▼                              ▼
┌─────────────────────┐        ┌─────────────────────┐
│   Dev CD Workflow   │        │  Prod CD Workflow   │
│  ┌───────────────┐  │        │  ┌───────────────┐  │
│  │Rolling Update │  │        │  │ Blue/Green    │  │
│  │ECS Service    │  │        │  │ CodeDeploy    │  │
│  │Health Check   │  │        │  │ 3x Guardrail  │  │
│  └───────────────┘  │        │  │ Canary 10%    │  │
└─────────────────────┘        │  └───────────────┘  │
                               └─────────────────────┘
```

### Dev Environment Flow

```
Push to develop
    ↓
[CI] Gradle Test → Docker Build → ECR Push
    ↓
[CD] Update Task Definition → Force New Deployment
    ↓
[Health Check] ALB checks /actuator/health
    ↓
Success → Discord Notification
Failure → Auto Rollback → Discord Alert
```

### Prod Environment Flow

```
Push to main (Manual Approval Required)
    ↓
[CI] Gradle Test → Docker Build → ECR Push
    ↓
[CD] Manual Approval Required
    ↓
CodeDeploy Blue/Green Deployment
    ↓
[Guardrail 1] ALB Health Check
    ├─ Pass → Continue
    └─ Fail → Stop & Terminate Green
    ↓
[Guardrail 2] Lambda Hook (AfterAllowTestTraffic)
    ├─ Pass → Continue
    └─ Fail → Stop & Terminate Green → Discord Alert
    ↓
[Guardrail 3] Canary 10% + CloudWatch Monitoring (5 min)
    ├─ No Alarm → Shift 90%
    └─ Alarm Triggered → Auto Rollback → Discord Alert
    ↓
100% Traffic on Green
    ↓
Keep Blue for 30 min → Terminate Blue
    ↓
Discord Success Notification
```

## Components and Interfaces

### 1. GitHub Actions Workflows

#### 1.1 Dev CI Workflow (`.github/workflows/dev-ci.yml`)

**Trigger**: Push to `develop` branch

**Jobs**:
- `test`: Run Gradle tests
- `build`: Build Docker image
- `push`: Push to ECR

**Outputs**:
- Docker image tagged with `$GITHUB_SHA`
- ECR image URI

#### 1.2 Dev CD Workflow (`.github/workflows/dev-cd.yml`)

**Trigger**: Completion of Dev CI workflow

**Jobs**:
- `deploy`: Update ECS service with Rolling Update

**Steps**:
1. Authenticate with AWS (OIDC)
2. Render new task definition with new image
3. Register task definition
4. Update ECS service (force new deployment)
5. Wait for service stability
6. Send Discord notification

#### 1.3 Prod CI Workflow (`.github/workflows/prod-ci.yml`)

**Trigger**: Push to `main` branch

**Manual Approval**: Required before starting

**Jobs**: Same as Dev CI

#### 1.4 Prod CD Workflow (`.github/workflows/prod-cd.yml`)

**Trigger**: Completion of Prod CI workflow

**Manual Approval**: Required before deployment

**Jobs**:
- `deploy`: Trigger CodeDeploy Blue/Green deployment

**Steps**:
1. Authenticate with AWS (OIDC)
2. Render new task definition
3. Register task definition
4. Create CodeDeploy deployment
5. Monitor deployment status
6. Send Discord notifications

### 2. AWS Components

#### 2.1 OIDC Provider

**Purpose**: Secure authentication without long-term credentials

**Configuration**:
```
Provider: token.actions.githubusercontent.com
Audience: sts.amazonaws.com
Thumbprint: (GitHub's certificate thumbprint)
```

**IAM Roles**:
- `github-actions-dev-role`: For Dev deployments
- `github-actions-prod-role`: For Prod deployments

**Trust Policy**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:ORG/REPO:ref:refs/heads/develop"
        }
      }
    }
  ]
}
```

#### 2.2 ECR (Elastic Container Registry)

**Repositories**:
- `unbox-user-service`
- `unbox-product-service`
- `unbox-order-service`
- `unbox-payment-service`
- `unbox-trade-service`

**Image Tagging Strategy**:
- Primary tag: Git commit SHA (e.g., `abc123def456`)
- Additional tags: `latest`, `dev-latest`, `prod-latest`

**Lifecycle Policy**:
- Keep last 10 images
- Delete untagged images after 7 days

#### 2.3 ECS (Elastic Container Service)

**Dev Environment**:
- Deployment Strategy: Rolling Update
- Minimum Healthy Percent: 50%
- Maximum Percent: 200%
- Health Check Grace Period: 60 seconds

**Prod Environment**:
- Deployment Controller: CODE_DEPLOY
- Deployment Strategy: Blue/Green
- Managed by CodeDeploy

#### 2.4 CodeDeploy (Prod Only)

**Application**: `unbox-prod-app`

**Deployment Group**: `unbox-prod-deployment-group`

**Deployment Configuration**:
```yaml
deploymentConfigName: CodeDeployDefault.ECSCanary10Percent5Minutes
```

**Traffic Routing**:
- Type: TimeBasedCanary
- Canary Percentage: 10%
- Canary Interval: 5 minutes

**Lifecycle Hooks**:
- `AfterAllowTestTraffic`: Lambda function for functional testing

**Automatic Rollback**:
- Enabled on deployment failure
- Enabled on CloudWatch alarm

**Termination Wait Time**: 30 minutes

#### 2.5 Lambda Hook (Prod Only)

**Function Name**: `unbox-prod-deployment-validator`

**Runtime**: Python 3.11

**Purpose**: Validate deployment before traffic shift

**Tests Performed**:
1. Health endpoint check (`/actuator/health`)
2. Critical API endpoint tests
3. Database connectivity check
4. Redis connectivity check

**Execution**:
- Triggered at `AfterAllowTestTraffic` lifecycle event
- Timeout: 5 minutes
- Return: `Succeeded` or `Failed` to CodeDeploy

**Pseudo-code**:
```python
def lambda_handler(event, context):
    deployment_id = event['DeploymentId']
    lifecycle_event_hook_execution_id = event['LifecycleEventHookExecutionId']
    
    # Get Green environment endpoint from event
    green_endpoint = get_green_endpoint(event)
    
    # Run tests
    tests = [
        test_health_endpoint(green_endpoint),
        test_critical_apis(green_endpoint),
        test_database_connection(green_endpoint),
        test_redis_connection(green_endpoint)
    ]
    
    # Check results
    if all(tests):
        status = 'Succeeded'
    else:
        status = 'Failed'
        send_discord_alert(f"Lambda Hook failed: {tests}")
    
    # Report back to CodeDeploy
    codedeploy.put_lifecycle_event_hook_execution_status(
        deploymentId=deployment_id,
        lifecycleEventHookExecutionId=lifecycle_event_hook_execution_id,
        status=status
    )
```

#### 2.6 CloudWatch Alarms (Prod Only)

**Alarm 1: High 5XX Error Rate**
```yaml
MetricName: HTTPCode_Target_5XX_Count
Namespace: AWS/ApplicationELB
Statistic: Sum
Period: 60 seconds
EvaluationPeriods: 2
Threshold: 10 errors
ComparisonOperator: GreaterThanThreshold
TreatMissingData: notBreaching
```

**Alarm 2: High Latency**
```yaml
MetricName: TargetResponseTime
Namespace: AWS/ApplicationELB
Statistic: Average
Period: 60 seconds
EvaluationPeriods: 2
Threshold: 2 seconds
ComparisonOperator: GreaterThanThreshold
TreatMissingData: notBreaching
```

**Actions**:
- Trigger CodeDeploy automatic rollback
- Send SNS notification → Discord webhook

#### 2.7 ALB (Application Load Balancer)

**Health Check Configuration**:
```yaml
HealthCheckProtocol: HTTP
HealthCheckPath: /actuator/health
HealthCheckIntervalSeconds: 30
HealthCheckTimeoutSeconds: 5
HealthyThresholdCount: 2
UnhealthyThresholdCount: 2
Matcher: 200
```

**Target Groups**:
- Blue Target Group: Current production traffic
- Green Target Group: New deployment traffic

### 3. Discord Integration

**Webhook URL**: Stored in GitHub Secrets

**Notification Types**:

1. **CI Started**
```json
{
  "embeds": [{
    "title": "🔨 CI Started",
    "description": "Environment: Dev/Prod\nBranch: develop/main\nCommit: abc123d",
    "color": 3447003
  }]
}
```

2. **CI Failed**
```json
{
  "embeds": [{
    "title": "❌ CI Failed",
    "description": "Environment: Dev/Prod\nReason: Tests failed\nCommit: abc123d",
    "color": 15158332
  }]
}
```

3. **CI Succeeded**
```json
{
  "embeds": [{
    "title": "✅ CI Succeeded",
    "description": "Environment: Dev/Prod\nImage: ECR_URI:abc123d\nDuration: 8m 32s",
    "color": 3066993
  }]
}
```

4. **CD Started**
```json
{
  "embeds": [{
    "title": "🚀 Deployment Started",
    "description": "Environment: Dev/Prod\nStrategy: Rolling/Blue-Green\nCommit: abc123d",
    "color": 3447003
  }]
}
```

5. **Deployment Failed**
```json
{
  "embeds": [{
    "title": "❌ Deployment Failed",
    "description": "Environment: Dev/Prod\nReason: Health check failed\nCommit: abc123d",
    "color": 15158332
  }]
}
```

6. **Deployment Succeeded**
```json
{
  "embeds": [{
    "title": "✅ Deployment Succeeded",
    "description": "Environment: Dev/Prod\nCommit: abc123d\nDuration: 12m 15s",
    "color": 3066993
  }]
}
```

7. **Rollback Triggered** (Urgent)
```json
{
  "embeds": [{
    "title": "🚨 ROLLBACK TRIGGERED",
    "description": "Environment: Prod\nReason: CloudWatch Alarm - High 5XX\nCommit: abc123d\nRolling back to previous version",
    "color": 15158332
  }]
}
```

## Data Models

### Task Definition Template

```json
{
  "family": "unbox-{environment}-{service-name}",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "{service-name}",
      "image": "ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com/unbox-{service-name}:IMAGE_TAG",
      "portMappings": [
        {
          "containerPort": 8081,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "{environment}"
        },
        {
          "name": "SERVER_PORT",
          "value": "8081"
        }
      ],
      "secrets": [
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:REGION:ACCOUNT_ID:secret:{environment}/db-password"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/unbox-{environment}-{service-name}",
          "awslogs-region": "ap-northeast-2",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": [
          "CMD-SHELL",
          "curl -f http://localhost:8081/actuator/health || exit 1"
        ],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

### CodeDeploy AppSpec

```yaml
version: 0.0
Resources:
  - TargetService:
      Type: AWS::ECS::Service
      Properties:
        TaskDefinition: "TASK_DEFINITION_ARN"
        LoadBalancerInfo:
          ContainerName: "{service-name}"
          ContainerPort: 8081
        PlatformVersion: "LATEST"
        NetworkConfiguration:
          AwsvpcConfiguration:
            Subnets:
              - "subnet-xxx"
              - "subnet-yyy"
            SecurityGroups:
              - "sg-xxx"
            AssignPublicIp: "DISABLED"

Hooks:
  - AfterAllowTestTraffic: "arn:aws:lambda:REGION:ACCOUNT_ID:function:unbox-prod-deployment-validator"
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: OIDC Authentication Success
*For any* GitHub Actions workflow execution, authenticating to AWS using OIDC should succeed and provide temporary credentials without using long-term access keys.

**Validates: Requirements 1.1, 1.2**

### Property 2: Workflow Failure on Test Failure
*For any* code push with failing tests, the CI workflow should stop immediately and not proceed to build or deployment stages.

**Validates: Requirements 2.3, 4.4**

### Property 3: Image Tagging Consistency
*For any* successful CI build, the Docker image tag should exactly match the Git commit SHA, ensuring traceability.

**Validates: Requirements 2.5, 11.1, 11.2**

### Property 4: Dev Deployment Speed
*For any* Dev deployment, the total time from code push to deployment completion should not exceed 15 minutes (10 min CI + 5 min CD).

**Validates: Requirements 2.7, 3.7**

### Property 5: Health Check Blocking
*For any* deployment where health checks fail, the system should immediately stop the deployment and rollback to the previous version.

**Validates: Requirements 3.6, 6.4, 14.1**

### Property 6: Manual Approval Requirement
*For any* Prod deployment, both CI and CD stages should require explicit manual approval before proceeding.

**Validates: Requirements 4.2, 5.2, 12.7**

### Property 7: Lambda Hook Failure Handling
*For any* Prod deployment where Lambda Hook returns FAILED status, CodeDeploy should immediately stop the deployment and terminate the Green environment.

**Validates: Requirements 7.5, 7.6, 14.2**

### Property 8: Canary Traffic Distribution
*For any* Prod deployment during Canary phase, exactly 10% of traffic should be routed to Green environment and 90% to Blue environment.

**Validates: Requirements 8.2**

### Property 9: CloudWatch Alarm Rollback
*For any* Prod deployment where CloudWatch Alarm triggers during Canary phase, CodeDeploy should automatically rollback to Blue environment within 2 minutes.

**Validates: Requirements 8.8, 8.9, 14.3, 14.7**

### Property 10: Blue Environment Retention
*For any* successful Prod deployment, the Blue environment should remain running for exactly 30 minutes before termination.

**Validates: Requirements 9.2, 9.3**

### Property 11: Discord Notification Completeness
*For any* deployment (success or failure), Discord notifications should be sent at all key stages: CI start, CI result, CD start, CD result, and any rollback events.

**Validates: Requirements 10.1-10.7**

### Property 12: Environment Strategy Separation
*For any* Dev deployment, the system should use Rolling Update without CodeDeploy, Lambda Hook, or Canary deployment, while Prod deployments should use all these features.

**Validates: Requirements 12.1-12.10**

### Property 13: Rollback Completeness
*For any* rollback event, the system should restore the previous task definition, shift 100% traffic back to Blue, and complete within 2 minutes.

**Validates: Requirements 14.5, 14.6, 14.7**

## Error Handling

### CI Stage Errors

**Test Failure**:
- Action: Stop workflow immediately
- Notification: Discord alert with test failure details
- Recovery: Developer fixes tests and pushes again

**Build Failure**:
- Action: Stop workflow immediately
- Notification: Discord alert with build error logs
- Recovery: Developer fixes build issues and pushes again

**ECR Push Failure**:
- Action: Retry up to 3 times
- Notification: Discord alert if all retries fail
- Recovery: Check ECR permissions and network connectivity

### CD Stage Errors

**Dev Environment**:

**Health Check Failure**:
- Action: ECS automatically rolls back to previous task definition
- Notification: Discord alert
- Recovery: Developer investigates health check endpoint

**Task Start Failure**:
- Action: ECS stops deployment
- Notification: Discord alert with task failure reason
- Recovery: Check task definition, resource limits, and logs

**Prod Environment**:

**Health Check Failure** (Guardrail 1):
- Action: CodeDeploy stops deployment, terminates Green environment
- Notification: Discord alert
- Recovery: Investigate health check endpoint, fix issues, redeploy

**Lambda Hook Failure** (Guardrail 2):
- Action: CodeDeploy stops deployment, terminates Green environment
- Notification: Discord warning alert with Lambda test results
- Recovery: Check Lambda logs, fix failing tests, redeploy

**CloudWatch Alarm** (Guardrail 3):
- Action: CodeDeploy automatically rolls back to Blue
- Notification: Discord urgent alert
- Recovery: Analyze CloudWatch metrics, fix performance/error issues, redeploy

**CodeDeploy Timeout**:
- Action: Automatic rollback after 1 hour
- Notification: Discord alert
- Recovery: Check deployment logs, increase timeout if needed

## Testing Strategy

### Unit Tests

**GitHub Actions Workflow Syntax**:
- Validate YAML syntax
- Check required secrets are defined
- Verify job dependencies

**Task Definition Templates**:
- Validate JSON structure
- Check required fields are present
- Verify environment variables

**Lambda Hook Function**:
- Test individual test functions
- Mock HTTP requests
- Test CodeDeploy status reporting

### Integration Tests

**Dev CI/CD Pipeline**:
1. Push test commit to develop branch
2. Verify CI workflow triggers
3. Verify tests run successfully
4. Verify Docker image is built and pushed
5. Verify CD workflow triggers
6. Verify ECS service updates
7. Verify health checks pass
8. Verify Discord notifications sent

**Prod CI/CD Pipeline**:
1. Push test commit to main branch
2. Verify manual approval is required
3. Approve and verify CI runs
4. Verify manual approval for CD
5. Approve and verify CodeDeploy deployment starts
6. Verify health checks pass
7. Verify Lambda Hook executes and passes
8. Verify Canary deployment (10%)
9. Wait 5 minutes, verify no alarms
10. Verify traffic shifts to 100%
11. Verify Blue environment maintained for 30 min
12. Verify Discord notifications at each stage

**Failure Scenarios**:

Test 1: Failing Tests
- Introduce failing test
- Push to develop
- Verify workflow stops at test stage
- Verify Discord alert sent

Test 2: Unhealthy Deployment
- Deploy app with failing health check
- Verify deployment stops
- Verify rollback occurs
- Verify Discord alert sent

Test 3: Lambda Hook Failure
- Configure Lambda to return FAILED
- Deploy to Prod
- Verify deployment stops after Lambda Hook
- Verify Green environment terminated
- Verify Discord alert sent

Test 4: CloudWatch Alarm Trigger
- Deploy app that generates 5XX errors
- Verify Canary deployment starts
- Verify CloudWatch Alarm triggers
- Verify automatic rollback
- Verify Discord urgent alert sent

### Property-Based Tests

Due to the nature of CI/CD pipelines (deployment workflows), property-based testing is not applicable. All testing will be done through integration tests and manual verification.

## Deployment Strategy

### Initial Setup

1. **Create OIDC Provider in AWS**
   - Add GitHub OIDC provider
   - Create IAM roles for Dev and Prod
   - Configure trust policies

2. **Create ECR Repositories**
   - One repository per service
   - Configure lifecycle policies

3. **Configure GitHub Secrets**
   - `AWS_ACCOUNT_ID`
   - `AWS_REGION`
   - `DEV_IAM_ROLE_ARN`
   - `PROD_IAM_ROLE_ARN`
   - `DISCORD_WEBHOOK_URL`

4. **Create Lambda Hook Function** (Prod only)
   - Deploy validation function
   - Configure IAM permissions
   - Test function manually

5. **Create CloudWatch Alarms** (Prod only)
   - Configure 5XX error alarm
   - Configure latency alarm
   - Link to CodeDeploy

6. **Create CodeDeploy Application** (Prod only)
   - Create application
   - Create deployment group
   - Configure Blue/Green settings
   - Add Lambda Hook
   - Enable automatic rollback

### Rollout Plan

**Phase 1: Dev Environment**
- Deploy Dev CI/CD workflows
- Test with one service (user-service)
- Verify all stages work correctly
- Fix any issues

**Phase 2: Dev All Services**
- Roll out to all 5 services
- Monitor for issues
- Gather feedback from developers

**Phase 3: Prod Environment**
- Deploy Prod CI/CD workflows
- Test with one service (user-service)
- Verify Blue/Green deployment
- Test all 3 Guardrails
- Test rollback scenarios

**Phase 4: Prod All Services**
- Roll out to all 5 services
- Monitor closely for first week
- Document any issues and resolutions

### Rollback Plan

**If CI/CD Pipeline Fails**:
- Revert workflow files to previous version
- Deploy manually using AWS Console
- Fix issues in separate branch
- Test thoroughly before re-deploying

**If Deployment Causes Production Issues**:
- Use CodeDeploy console to manually rollback
- Or deploy previous Git commit SHA
- Investigate root cause
- Fix and redeploy

## Monitoring and Observability

### Metrics to Track

**CI Metrics**:
- CI success rate
- CI duration (p50, p95, p99)
- Test failure rate
- Build failure rate

**CD Metrics**:
- Deployment success rate
- Deployment duration
- Rollback frequency
- Health check failure rate

**Prod-Specific Metrics**:
- Lambda Hook success rate
- Canary deployment success rate
- CloudWatch Alarm trigger frequency
- Time to rollback

### Dashboards

**CloudWatch Dashboard**: `unbox-cicd-metrics`
- CI/CD success rates
- Deployment durations
- Error rates during deployments
- Rollback events

**GitHub Actions Dashboard**:
- Workflow run history
- Success/failure trends
- Duration trends

### Alerts

**Critical Alerts** (Discord urgent channel):
- Prod deployment rollback
- Lambda Hook failure
- CloudWatch Alarm trigger
- Multiple consecutive deployment failures

**Warning Alerts** (Discord warning channel):
- Dev deployment failure
- CI test failures
- Slow deployment (>20 minutes)

**Info Alerts** (Discord info channel):
- Successful deployments
- CI completion
- Deployment started

## Cost Estimation

### Dev Environment (per deployment)
- GitHub Actions minutes: ~10 minutes = $0.008
- ECR storage: Negligible
- ECS task updates: Free
- CloudWatch Logs: ~$0.01
- **Total per deployment: ~$0.02**

### Prod Environment (per deployment)
- GitHub Actions minutes: ~15 minutes = $0.012
- CodeDeploy: Free (included in ECS)
- Lambda Hook execution: ~$0.0001
- CloudWatch Alarms: $0.10/month per alarm = $0.20/month
- CloudWatch Logs: ~$0.02
- **Total per deployment: ~$0.03**
- **Monthly fixed cost: $0.20 (alarms)**

### Monthly Estimate
- Dev deployments: 50/month × $0.02 = $1.00
- Prod deployments: 10/month × $0.03 = $0.30
- Fixed costs: $0.20
- **Total: ~$1.50/month**

**Note**: This is extremely cost-effective. Main costs are in the infrastructure (ECS, RDS, etc.), not the CI/CD pipeline itself.
