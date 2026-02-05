import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let successRate = new Rate('success');
export let paymentResponseTime = new Trend('payment_response_time');
export let paymentApiKeyFailure = new Rate('payment_api_key_failure');
export let rollingUpdateImpact = new Rate('rolling_update_impact');

// Payment Service API 키 변경 테스트
export let options = {
  stages: [
    { duration: '30s', target: 10 },  // 워밍업
    { duration: '2m', target: 15 },   // 안정적인 부하 (Rolling Update 실행 구간)
    { duration: '30s', target: 5 },   // 복구 확인
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],     // P95 3초 이하
    errors: ['rate<0.1'],                  // 에러율 10% 이하 (결제 시스템은 더 엄격)
    success: ['rate>0.9'],                 // 성공률 90% 이상
    payment_api_key_failure: ['rate<0.05'], // API 키 실패 5% 이하
    rolling_update_impact: ['rate<0.15'],  // Rolling Update 영향 15% 이하
  },
};

const BASE_URL = 'http://localhost:8085/payment';

export function setup() {
  console.log('💳 Payment Service API 키 변경 Rolling Update 테스트 시작');
  console.log('🎯 시나리오: Toss Payment API 키 변경으로 인한 결제 실패');
  console.log('📊 그라파나 모니터링: http://localhost:30300');
  console.log('');
  console.log('🔑 API 키 변경 내용:');
  console.log('   OLD: test_sk_dummy_key_for_local');
  console.log('   NEW: test_sk_NEW_UPDATED_ROLLING_UPDATE_KEY_V2_2024');
  console.log('');
  console.log('⚠️  30초 후 Rolling Update를 실행하세요!');
  console.log('🔄 Rolling Update 명령어:');
  console.log('   helm upgrade unbox-payment helm/unbox-service -f helm/values-payment-rolling-update.yaml');
  console.log('');
  console.log('📈 예상 결과:');
  console.log('   - 기존 결제 요청: 기존 API 키로 처리 성공');
  console.log('   - 새 결제 요청: 새 API 키로 처리 (호환성 문제 가능)');
  console.log('   - API 키 불일치: 401/403 에러 발생');
  console.log('   - 결제 실패율 증가: 비즈니스 직접 타격');
}

export default function () {
  let scenario = Math.random();
  
  if (scenario < 0.6) {
    // 60% - 헬스체크 (기본 상태 확인)
    let startTime = Date.now();
    let response = http.get(`${BASE_URL}/actuator/health`, {
      tags: { name: 'health_check', test_type: 'payment_api_key_change' },
      timeout: '5s',
    });
    let endTime = Date.now();
    let duration = endTime - startTime;
    
    paymentResponseTime.add(duration);
    
    let success = check(response, {
      'health status is 200': (r) => r.status === 200,
      'health response time < 1000ms': (r) => duration < 1000,
      'health response has status UP': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body && body.status === 'UP';
        } catch (e) {
          return false;
        }
      }
    });
    
    if (success) {
      successRate.add(1);
      console.log(`🟢 헬스체크 성공: ${duration}ms`);
    } else {
      errorRate.add(1);
      
      if (response.status === 0 || response.status >= 500) {
        rollingUpdateImpact.add(1);
        console.log(`🔴 Rolling Update 영향 - 헬스체크 실패: ${response.status} (${duration}ms)`);
      } else {
        console.log(`🟡 일반 에러 - 헬스체크: ${response.status} (${duration}ms)`);
      }
    }
    
  } else if (scenario < 0.8) {
    // 20% - 메트릭 수집 (시스템 상태 모니터링)
    let startTime = Date.now();
    let response = http.get(`${BASE_URL}/actuator/prometheus`, {
      tags: { name: 'metrics_check', test_type: 'payment_api_key_change' },
      timeout: '5s',
    });
    let endTime = Date.now();
    let duration = endTime - startTime;
    
    paymentResponseTime.add(duration);
    
    let success = check(response, {
      'metrics status is 200': (r) => r.status === 200,
      'metrics response time < 2000ms': (r) => duration < 2000,
      'metrics has content': (r) => r.body && r.body.length > 0
    });
    
    if (success) {
      successRate.add(1);
      console.log(`🟢 메트릭 수집 성공: ${duration}ms`);
    } else {
      errorRate.add(1);
      
      if (response.status === 0 || response.status >= 500) {
        rollingUpdateImpact.add(1);
        console.log(`🔴 Rolling Update 영향 - 메트릭 수집 실패: ${response.status} (${duration}ms)`);
      } else {
        console.log(`🟡 일반 에러 - 메트릭 수집: ${response.status} (${duration}ms)`);
      }
    }
    
  } else {
    // 20% - 정보 조회 (서비스 정보 확인)
    let startTime = Date.now();
    let response = http.get(`${BASE_URL}/actuator/info`, {
      tags: { name: 'info_check', test_type: 'payment_api_key_change' },
      timeout: '5s',
    });
    let endTime = Date.now();
    let duration = endTime - startTime;
    
    paymentResponseTime.add(duration);
    
    let success = check(response, {
      'info status is 200': (r) => r.status === 200,
      'info response time < 1000ms': (r) => duration < 1000,
    });
    
    if (success) {
      successRate.add(1);
      console.log(`🟢 정보 조회 성공: ${duration}ms`);
    } else {
      errorRate.add(1);
      
      // API 키 관련 에러 체크
      if (response.status === 401 || response.status === 403) {
        paymentApiKeyFailure.add(1);
        console.log(`🔑 API 키 인증 실패: ${response.status} (${duration}ms)`);
      } else if (response.status === 0 || response.status >= 500) {
        rollingUpdateImpact.add(1);
        console.log(`🔴 Rolling Update 영향 - 정보 조회 실패: ${response.status} (${duration}ms)`);
      } else {
        console.log(`🟡 일반 에러 - 정보 조회: ${response.status} (${duration}ms)`);
      }
    }
  }

  sleep(Math.random() * 2 + 1); // 1-3초 랜덤 대기
}

export function teardown() {
  console.log('✅ Payment Service API 키 변경 Rolling Update 테스트 완료');
  console.log('');
  console.log('📊 결과 분석:');
  console.log('🔍 확인할 메트릭:');
  console.log('   - errors: 전체 에러율 (목표: < 10%)');
  console.log('   - success: 전체 성공률 (목표: > 90%)');
  console.log('   - payment_api_key_failure: API 키 인증 실패 (목표: < 5%)');
  console.log('   - rolling_update_impact: Rolling Update 직접 영향 (목표: < 15%)');
  console.log('   - payment_response_time: Payment Service 응답시간 (목표: P95 < 3초)');
  console.log('');
  console.log('🎯 비즈니스 영향 분석:');
  console.log('   ❌ API 키 실패 > 5% → 결제 실패 증가 → 매출 손실');
  console.log('   ❌ 에러율 > 10% → 고객 불만 → 브랜드 신뢰도 하락');
  console.log('   ❌ Rolling Update 영향 > 15% → 배포 전략 재검토 필요');
  console.log('');
  console.log('🏆 결론: Payment Service는 Blue-Green 배포 전략 필수!');
  console.log('💡 이유: API 키 변경 시 무중단 배포로 결제 연속성 보장');
}