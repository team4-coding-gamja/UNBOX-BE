# Tasks: CI/CD Pipeline Implementation

## Overview

Implementation tasks for UNBOX CI/CD pipeline. Tasks are organized by phase and can be executed in parallel where dependencies allow.

## Task Organization

- **Phase 1**: AWS Infrastructure Setup (Prerequisites)
- **Phase 2**: Dev Environment CI/CD
- **Phase 3**: Prod Environment CI/CD
- **Phase 4**: Testing and Validation
- **Phase 5**: Documentation and Rollout

---

## Phase 1: AWS Infrastructure Setup

### Task 1.1: Create OIDC Provider

**Priority**: P0 (Blocker)  
**Estimated Time**: 30 minutes  
**Dependencies**: None

**Steps**:
1. Create OIDC provider in AWS IAM
   - Provider URL: `token.actions.githubusercontent.com`
   - Audience: `sts.amazonaws.com`
2. Note the provider ARN for next tasks

**Acceptance Criteria**:
- OIDC provider visible in AWS IAM console
- Provider ARN documented

**Files Created**: None (AWS Console)

---

### Task 1.2: Create IAM Role for Dev

**Priority**: P0 (Blocker)  
**Estimated Time**: 30 minutes  
**Dependencies**: Task 1.1

**Steps**:
1. Create IAM role: `github-actions-dev-role`
2. Configure trust policy with OIDC provider
3. Attach permissions:
   - ECR: Push images
   - ECS: Update services, register task definitions
   - CloudWatch Logs: Create log groups, put logs
4. Add condition for `develop` branch only

**Acceptance Criteria**:
- Role can be assumed by GitHub Actions from develop branch
- Role has minimum required permissions

**Files Created**: 
- `terraform/modules/iam/github-actions-dev-role.tf` (optional, if using Terraform)

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
          "token.actions.githubusercontent.com:sub": "repo:YOUR_ORG/YOUR_REPO:ref:refs/heads/develop"
        }
      }
    }
  ]
}
```

---

### Task 1.3: Create IAM Role for Prod

**Priority**: P0 (Blocker)  
**Estimated Time**: 30 minutes  
**Dependencies**: Task 1.1

**Steps**:
1. Create IAM role: `github-actions-prod-role`
2. Configure trust policy with OIDC provider
3. Attach permissions:
   - ECR: Push images
   - ECS: Update services, register task definitions
   - CodeDeploy: Create deployments, get deployment status
   - CloudWatch Logs: Create log groups, put logs
4. Add condition for `main` branch only

**Acceptance Criteria**:
- Role can be assumed by GitHub Actions from main branch
- Role has minimum required permissions including CodeDeploy

**Files Created**: 
- `terraform/modules/iam/github-actions-prod-role.tf` (optional)

---

### Task 1.4: Create ECR Repositories

**Priority**: P0 (Blocker)  
**Estimated Time**: 20 minutes  
**Dependencies**: None

**Steps**:
1. Create ECR repositories for each service:
   - `unbox-user-service`
   - `unbox-product-service`
   - `unbox-order-service`
   - `unbox-payment-service`
   - `unbox-trade-service`
2. Configure lifecycle policy (keep last 10 images)
3. Enable image scanning

**Acceptance Criteria**:
- All 5 repositories created
- Lifecycle policy applied
- Image scanning enabled

**Files Created**: 
- `terraform/modules/ecr/repositories.tf` (if using Terraform)

---

### Task 1.5: Create Lambda Hook Function (Prod)

**Priority**: P1 (High)  
**Estimated Time**: 2 hours  
**Dependencies**: None

**Steps**:
1. Create Lambda function: `unbox-prod-deployment-validator`
2. Implement validation logic:
   - Health check test
   - Critical API tests
   - Database connectivity test
   - Redis connectivity test
3. Configure IAM role with permissions:
   - CodeDeploy: PutLifecycleEventHookExecutionStatus
   - CloudWatch Logs: CreateLogGroup, PutLogEvents
4. Set timeout to 5 minutes
5. Configure environment variables for test endpoints

**Acceptance Criteria**:
- Lambda function deploys successfully
- Function can report status to CodeDeploy
- All test functions work correctly

**Files Created**:
- `lambda/deployment-validator/handler.py`
- `lambda/deployment-validator/requirements.txt`
- `lambda/deployment-validator/README.md`

**Sample Code**:
```python
import boto3
import requests
import os

