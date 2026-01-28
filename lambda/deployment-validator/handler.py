"""
CodeDeploy Deployment Validator Lambda Function

이 Lambda 함수는 CodeDeploy Blue/Green 배포 중 AfterAllowTestTraffic 단계에서 실행되어
Green 환경의 헬스를 검증합니다.

검증 항목:
1. Health Check 엔드포인트 (/actuator/health)
2. 응답 시간 (2초 이내)
3. HTTP 상태 코드 (200 OK)
"""

import json
import boto3
import urllib3
import time
from datetime import datetime

# AWS 클라이언트 초기화
codedeploy = boto3.client('codedeploy')
elbv2 = boto3.client('elbv2')
http = urllib3.PoolManager()

def lambda_handler(event, context):
    """
    Lambda 핸들러 함수
    
    Args:
        event: CodeDeploy 이벤트 (DeploymentId, LifecycleEventHookExecutionId 포함)
        context: Lambda 컨텍스트
    
    Returns:
        dict: 실행 결과
    """
    print(f"Received event: {json.dumps(event)}")
    
    # CodeDeploy 정보 추출
    deployment_id = event['DeploymentId']
    lifecycle_event_hook_execution_id = event['LifecycleEventHookExecutionId']
    
    try:
        # Green 환경 엔드포인트 가져오기
        green_endpoint = get_green_endpoint(deployment_id)
        print(f"Green endpoint: {green_endpoint}")
        
        # 검증 테스트 실행
        validation_results = run_validation_tests(green_endpoint)
        
        # 결과 판단
        all_passed = all(validation_results.values())
        status = 'Succeeded' if all_passed else 'Failed'
        
        # 결과 로깅
        print(f"Validation results: {json.dumps(validation_results)}")
        print(f"Overall status: {status}")
        
        # CodeDeploy에 결과 보고
        report_to_codedeploy(
            deployment_id,
            lifecycle_event_hook_execution_id,
            status
        )
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'status': status,
                'results': validation_results,
                'endpoint': green_endpoint
            })
        }
        
    except Exception as e:
        print(f"Error during validation: {str(e)}")
        
        # 에러 발생 시 Failed 보고
        report_to_codedeploy(
            deployment_id,
            lifecycle_event_hook_execution_id,
            'Failed'
        )
        
        return {
            'statusCode': 500,
            'body': json.dumps({
                'status': 'Failed',
                'error': str(e)
            })
        }


def get_green_endpoint(deployment_id):
    """
    CodeDeploy 배포 정보에서 Green 환경의 Target Group을 찾아 엔드포인트 반환
    
    Args:
        deployment_id: CodeDeploy 배포 ID
    
    Returns:
        str: Green 환경 ALB 엔드포인트 URL
    """
    # 배포 정보 가져오기
    deployment = codedeploy.get_deployment(deploymentId=deployment_id)
    
    # Target Group ARN 추출
    target_group_info = deployment['deploymentInfo']['targetInstances']['tagFilters']
    
    # 실제 구현에서는 ALB DNS를 반환
    # 여기서는 환경 변수나 배포 정보에서 ALB DNS를 가져와야 함
    alb_dns = deployment['deploymentInfo'].get('loadBalancerInfo', {}).get('targetGroupInfoList', [{}])[0].get('name', '')
    
    # ALB DNS가 없으면 환경 변수에서 가져오기
    import os
    alb_dns = os.environ.get('ALB_DNS', alb_dns)
    
    return f"http://{alb_dns}"


def run_validation_tests(endpoint):
    """
    Green 환경에 대한 검증 테스트 실행
    
    Args:
        endpoint: Green 환경 엔드포인트 URL
    
    Returns:
        dict: 테스트 결과 (test_name: bool)
    """
    results = {}
    
    # Test 1: Health Check
    results['health_check'] = test_health_endpoint(endpoint)
    
    # Test 2: Response Time
    results['response_time'] = test_response_time(endpoint)
    
    # Test 3: API Availability (선택사항)
    # results['api_availability'] = test_api_endpoints(endpoint)
    
    return results


def test_health_endpoint(endpoint):
    """
    헬스체크 엔드포인트 테스트
    
    Args:
        endpoint: 엔드포인트 URL
    
    Returns:
        bool: 테스트 성공 여부
    """
    try:
        health_url = f"{endpoint}/actuator/health"
        print(f"Testing health endpoint: {health_url}")
        
        response = http.request(
            'GET',
            health_url,
            timeout=5.0,
            retries=urllib3.Retry(total=3, backoff_factor=0.3)
        )
        
        print(f"Health check status: {response.status}")
        print(f"Health check response: {response.data.decode('utf-8')[:200]}")
        
        # 200 OK 확인
        if response.status != 200:
            return False
        
        # 응답 본문 확인 (선택사항)
        try:
            data = json.loads(response.data.decode('utf-8'))
            status = data.get('status', '').upper()
            return status == 'UP'
        except:
            # JSON 파싱 실패해도 200이면 통과
            return True
            
    except Exception as e:
        print(f"Health check failed: {str(e)}")
        return False


def test_response_time(endpoint):
    """
    응답 시간 테스트 (2초 이내)
    
    Args:
        endpoint: 엔드포인트 URL
    
    Returns:
        bool: 테스트 성공 여부
    """
    try:
        health_url = f"{endpoint}/actuator/health"
        print(f"Testing response time: {health_url}")
        
        start_time = time.time()
        response = http.request('GET', health_url, timeout=5.0)
        elapsed_time = time.time() - start_time
        
        print(f"Response time: {elapsed_time:.2f} seconds")
        
        # 2초 이내 응답 확인
        return elapsed_time < 2.0
        
    except Exception as e:
        print(f"Response time test failed: {str(e)}")
        return False


def report_to_codedeploy(deployment_id, lifecycle_event_hook_execution_id, status):
    """
    CodeDeploy에 검증 결과 보고
    
    Args:
        deployment_id: 배포 ID
        lifecycle_event_hook_execution_id: Lifecycle 이벤트 실행 ID
        status: 'Succeeded' 또는 'Failed'
    """
    try:
        codedeploy.put_lifecycle_event_hook_execution_status(
            deploymentId=deployment_id,
            lifecycleEventHookExecutionId=lifecycle_event_hook_execution_id,
            status=status
        )
        print(f"Reported status '{status}' to CodeDeploy")
    except Exception as e:
        print(f"Failed to report to CodeDeploy: {str(e)}")
        raise
