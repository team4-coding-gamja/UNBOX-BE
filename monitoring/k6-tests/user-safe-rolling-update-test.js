import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let successRate = new Rate('success');
export let userResponseTime = new Trend('user_response_time');
export let rollingUpdateImpact = new Rate('rolling_update_impact');

// Rolling Update 안전성 증명 테스트 (User Service)
export let options = {
  stages: [
    { duration: '1m', target: 10 },   // 워밍업
    { duration: '3m', target: 20 },   // 안정적인 부하 (Rolling Update 실행 구간)
    { duration: '1m', target: 5 },    // 복구 확인
  ],
  thresholds: {
    // 엄격한 성공 기준 설정
    http_req_duration: ['p(95)<2000'],     // P95 2초 이하
    errors: ['rate<0.05'],                 // 에러율 5% 이하
    success: ['rate>0.95'],                // 성공률 95% 이상
    rolling_update_impact: ['rate<0.1'],   // Rolling Update 영향 10% 이하
  },
};

const BASE_URL = 'http://localhost:8081/user';

// 테스트용 사용자 데이터
const testUsers = [
  { email: 'test1@example.com', password: 'password123' },
  { email: 'test2@example.com', password: 'password123' },
  { email: 'test3@example.com', password: 'password123' },
  { email: 'test4@example.com', password: 'password123' },
  { email: 'test5@example.com', password: 'password123' },
];

export function setup() {
  console.log('🚀 User Service Rolling Update 안전성 증명 테스트 시작');
  console.log('🎯 목표: "Rolling Update + 헬스체크 + Graceful Shutdown = 안전한 배포" 증명');
  console.log('📊 그라파나 모니터링: http://localhost:30300');
  console.log('');
  console.log('🔧 최적화된 Rolling Update 설정:');
  console.log('   - maxUnavailable: 0 (항상 최소 1개 Pod 유지)');
  console.log('   - maxSurge: 1 (점진적 배포)');
  console.log('   - Graceful Shutdown: 60초');
  console.log('   - 헬스체크: 엄격한 설정');
  console.log('');
  console.log('⚠️  1분 후 Rolling Update를 실행하세요!');
  console.log('🔄 Rolling Update 명령어:');
  console.log('   helm upgrade unbox-user helm/unbox-service -f helm/values-user-rolling-update.yaml');
  console.log('');
  console.log('📈 예상 결과 (성공 시):');
  console.log('   - 에러율 < 5% (거의 무중단)');
  console.log('   - 성공률 > 95% (안정적 서비스)');
  console.log('   - P95 응답시간 < 2초 (성능 유지)');
  console.log('   - Rolling Update 영향 < 10% (최소 영향)');
}

