# Requirements Document: CI/CD Pipeline

## Introduction

UNBOX 백엔드 서비스를 위한 자동화된 CI/CD 파이프라인 구축. Dev 환경은 빠른 배포를, Prod 환경은 무중단 배포와 자동 롤백을 통한 안정성을 목표로 함.

## Glossary

- **CI (Continuous Integration)**: 코드 변경사항을 자동으로 빌드하고 테스트하는 프로세스
- **CD (Continuous Deployment)**: 검증된 코드를 자동으로 배포하는 프로세스
- **OIDC (OpenID Connect)**: GitHub과 AWS 간 안전한 인증을 위한 표준 프로토콜
- **Blue/Green Deployment**: 구버전(Blue)과 신버전(Green)을 동시에 운영하며 트래픽을 전환하는 배포 방식
- **Canary Deployment**: 신버전에 일부 트래픽만 먼저 보내 검증하는 배포 전략
- **Rolling Update**: 인스턴스를 하나씩 순차적으로 업데이트하는 배포 방식
- **Guardrail**: 배포 중 문제를 감지하고 자동으로 중단/롤백하는 안전장치
- **ECR (Elastic Container Registry)**: AWS의 Docker 이미지 저장소
- **ECS (Elastic Container Service)**: AWS의 컨테이너 오케스트레이션 서비스
- **CodeDeploy**: AWS의 자동 배포 서비스
- **Lambda Hook**: CodeDeploy 배포 중 특정 시점에 실행되는 Lambda 함수
- **CloudWatch Alarm**: AWS 리소스의 메트릭을 모니터링하고 임계치 초과 시 알림을 발생시키는 서비스

## Requirements

### Requirement 1: 인증 및 보안

**User Story:** As an cloud engineer, I want to use OIDC for AWS authentication, so that I don't need to manage long-term credentials.

#### Acceptance Criteria

1. WHEN GitHub Actions workflow runs, THE System SHALL authenticate to AWS using OIDC
2. THE System SHALL NOT use AWS Access Keys or Secret Keys
3. WHEN authentication fails, THE System SHALL stop the workflow immediately
4. THE System SHALL use separate IAM roles for Dev and Prod environments

---

### Requirement 2: Dev 환경 CI (Continuous Integration)

**User Story:** As a developer, I want automated testing and building when I push to develop branch, so that I can quickly validate my changes.

#### Acceptance Criteria

1. WHEN code is pushed to develop branch, THE System SHALL trigger CI workflow automatically
2. THE System SHALL run Gradle tests (unit and integration tests)
3. WHEN tests fail, THE System SHALL stop the workflow and notify the team
4. WHEN tests pass, THE System SHALL build Docker image
5. THE System SHALL tag the image with Git commit SHA
6. THE System SHALL push the image to ECR repository
7. THE System SHALL complete CI process within 10 minutes

---

### Requirement 3: Dev 환경 CD (Continuous Deployment)

**User Story:** As a developer, I want automatic deployment to Dev environment, so that I can test my changes quickly.

#### Acceptance Criteria

1. WHEN Docker image is pushed to ECR, THE System SHALL trigger CD workflow automatically
2. THE System SHALL use Rolling Update deployment strategy
3. THE System SHALL update ECS task definition with new image tag
4. THE System SHALL force new deployment on ECS service
5. WHEN deployment starts, THE System SHALL wait for health checks to pass
6. WHEN health checks fail, THE System SHALL automatically rollback to previous version
7. THE System SHALL complete deployment within 5 minutes
8. THE System SHALL send notification to Discord on success or failure

---

### Requirement 4: Prod 환경 CI (Continuous Integration)

**User Story:** As a cloud engineer, I want thorough testing before production deployment, so that I can ensure code quality.

#### Acceptance Criteria

