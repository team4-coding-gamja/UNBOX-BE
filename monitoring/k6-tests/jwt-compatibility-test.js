import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let authErrorRate = new Rate('auth_errors');

// JWT 토큰 호환성 문제 재현용 테스트
export let options = {
  stages: [
    { duration: '30s', target: 10 },  // 기존 토큰으로 정상 동작 확인
    { duration: '2m', target: 20 },   // Rolling Update 실행 구간
    { duration: '30s', target: 0 },   // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    errors: ['rate<0.5'], // Rolling Update 시 높은 에러율 허용
    auth_errors: ['rate<0.8'], // 401 에러 80% 이하
  },
};

const BASE_URL = 'http://localhost:8081/user';

// 실제 DB에 있는 사용자 데이터
const users = [
  { email: 'user@unbox.com', password: '12341234!' },
  { email: 'buyer1@unbox.com', password: '12341234!' },
  { email: 'seller3@unbox.com', password: '12341234!' },
];

// 기존 토큰 (Rolling Update 전에 발급받은 토큰)
let OLD_TOKENS = [];

export function setup() {
  console.log('🚀 JWT 토큰 호환성 문제 재현 테스트 시작');
  console.log('🎯 목표: Rolling Update 시 기존 JWT 토큰으로 401 에러 발생');
  console.log('📊 그라파나 모니터링: http://localhost:30300');
  console.log('');
  
  // 기존 JWT Secret Key로 토큰들을 미리 발급받기
  console.log('🔑 기존 JWT Secret Key로 토큰 발급 중...');
  
  for (let user of users) {
    let loginResponse = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(user), {
      headers: { 'Content-Type': 'application/json' },
    });
    
    if (loginResponse.status === 200) {
      let token = loginResponse.json('accessToken');
      OLD_TOKENS.push(token);
      console.log(`✅ ${user.email}: 토큰 발급 성공`);
    } else {
      console.log(`❌ ${user.email}: 토큰 발급 실패 (${loginResponse.status})`);
    }
  }
  
  console.log('');
  console.log('⚠️  이제 Rolling Update를 실행하세요!');
  console.log('🔄 Rolling Update 명령어:');
  console.log('   helm upgrade unbox-user helm/unbox-service -f helm/values-user-rolling-update.yaml');
  console.log('');
  console.log('📈 예상 결과:');
  console.log('   - 기존 토큰으로 401 Unauthorized 에러 발생');
  console.log('   - 새로운 로그인은 성공 (새 JWT Secret Key)');
  console.log('   - auth_errors 메트릭 급증');
  
  return { oldTokens: OLD_TOKENS };
}

export default function (data) {
  let useOldToken = Math.random() < 0.7; // 70% 확률로 기존 토큰 사용
  
  if (useOldToken && data.oldTokens && data.oldTokens.length > 0) {
    // 기존 토큰으로 API 호출 (401 에러 예상)
    let oldToken = data.oldTokens[Math.floor(Math.random() * data.oldTokens.length)];
    
    let profileResponse = http.get(`${BASE_URL}/api/users/me`, {
      headers: { 
        'Authorization': `Bearer ${oldToken}`,
        'Content-Type': 'application/json'
      },
      tags: { name: 'old_token_profile', token_type: 'old' },
    });

    let success = check(profileResponse, {
      'old token profile success': (r) => r.status === 200,
    });

    if (!success) {
      errorRate.add(1);
      if (profileResponse.status === 401) {
        authErrorRate.add(1);
        console.log(`🚨 401 에러 발생! 기존 JWT 토큰 호환성 문제 재현됨`);
      }
    }
    
  } else {
    // 새로운 로그인 시도 (새 JWT Secret Key로 성공 예상)
    let user = users[Math.floor(Math.random() * users.length)];
    let loginPayload = {
      email: user.email,
      password: user.password
    };

    let loginResponse = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(loginPayload), {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'new_login', token_type: 'new' },
    });

    let loginSuccess = check(loginResponse, {
      'new login status is 200': (r) => r.status === 200,
      'new login has token': (r) => r.json('accessToken') !== undefined,
    });

    if (!loginSuccess) {
      errorRate.add(1);
      if (loginResponse.status === 401) {
        authErrorRate.add(1);
      }
    }

    // 새 토큰으로 프로필 조회
    if (loginResponse.status === 200) {
      let newToken = loginResponse.json('accessToken');
      
      let profileResponse = http.get(`${BASE_URL}/api/users/me`, {
        headers: { 
          'Authorization': `Bearer ${newToken}`,
          'Content-Type': 'application/json'
        },
        tags: { name: 'new_token_profile', token_type: 'new' },
      });

      let profileSuccess = check(profileResponse, {
        'new token profile success': (r) => r.status === 200,
      });

      if (!profileSuccess) {
        errorRate.add(1);
        if (profileResponse.status === 401) {
          authErrorRate.add(1);
        }
      }
    }
  }

  sleep(1);

  // 헬스체크
  let healthResponse = http.get(`${BASE_URL}/actuator/health`, {
    tags: { name: 'health' },
  });

  check(healthResponse, {
    'health status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(1);
}

export function teardown() {
  console.log('✅ JWT 토큰 호환성 테스트 완료');
  console.log('📊 그라파나에서 401 에러 스파이크를 확인하세요');
  console.log('📸 Rolling Update Problems Analysis 대시보드에서 결과 확인');
  console.log('');
  console.log('🔍 확인할 메트릭:');
  console.log('   - auth_errors: JWT 토큰 호환성 문제로 인한 401 에러율');
  console.log('   - HTTP 401 응답 코드 스파이크');
  console.log('   - 에러율 급증 패턴');
}