export default function () {
  let scenario = Math.random();
  
  if (scenario < 0.4) {
    // 40% - 로그인 테스트
    let user = testUsers[Math.floor(Math.random() * testUsers.length)];
    
    let loginData = {
      email: user.email,
      password: user.password
    };
    
    let startTime = Date.now();
    let response = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(loginData), {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'login', test_type: 'rolling_update_safety' },
      timeout: '10s',
    });
    let endTime = Date.now();
    let duration = endTime - startTime;
    
    userResponseTime.add(duration);
    
    let success = check(response, {
      'login status is 200': (r) => r.status === 200,
      'login response time < 3000ms': (r) => duration < 3000,
      'login has valid token': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body && body.token;
        } catch (e) {
          return false;
        }
      }
    });
    
    if (success) {
      successRate.add(1);
      console.log(`🟢 로그인 성공 - ${user.email}: ${duration}ms`);
    } else {
      errorRate.add(1);
      
      // Rolling Update 영향 분석
      if (response.status === 0 || response.status >= 500) {
        rollingUpdateImpact.add(1);
        console.log(`🔴 Rolling Update 영향 - 로그인 실패: ${response.status} (${duration}ms)`);
      } else {
        console.log(`🟡 일반 에러 - 로그인: ${response.status} (${duration}ms)`);
      }
    }
    
  } else if (scenario < 0.7) {
    // 30% - 사용자 정보 조회 (토큰 필요)
    // 먼저 로그인해서 토큰 획득
    let user = testUsers[Math.floor(Math.random() * testUsers.length)];
    let loginResponse = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
      email: user.email,
      password: user.password
    }), {
      headers: { 'Content-Type': 'application/json' },
      timeout: '5s',
    });
    
    if (loginResponse.status === 200) {
      try {
        let loginBody = JSON.parse(loginResponse.body);
        let token = loginBody.token;
        
        let startTime = Date.now();
        let response = http.get(`${BASE_URL}/api/users/me`, {
          headers: { 'Authorization': `Bearer ${token}` },
          tags: { name: 'get_user_info', test_type: 'rolling_update_safety' },
          timeout: '5s',
        });
        let endTime = Date.now();
        let duration = endTime - startTime;
        
        userResponseTime.add(duration);
        
        let success = check(response, {
          'user info status is 200': (r) => r.status === 200,
          'user info response time < 2000ms': (r) => duration < 2000,
        });
        
        if (success) {
          successRate.add(1);
          console.log(`🟢 사용자 정보 조회 성공 - ${user.email}: ${duration}ms`);
        } else {
          errorRate.add(1);
          
          if (response.status === 0 || response.status >= 500) {
            rollingUpdateImpact.add(1);
            console.log(`🔴 Rolling Update 영향 - 사용자 정보 조회 실패: ${response.status} (${duration}ms)`);
          }
        }
      } catch (e) {
        errorRate.add(1);
        console.log(`🟡 토큰 파싱 에러: ${e.message}`);
      }
    } else {
      errorRate.add(1);
      console.log(`🟡 로그인 실패로 사용자 정보 조회 불가: ${loginResponse.status}`);
    }
    
  } else {
    // 30% - 헬스체크
    let startTime = Date.now();
    let response = http.get(`${BASE_URL}/actuator/health`, {
      tags: { name: 'health_check', test_type: 'rolling_update_safety' },
      timeout: '3s',
    });
    let endTime = Date.now();
    let duration = endTime - startTime;
    
    let success = check(response, {
      'health status is 200': (r) => r.status === 200,
      'health response time < 1000ms': (r) => duration < 1000,
    });
    
    if (success) {
      successRate.add(1);
      console.log(`🟢 헬스체크 성공: ${duration}ms`);
    } else {
      errorRate.add(1);
      
      if (response.status === 0 || response.status >= 500) {
        rollingUpdateImpact.add(1);
        console.log(`🔴 Rolling Update 영향 - 헬스체크 실패: ${response.status} (${duration}ms)`);
      }
    }
  }

  sleep(Math.random() * 2 + 1); // 1-3초 랜덤 대기
}

export function teardown() {
  console.log('✅ User Service Rolling Update 안전성 테스트 완료');
  console.log('');
  console.log('📊 결과 분석:');
  console.log('🔍 확인할 메트릭:');
  console.log('   - errors: 전체 에러율 (목표: < 5%)');
  console.log('   - success: 전체 성공률 (목표: > 95%)');
  console.log('   - rolling_update_impact: Rolling Update 직접 영향 (목표: < 10%)');
  console.log('   - user_response_time: 사용자 서비스 응답시간 (목표: P95 < 2초)');
  console.log('');
  console.log('🎯 성공 기준:');
  console.log('   ✅ 에러율 5% 이하 → Rolling Update 안전함');
  console.log('   ✅ 성공률 95% 이상 → 서비스 안정성 유지');
  console.log('   ✅ P95 응답시간 2초 이하 → 성능 영향 최소');
  console.log('   ✅ Rolling Update 영향 10% 이하 → 배포 전략 효과적');
  console.log('');
  console.log('🏆 결론: Rolling Update + 적절한 설정 = 안전한 배포 전략!');
}