codedeploy = boto3.client('codedeploy')

def lambda_handler(event, context):
    deployment_id = event['DeploymentId']
    lifecycle_event_hook_execution_id = event['LifecycleEventHookExecutionId']
    
    # Extract Green environment endpoint
    green_endpoint = extract_green_endpoint(event)
    
    # Run validation tests
    tests_passed = run_validation_tests(green_endpoint)
    
    # Report status to CodeDeploy
    status = 'Succeeded' if tests_passed else 'Failed'
    
    codedeploy.put_lifecycle_event_hook_execution_status(
        deploymentId=deployment_id,
        lifecycleEventHookExecutionId=lifecycle_event_hook_execution_id,
        status=status
    )
    
    return {'statusCode': 200, 'body': status}

def run_validation_tests(endpoint):
    try:
        # Test 1: Health check
        health_response = requests.get(f"{endpoint}/actuator/health", timeout=10)
        if health_response.status_code != 200:
            return False
        
        # Test 2: Critical API endpoint
        api_response = requests.get(f"{endpoint}/api/v1/health", timeout=10)
        if api_response.status_code != 200:
            return False
        
        # Add more tests as needed
        
        return True
    except Exception as e:
        print(f"Validation failed: {str(e)}")
        return False
```

---

### Task 1.6: Create CloudWatch Alarms (Prod)

**Priority**: P1 (High)  
**Estimated Time**: 1 hour  
**Dependencies**: None

**Steps**:
1. Create alarm for 5XX errors:
   - Metric: `HTTPCode_Target_5XX_Count`
   - Threshold: 10 errors in 2 minutes
   - Action: Trigger CodeDeploy rollback
2. Create alarm for high latency:
   - Metric: `TargetResponseTime`
   - Threshold: 2 seconds average over 2 minutes
   - Action: Trigger CodeDeploy rollback
3. Link alarms to CodeDeploy deployment group

**Acceptance Criteria**:
- Both alarms created and active
- Alarms linked to CodeDeploy for automatic rollback
- Test alarms trigger correctly

**Files Created**:
- `terraform/environments/production/cloudwatch-alarms.tf` (if using Terraform)

---

### Task 1.7: Configure CodeDeploy (Prod)

**Priority**: P1 (High)  
**Estimated Time**: 1 hour  
**Dependencies**: Task 1.5, Task 1.6

**Steps**:
1. Create CodeDeploy application: `unbox-prod-app`
2. Create deployment group for each service
3. Configure Blue/Green deployment settings:
   - Deployment config: `CodeDeployDefault.ECSCanary10Percent5Minutes`
   - Traffic routing: TimeBasedCanary
   - Termination wait time: 30 minutes
4. Add Lambda Hook at `AfterAllowTestTraffic`
5. Enable automatic rollback on:
   - Deployment failure
   - CloudWatch alarm
6. Link CloudWatch alarms

**Acceptance Criteria**:
- CodeDeploy application and deployment groups created
- Blue/Green settings configured correctly
- Lambda Hook linked
- Automatic rollback enabled

**Files Created**:
- `terraform/environments/production/codedeploy.tf` (if using Terraform)

---

### Task 1.8: Configure GitHub Secrets

**Priority**: P0 (Blocker)  
**Estimated Time**: 15 minutes  
**Dependencies**: Task 1.2, Task 1.3

**Steps**:
1. Add secrets to GitHub repository:
   - `AWS_ACCOUNT_ID`: Your AWS account ID
   - `AWS_REGION`: `ap-northeast-2`
   - `DEV_IAM_ROLE_ARN`: ARN from Task 1.2
   - `PROD_IAM_ROLE_ARN`: ARN from Task 1.3
   - `DISCORD_WEBHOOK_URL`: Discord webhook URL

**Acceptance Criteria**:
- All secrets added to GitHub
- Secrets accessible in workflow runs

**Files Created**: None (GitHub UI)

---

## Phase 2: Dev Environment CI/CD

### Task 2.1: Create Dev CI Workflow

**Priority**: P0 (Blocker)  
**Estimated Time**: 2 hours  
**Dependencies**: Task 1.8

**Steps**:
1. Create workflow file: `.github/workflows/dev-ci.yml`
2. Configure trigger: Push to `develop` branch
3. Implement jobs:
   - Authenticate with AWS using OIDC
   - Run Gradle tests
   - Build Docker image
   - Tag with Git SHA
   - Push to ECR
4. Add Discord notifications
5. Output image URI for CD workflow

**Acceptance Criteria**:
- Workflow triggers on push to develop
- Tests run successfully
- Image builds and pushes to ECR
- Discord notifications sent

**Files Created**:
- `.github/workflows/dev-ci.yml`

---

### Task 2.2: Create Dev CD Workflow

**Priority**: P0 (Blocker)  
**Estimated Time**: 2 hours  
**Dependencies**: Task 2.1

**Steps**:
1. Create workflow file: `.github/workflows/dev-cd.yml`
2. Configure trigger: Completion of dev-ci workflow
3. Implement jobs:
   - Authenticate with AWS using OIDC
   - Render task definition with new image
   - Register task definition
   - Update ECS service (force new deployment)
   - Wait for service stability
4. Add Discord notifications

**Acceptance Criteria**:
- Workflow triggers after CI completes
- Task definition updates with new image
- ECS service deploys successfully
- Health checks pass
- Discord notifications sent

**Files Created**:
- `.github/workflows/dev-cd.yml`
- `task-definitions/dev-user-service.json` (template)

---

### Task 2.3: Create Task Definition Templates (Dev)

**Priority**: P0 (Blocker)  
**Estimated Time**: 1 hour  
**Dependencies**: None

**Steps**:
1. Create task definition templates for each service:
   - `task-definitions/dev-user-service.json`
   - `task-definitions/dev-product-service.json`
   - `task-definitions/dev-order-service.json`
   - `task-definitions/dev-payment-service.json`
   - `task-definitions/dev-trade-service.json`
2. Use placeholder for image tag: `<IMAGE_TAG>`
3. Configure health checks
4. Configure logging

**Acceptance Criteria**:
- All 5 templates created
- Templates have correct structure
- Placeholders ready for replacement

**Files Created**:
- `task-definitions/dev-*.json` (5 files)

---

### Task 2.4: Test Dev CI/CD Pipeline

**Priority**: P0 (Blocker)  
**Estimated Time**: 1 hour  
**Dependencies**: Task 2.1, Task 2.2, Task 2.3

**Steps**:
1. Create test branch from develop
2. Make small code change
3. Push to develop branch
4. Monitor CI workflow execution
5. Monitor CD workflow execution
6. Verify deployment success
7. Check Discord notifications
8. Verify service health

**Acceptance Criteria**:
- CI workflow completes successfully
- CD workflow completes successfully
- Service is healthy in Dev environment
- All Discord notifications received

**Files Created**: None (testing)

---

## Phase 3: Prod Environment CI/CD

### Task 3.1: Create Prod CI Workflow

**Priority**: P1 (High)  
**Estimated Time**: 2 hours  
**Dependencies**: Task 1.8

**Steps**:
1. Create workflow file: `.github/workflows/prod-ci.yml`
2. Configure trigger: Push to `main` branch
3. Add manual approval gate before starting
4. Implement jobs (same as Dev CI):
   - Authenticate with AWS using OIDC
   - Run Gradle tests
   - Build Docker image
   - Tag with Git SHA
   - Push to ECR
5. Add Discord notifications
6. Output image URI for CD workflow

**Acceptance Criteria**:
- Workflow requires manual approval
- Tests run successfully
- Image builds and pushes to ECR
- Discord notifications sent

**Files Created**:
- `.github/workflows/prod-ci.yml`

---

### Task 3.2: Create Prod CD Workflow

**Priority**: P1 (High)  
**Estimated Time**: 3 hours  
**Dependencies**: Task 3.1, Task 1.7

**Steps**:
1. Create workflow file: `.github/workflows/prod-cd.yml`
2. Configure trigger: Completion of prod-ci workflow
3. Add manual approval gate before deployment
4. Implement jobs:
   - Authenticate with AWS using OIDC
   - Render task definition with new image
   - Register task definition
   - Create AppSpec file
   - Trigger CodeDeploy deployment
   - Monitor deployment status
   - Wait for deployment completion
5. Add Discord notifications for all stages

**Acceptance Criteria**:
- Workflow requires manual approval
- CodeDeploy deployment triggers
- Blue/Green deployment executes
- All Guardrails work correctly
- Discord notifications sent at each stage

**Files Created**:
- `.github/workflows/prod-cd.yml`
- `appspecs/prod-user-service.yaml` (template)

---

### Task 3.3: Create Task Definition Templates (Prod)

**Priority**: P1 (High)  
**Estimated Time**: 1 hour  
**Dependencies**: None

**Steps**:
1. Create task definition templates for each service:
   - `task-definitions/prod-user-service.json`
   - `task-definitions/prod-product-service.json`
   - `task-definitions/prod-order-service.json`
   - `task-definitions/prod-payment-service.json`
   - `task-definitions/prod-trade-service.json`
2. Use placeholder for image tag: `<IMAGE_TAG>`
3. Configure health checks
4. Configure logging
5. Increase resources compared to Dev

**Acceptance Criteria**:
- All 5 templates created
- Templates have correct structure
- Resources appropriate for Prod

**Files Created**:
- `task-definitions/prod-*.json` (5 files)

---

### Task 3.4: Create AppSpec Templates (Prod)

**Priority**: P1 (High)  
**Estimated Time**: 1 hour  
**Dependencies**: Task 1.5

**Steps**:
1. Create AppSpec templates for each service:
   - `appspecs/prod-user-service.yaml`
   - `appspecs/prod-product-service.yaml`
   - `appspecs/prod-order-service.yaml`
   - `appspecs/prod-payment-service.yaml`
   - `appspecs/prod-trade-service.yaml`
2. Configure Lambda Hook reference
3. Use placeholders for task definition ARN

**Acceptance Criteria**:
- All 5 AppSpec files created
- Lambda Hook correctly referenced
- Placeholders ready for replacement

**Files Created**:
- `appspecs/prod-*.yaml` (5 files)

---

### Task 3.5: Implement Discord Notification Helper

**Priority**: P1 (High)  
**Estimated Time**: 1 hour  
**Dependencies**: None

**Steps**:
1. Create reusable script for Discord notifications
2. Support different notification types:
   - CI Started
   - CI Failed
   - CI Succeeded
   - CD Started
   - Deployment Failed
   - Deployment Succeeded
   - Rollback Triggered
3. Include relevant information (environment, commit SHA, duration)
4. Use color coding for different types

**Acceptance Criteria**:
- Script works for all notification types
- Notifications display correctly in Discord
- Script is reusable across workflows

**Files Created**:
- `scripts/discord-notify.sh`

**Sample Script**:
```bash
#!/bin/bash

