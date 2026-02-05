import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let authErrorRate = new Rate('auth_errors');

// JWT 토큰 호환성 문제 재현용 테스트 (짧은 버전)
export let options = {
  stages: [
    { duration: '30s', target: 20 },  // 워밍업
    { duration: '2m', target: 50 },   // 지속적인 부하 (Rolling Update 실행 구간)
    { duration: '30s', target: 0 },   // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    errors: ['rate<0.3'], // Rolling Update 시 일시적 증가 허용
    auth_errors: ['rate<0.2'], // 401 에러 20% 이하
  },
};

const BASE_URL = 'http://localhost:8081/user';

// 실제 DB에 있는 사용자 데이터
const users = [
  { email: 'user@unbox.com', password: '12341234!' },
  { email: 'buyer1@unbox.com', password: '12341234!' },
  { email: 'seller3@unbox.com', password: '12341234!' },
];

export default function () {
  // 1. 로그인 테스트 (JWT 토큰 발급)
  let user = users[Math.floor(Math.random() * users.length)];
  let loginPayload = {
    email: user.email,
    password: user.password
  };

  let loginResponse = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(loginPayload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });

  let loginSuccess = check(loginResponse, {
    'login status is 200': (r) => r.status === 200,
    'login has token': (r) => r.json('accessToken') !== undefined,
  });

  if (!loginSuccess) {
    errorRate.add(1);
    if (loginResponse.status === 401) {
      authErrorRate.add(1);
    }
  }

  let token = '';
  if (loginResponse.status === 200) {
    token = loginResponse.json('accessToken');
  }

  sleep(1);

  // 2. 인증이 필요한 API 호출 (JWT 토큰 검증)
  if (token) {
    let profileResponse = http.get(`${BASE_URL}/api/users/me`, {
      headers: { 
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      tags: { name: 'profile' },
    });

    let profileSuccess = check(profileResponse, {
      'profile status is 200': (r) => r.status === 200,
    });

    if (!profileSuccess) {
      errorRate.add(1);
      if (profileResponse.status === 401) {
        authErrorRate.add(1);
      }
    }
  }

  sleep(1);

  // 3. 헬스체크
  let healthResponse = http.get(`${BASE_URL}/actuator/health`, {
    tags: { name: 'health' },
  });

  check(healthResponse, {
    'health status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(1);
}

export function setup() {
  console.log('🚀 User Service JWT 토큰 호환성 테스트 시작');
  console.log('🎯 목표: Rolling Update 시 401 인증 에러 스파이크 재현');
  console.log('📊 그라파나 모니터링: http://localhost:30300');
  console.log('⚠️  부하 테스트 실행 중 Rolling Update를 수행하세요!');
  console.log('');
  console.log('🔄 Rolling Update 명령어:');
  console.log('   kubectl set image deployment/unbox-user unbox-user=k3d-unbox-registry:5000/unbox-user:simple');
  console.log('');
  console.log('📈 예상 문제점:');
  console.log('   - 신구 버전 JWT Secret Key 불일치');
  console.log('   - 401 Unauthorized 에러 스파이크');
  console.log('   - 인증 실패율 급증');
}

export function teardown() {
  console.log('✅ JWT 토큰 호환성 테스트 완료');
  console.log('📊 그라파나에서 401 에러 스파이크를 확인하세요');
  console.log('📸 Rolling Update Problems Analysis 대시보드 스크린샷 캡처');
}