1. WHEN code is pushed to main branch, THE System SHALL trigger CI workflow automatically
2. THE System SHALL require manual approval before starting CI
3. THE System SHALL run Gradle tests (unit and integration tests)
4. WHEN tests fail, THE System SHALL stop the workflow and notify the team
5. WHEN tests pass, THE System SHALL build Docker image
6. THE System SHALL tag the image with Git commit SHA
7. THE System SHALL push the image to ECR repository
8. THE System SHALL complete CI process within 10 minutes

---

### Requirement 5: Prod 환경 CD - Blue/Green Deployment

**User Story:** As a cloud engineer, I want zero-downtime deployment with automatic rollback, so that I can maintain service availability.

#### Acceptance Criteria

1. WHEN Docker image is pushed to ECR, THE System SHALL trigger CD workflow
2. THE System SHALL require manual approval before deployment
3. THE System SHALL use CodeDeploy for Blue/Green deployment
4. THE System SHALL create new task definition with new image tag
5. THE System SHALL provision Green environment with new tasks
6. THE System SHALL keep Blue environment running during deployment
7. WHEN Green environment is ready, THE System SHALL proceed to traffic shifting

---

### Requirement 6: Prod 환경 Guardrail - Health Check

**User Story:** As a cloud engineer, I want automatic health verification, so that unhealthy deployments are blocked.

#### Acceptance Criteria

1. WHEN Green environment is provisioned, THE System SHALL perform ALB health checks
2. THE System SHALL check /actuator/health endpoint
3. WHEN health check fails, THE System SHALL stop deployment immediately
4. WHEN health check fails, THE System SHALL terminate Green environment
5. WHEN health check passes, THE System SHALL proceed to Lambda Hook verification

---

### Requirement 7: Prod 환경 Guardrail - Lambda Hook

**User Story:** As a cloud engineer, I want functional testing before traffic shift, so that I can verify business logic works correctly.

#### Acceptance Criteria

1. WHEN health checks pass, THE System SHALL trigger Lambda Hook at AfterAllowTestTraffic lifecycle event
2. THE Lambda SHALL test critical API endpoints
3. THE Lambda SHALL verify database connectivity
4. THE Lambda SHALL verify Redis connectivity
5. WHEN Lambda tests fail, THE Lambda SHALL return FAILED status to CodeDeploy
6. WHEN Lambda returns FAILED, THE System SHALL stop deployment and terminate Green environment
7. WHEN Lambda tests pass, THE System SHALL proceed to Canary deployment
8. THE System SHALL send Discord notification on Lambda test failure

---

### Requirement 8: Prod 환경 Guardrail - Canary Deployment

**User Story:** As a cloud engineer, I want gradual traffic shifting with monitoring, so that I can detect issues with minimal user impact.

#### Acceptance Criteria

1. WHEN Lambda Hook passes, THE System SHALL start Canary deployment
2. THE System SHALL shift 10% of traffic to Green environment
3. THE System SHALL monitor CloudWatch metrics for 5 minutes
4. THE System SHALL monitor 5XX error rate
5. THE System SHALL monitor response latency
6. WHEN error rate exceeds 5%, THE System SHALL trigger CloudWatch Alarm
7. WHEN latency exceeds 2 seconds, THE System SHALL trigger CloudWatch Alarm
8. WHEN CloudWatch Alarm triggers, THE System SHALL automatically rollback to Blue environment
9. WHEN rollback occurs, THE System SHALL terminate Green environment
10. WHEN 5 minutes pass without alarms, THE System SHALL shift remaining 90% traffic to Green
11. THE System SHALL send Discord notification on alarm trigger and rollback

---

### Requirement 9: Prod 환경 Stabilization

**User Story:** As a cloud engineer, I want to maintain old version temporarily after deployment, so that I can quickly rollback if issues are discovered.

#### Acceptance Criteria

1. WHEN 100% traffic is shifted to Green, THE System SHALL keep Blue environment running
2. THE System SHALL maintain Blue environment for 30 minutes
3. WHEN 30 minutes pass without issues, THE System SHALL terminate Blue environment
4. THE System SHALL send Discord notification on successful deployment completion