WEBHOOK_URL=$1
TITLE=$2
DESCRIPTION=$3
COLOR=$4

curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"embeds\": [{
      \"title\": \"$TITLE\",
      \"description\": \"$DESCRIPTION\",
      \"color\": $COLOR,
      \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
    }]
  }"
```

---

### Task 3.6: Test Prod CI/CD Pipeline

**Priority**: P1 (High)  
**Estimated Time**: 2 hours  
**Dependencies**: Task 3.1, Task 3.2, Task 3.3, Task 3.4

**Steps**:
1. Create test branch from main
2. Make small code change
3. Push to main branch
4. Approve CI workflow
5. Monitor CI execution
6. Approve CD workflow
7. Monitor CodeDeploy deployment
8. Verify all 3 Guardrails execute
9. Verify Canary deployment
10. Verify traffic shift to 100%
11. Verify Blue environment maintained for 30 min
12. Check all Discord notifications

**Acceptance Criteria**:
- CI workflow completes with approval
- CD workflow completes with approval
- All Guardrails pass
- Canary deployment succeeds
- Service is healthy in Prod environment
- All Discord notifications received

**Files Created**: None (testing)

---

## Phase 4: Testing and Validation

### Task 4.1: Test Failure Scenarios - Test Failure

**Priority**: P1 (High)  
**Estimated Time**: 30 minutes  
**Dependencies**: Task 2.4

**Steps**:
1. Introduce failing test in code
2. Push to develop branch
3. Verify CI workflow stops at test stage
4. Verify no image is built
5. Verify Discord alert sent

**Acceptance Criteria**:
- Workflow stops immediately on test failure
- No Docker image created
- Discord alert received with error details

**Files Created**: None (testing)

---

### Task 4.2: Test Failure Scenarios - Unhealthy Deployment (Dev)

**Priority**: P1 (High)  
**Estimated Time**: 30 minutes  
**Dependencies**: Task 2.4

**Steps**:
1. Deploy app with failing health check endpoint
2. Push to develop branch
3. Verify deployment starts
4. Verify health checks fail
5. Verify automatic rollback
6. Verify Discord alert sent

**Acceptance Criteria**:
- Deployment stops on health check failure
- ECS rolls back to previous version
- Discord alert received

**Files Created**: None (testing)

---

### Task 4.3: Test Failure Scenarios - Lambda Hook Failure (Prod)

**Priority**: P1 (High)  
**Estimated Time**: 1 hour  
**Dependencies**: Task 3.6

**Steps**:
1. Modify Lambda to return FAILED status
2. Push to main branch
3. Approve CI and CD
4. Verify deployment proceeds to Lambda Hook
5. Verify Lambda returns FAILED
6. Verify CodeDeploy stops deployment
7. Verify Green environment terminated
8. Verify Discord alert sent

**Acceptance Criteria**:
- Deployment stops after Lambda Hook failure
- Green environment cleaned up
- Blue environment still serving traffic
- Discord alert received

**Files Created**: None (testing)

---

### Task 4.4: Test Failure Scenarios - CloudWatch Alarm (Prod)

**Priority**: P1 (High)  
**Estimated Time**: 1 hour  
**Dependencies**: Task 3.6

**Steps**:
1. Deploy app that generates 5XX errors
2. Push to main branch
3. Approve CI and CD
4. Verify Canary deployment starts (10%)
5. Verify CloudWatch Alarm triggers
6. Verify automatic rollback to Blue
7. Verify Green environment terminated
8. Verify Discord urgent alert sent

**Acceptance Criteria**:
- CloudWatch Alarm triggers on high error rate
- CodeDeploy automatically rolls back
- Blue environment restored to 100% traffic
- Discord urgent alert received

**Files Created**: None (testing)

---

### Task 4.5: Test Manual Rollback (Prod)

**Priority**: P2 (Medium)  
**Estimated Time**: 30 minutes  
**Dependencies**: Task 3.6

**Steps**:
1. Deploy successful version to Prod
2. Use CodeDeploy console to manually rollback
3. Verify rollback completes
4. Verify previous version restored
5. Verify service health

**Acceptance Criteria**:
- Manual rollback works through console
- Previous version restored successfully
- Service remains healthy

**Files Created**: None (testing)

---

### Task 4.6: Performance Testing - CI Duration

**Priority**: P2 (Medium)  
**Estimated Time**: 30 minutes  
**Dependencies**: Task 2.4, Task 3.6

**Steps**:
1. Run 5 CI workflows for Dev
2. Run 5 CI workflows for Prod
3. Measure average duration
4. Verify Dev CI < 10 minutes
5. Verify Prod CI < 10 minutes

**Acceptance Criteria**:
- Dev CI completes within 10 minutes
- Prod CI completes within 10 minutes
- Performance meets requirements

**Files Created**: None (testing)

---

### Task 4.7: Performance Testing - CD Duration

**Priority**: P2 (Medium)  
**Estimated Time**: 30 minutes  
**Dependencies**: Task 2.4, Task 3.6

**Steps**:
1. Run 5 CD workflows for Dev
2. Run 5 CD workflows for Prod
3. Measure average duration
4. Verify Dev CD < 5 minutes
5. Verify Prod CD completes (including Canary wait)

**Acceptance Criteria**:
- Dev CD completes within 5 minutes
- Prod CD completes successfully with all stages
- Performance meets requirements

**Files Created**: None (testing)

---

## Phase 5: Documentation and Rollout

### Task 5.1: Create CI/CD User Guide

**Priority**: P2 (Medium)  
**Estimated Time**: 2 hours  
**Dependencies**: Task 4.7

**Steps**:
1. Create comprehensive user guide
2. Document workflow triggers
3. Document approval process
4. Document how to monitor deployments
5. Document how to rollback
6. Add troubleshooting section
7. Add FAQ section

**Acceptance Criteria**:
- Guide covers all common scenarios
- Clear instructions for developers
- Troubleshooting steps included

**Files Created**:
- `docs/CICD_USER_GUIDE.md`

---

### Task 5.2: Create Runbook for Operations

**Priority**: P2 (Medium)  
**Estimated Time**: 2 hours  
**Dependencies**: Task 4.7

**Steps**:
1. Create operations runbook
2. Document how to handle deployment failures
3. Document how to perform manual rollback
4. Document how to check Lambda Hook logs
5. Document how to check CloudWatch metrics
6. Add incident response procedures

**Acceptance Criteria**:
- Runbook covers all operational scenarios
- Clear procedures for incident response
- Links to relevant AWS consoles

**Files Created**:
- `docs/CICD_RUNBOOK.md`

---

### Task 5.3: Update Main README

**Priority**: P2 (Medium)  
**Estimated Time**: 30 minutes  
**Dependencies**: Task 5.1

**Steps**:
1. Add CI/CD section to main README
2. Link to user guide
3. Document branch strategy
4. Document deployment process overview
5. Add badges for workflow status

**Acceptance Criteria**:
- README updated with CI/CD information
- Links work correctly
- Badges display workflow status

**Files Created**: None (update existing)

---

### Task 5.4: Rollout to All Services

**Priority**: P1 (High)  
**Estimated Time**: 4 hours  
**Dependencies**: Task 4.7

**Steps**:
1. Week 1: Deploy Dev CI/CD for user-service only
2. Week 1: Monitor and fix any issues
3. Week 2: Deploy Dev CI/CD for all 5 services
4. Week 2: Gather feedback from developers
5. Week 3: Deploy Prod CI/CD for user-service only
6. Week 3: Test all Guardrails thoroughly
7. Week 4: Deploy Prod CI/CD for all 5 services
8. Week 4: Monitor closely for first week

**Acceptance Criteria**:
- All services have CI/CD configured
- No major issues during rollout
- Team is comfortable with new process

**Files Created**: None (rollout process)

---

### Task 5.5: Create Monitoring Dashboard

**Priority**: P2 (Medium)  
**Estimated Time**: 2 hours  
**Dependencies**: Task 5.4

**Steps**:
1. Create CloudWatch Dashboard: `unbox-cicd-metrics`
2. Add widgets for:
   - CI success rate
   - CD success rate
   - Deployment duration
   - Rollback frequency
   - Error rates during deployment
3. Configure time ranges
4. Share dashboard with team

**Acceptance Criteria**:
- Dashboard shows all key metrics
- Metrics update in real-time
- Team has access to dashboard

**Files Created**: None (AWS Console)

---

### Task 5.6: Setup Alerts for CI/CD Issues

**Priority**: P2 (Medium)  
**Estimated Time**: 1 hour  
**Dependencies**: Task 5.5

**Steps**:
1. Create CloudWatch Alarms for:
   - Multiple consecutive CI failures (>3)
   - Multiple consecutive CD failures (>3)
   - High rollback frequency (>2 per day)
2. Configure SNS topic for alerts
3. Link SNS to Discord webhook

**Acceptance Criteria**:
- Alarms trigger correctly
- Team receives alerts in Discord
- Alert fatigue minimized

**Files Created**: None (AWS Console)

---

## Summary

### Total Tasks: 36

**By Priority**:
- P0 (Blocker): 10 tasks
- P1 (High): 16 tasks
- P2 (Medium): 10 tasks

**By Phase**:
- Phase 1 (AWS Setup): 8 tasks
- Phase 2 (Dev CI/CD): 4 tasks
- Phase 3 (Prod CI/CD): 6 tasks
- Phase 4 (Testing): 7 tasks
- Phase 5 (Documentation): 6 tasks
- Phase 6 (Rollout): 5 tasks

**Estimated Total Time**: ~35 hours

**Critical Path**:
1. Task 1.1 → 1.2 → 1.8 → 2.1 → 2.2 → 2.3 → 2.4 (Dev CI/CD)
2. Task 1.1 → 1.3 → 1.5 → 1.6 → 1.7 → 3.1 → 3.2 → 3.6 (Prod CI/CD)

**Parallelization Opportunities**:
- Tasks 1.2 and 1.3 can run in parallel
- Tasks 1.4, 1.5, 1.6 can run in parallel
- Phase 2 and Phase 3 can partially overlap
- All testing tasks (Phase 4) can run in parallel after dependencies met

---

## Next Steps

1. Review this task list with the team
2. Assign tasks to team members
3. Set up project tracking (GitHub Projects, Jira, etc.)
4. Begin with Phase 1 tasks
5. Schedule regular check-ins to track progress
6. Update this document as tasks are completed
