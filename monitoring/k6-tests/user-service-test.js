import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let authErrorRate = new Rate('auth_errors');

// 테스트 설정
export let options = {
  stages: [
    { duration: '2m', target: 20 }, // 워밍업
    { duration: '5m', target: 50 }, // 정상 부하
    { duration: '2m', target: 100 }, // 피크 부하 (Rolling Update 시점)
    { duration: '5m', target: 50 }, // 안정화
    { duration: '2m', target: 0 }, // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이하
    errors: ['rate<0.1'], // 에러율 10% 이하
    auth_errors: ['rate<0.05'], // 인증 에러율 5% 이하
  },
};

const BASE_URL = 'http://localhost:8081';

// 테스트 데이터
const users = [
  { email: 'test1@unbox.com', password: '12341234!' },
  { email: 'test2@unbox.com', password: '12341234!' },
  { email: 'seller3@unbox.com', password: '12341234!' }, // 시드 데이터
];

export default function () {
  // 1. 회원가입 테스트 (새 사용자)
  let signupPayload = {
    email: `user${Math.random().toString(36).substr(2, 9)}@unbox.com`,
    password: '12341234!',
    nickname: `testuser${Math.random().toString(36).substr(2, 5)}`,
    phone: '010-1234-5678'
  };

  let signupResponse = http.post(`${BASE_URL}/user/signup`, JSON.stringify(signupPayload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'signup' },
  });

  check(signupResponse, {
    'signup status is 200 or 409': (r) => r.status === 200 || r.status === 409,
  }) || errorRate.add(1);

  sleep(1);

  // 2. 로그인 테스트 (기존 사용자)
  let user = users[Math.floor(Math.random() * users.length)];
  let loginPayload = {
    email: user.email,
    password: user.password
  };

  let loginResponse = http.post(`${BASE_URL}/user/login`, JSON.stringify(loginPayload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'login' },
  });

  let loginSuccess = check(loginResponse, {
    'login status is 200': (r) => r.status === 200,
    'login has token': (r) => r.json('token') !== undefined,
  });

  if (!loginSuccess) {
    errorRate.add(1);
    if (loginResponse.status === 401) {
      authErrorRate.add(1);
    }
  }

  let token = '';
  if (loginResponse.status === 200) {
    token = loginResponse.json('token');
  }

  sleep(1);

  // 3. 인증이 필요한 API 테스트 (프로필 조회)
  if (token) {
    let profileResponse = http.get(`${BASE_URL}/user/profile`, {
      headers: { 
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      tags: { name: 'profile' },
    });

    let profileSuccess = check(profileResponse, {
      'profile status is 200': (r) => r.status === 200,
      'profile has user data': (r) => r.json('email') !== undefined,
    });

    if (!profileSuccess) {
      errorRate.add(1);
      if (profileResponse.status === 401) {
        authErrorRate.add(1);
      }
    }
  }

  sleep(2);

  // 4. 헬스체크 (모니터링용)
  let healthResponse = http.get(`${BASE_URL}/user/actuator/health`, {
    tags: { name: 'health' },
  });

  check(healthResponse, {
    'health status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(1);
}

// 테스트 시작/종료 시 실행
export function setup() {
  console.log('🚀 User Service Load Test Started');
  console.log('📊 Target: Authentication & JWT Token Compatibility');
  console.log('⚠️  Watch for 401 errors during Rolling Updates');
}

export function teardown() {
  console.log('✅ User Service Load Test Completed');
}