---

### Requirement 10: 알림 시스템

**User Story:** As a team member, I want to receive deployment notifications, so that I can stay informed about deployment status.

#### Acceptance Criteria

1. WHEN CI starts, THE System SHALL send Discord notification
2. WHEN CI fails, THE System SHALL send Discord notification with error details
3. WHEN CI succeeds, THE System SHALL send Discord notification
4. WHEN CD starts, THE System SHALL send Discord notification
5. WHEN deployment fails, THE System SHALL send Discord notification with failure reason
6. WHEN deployment succeeds, THE System SHALL send Discord notification
7. WHEN rollback occurs, THE System SHALL send urgent Discord notification
8. THE System SHALL include deployment environment (Dev/Prod) in notifications
9. THE System SHALL include Git commit SHA in notifications
10. THE System SHALL include deployment duration in notifications

---

### Requirement 11: 추적성 (Traceability)

**User Story:** As a cloud engineer, I want to trace which code version is running in production, so that I can quickly identify issues.

#### Acceptance Criteria

1. THE System SHALL use Git commit SHA as Docker image tag
2. THE System SHALL include Git commit SHA in ECS task definition
3. THE System SHALL include Git commit SHA in Discord notifications
4. WHEN viewing ECS service, THE System SHALL display which Git commit is running
5. THE System SHALL maintain deployment history in CodeDeploy

---

### Requirement 12: 환경별 차이

**User Story:** As a cloud engineer, I want different deployment strategies for different environments, so that I can balance speed and safety.

#### Acceptance Criteria

1. THE Dev_Environment SHALL use Rolling Update deployment
2. THE Dev_Environment SHALL NOT require manual approval
3. THE Dev_Environment SHALL NOT use CodeDeploy
4. THE Dev_Environment SHALL NOT use Lambda Hook
5. THE Dev_Environment SHALL NOT use Canary deployment
6. THE Prod_Environment SHALL use Blue/Green deployment
7. THE Prod_Environment SHALL require manual approval for CI and CD
8. THE Prod_Environment SHALL use CodeDeploy
9. THE Prod_Environment SHALL use Lambda Hook
10. THE Prod_Environment SHALL use Canary deployment with CloudWatch monitoring

---

### Requirement 13: 비용 관리

**User Story:** As a cloud engineer, I want to control deployment costs, so that I don't exceed budget.

#### Acceptance Criteria

1. THE System SHALL reuse existing infrastructure (no new resources during deployment)
2. THE System SHALL terminate old tasks after successful deployment
3. THE Dev_Environment SHALL use minimal resources (1 task)
4. THE Prod_Environment SHALL maintain high availability (4 tasks)
5. THE System SHALL clean up failed deployments automatically

---

### Requirement 14: 롤백 전략

**User Story:** As a cloud engineer, I want automatic and manual rollback options, so that I can quickly recover from issues.

#### Acceptance Criteria

1. WHEN health check fails, THE System SHALL automatically rollback
2. WHEN Lambda Hook fails, THE System SHALL automatically rollback
3. WHEN CloudWatch Alarm triggers, THE System SHALL automatically rollback
4. THE System SHALL support manual rollback through CodeDeploy console
5. WHEN rollback occurs, THE System SHALL restore previous task definition
6. WHEN rollback occurs, THE System SHALL shift 100% traffic back to Blue
7. THE System SHALL complete rollback within 2 minutes

---

### Requirement 15: 모니터링 및 로깅

**User Story:** As a cloud engineer, I want comprehensive logging, so that I can troubleshoot deployment issues.

#### Acceptance Criteria

1. THE System SHALL log all deployment steps to CloudWatch Logs
2. THE System SHALL log Lambda Hook test results
3. THE System SHALL log CodeDeploy lifecycle events
4. THE System SHALL retain logs for 30 days in Prod environment
5. THE System SHALL retain logs for 7 days in Dev environment
6. WHEN deployment fails, THE System SHALL include relevant logs in Discord notification
