import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let authErrorRate = new Rate('auth_errors');
export let serverErrorRate = new Rate('server_errors');

// Rolling Update 문제점 재현용 부하 테스트
export let options = {
  stages: [
    { duration: '1m', target: 30 },   // 워밍업
    { duration: '10m', target: 80 },  // 지속적인 부하 (Rolling Update 실행 구간)
    { duration: '2m', target: 0 },    // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 문제 발생 시 임계값 초과 예상
    errors: ['rate<0.2'], // 20% 이하 (Rolling Update 시 일시적 증가 허용)
  },
};

const BASE_URLS = {
  user: 'http://localhost:8081',
  product: 'http://localhost:8082', 
  order: 'http://localhost:8083',
  trade: 'http://localhost:8084',
  payment: 'http://localhost:8085'
};

// 테스트 데이터
const users = [
  { email: 'seller3@unbox.com', password: '12341234!' },
  { email: 'test1@unbox.com', password: '12341234!' },
  { email: 'test2@unbox.com', password: '12341234!' }
];

const productIds = [1, 2, 3, 4, 5];
const popularProducts = [1, 2, 3]; // 캐시 테스트용

export default function () {
  let scenarioType = Math.random();
  
  if (scenarioType < 0.3) {
    // 30% - User Service 인증 플로우 (401 에러 재현용)
    userAuthenticationFlow();
  } else if (scenarioType < 0.6) {
    // 30% - Product Service 캐시 테스트 (캐시 무효화 재현용)
    productCacheFlow();
  } else if (scenarioType < 0.8) {
    // 20% - Order Service 트랜잭션 (5xx 에러 재현용)
    orderTransactionFlow();
  } else {
    // 20% - 전체 서비스 헬스체크
    healthCheckFlow();
  }
  
  sleep(1 + Math.random() * 2); // 1-3초 랜덤 대기
}

function userAuthenticationFlow() {
  // 1. 로그인 시도 (JWT 토큰 호환성 문제 재현)
  let user = users[Math.floor(Math.random() * users.length)];
  let loginPayload = {
    email: user.email,
    password: user.password
  };

  let loginResponse = http.post(`${BASE_URLS.user}/user/login`, JSON.stringify(loginPayload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { service: 'user', endpoint: 'login' },
  });

  let loginSuccess = check(loginResponse, {
    'login status is 200': (r) => r.status === 200,
  });

  if (!loginSuccess) {
    errorRate.add(1);
    if (loginResponse.status === 401) {
      authErrorRate.add(1);
    }
  }

  // 2. 인증이 필요한 API 호출
  if (loginResponse.status === 200) {
    let token = loginResponse.json('token');
    if (token) {
      let profileResponse = http.get(`${BASE_URLS.user}/user/profile`, {
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        tags: { service: 'user', endpoint: 'profile' },
      });

      if (!check(profileResponse, { 'profile status is 200': (r) => r.status === 200 })) {
        errorRate.add(1);
        if (profileResponse.status === 401) {
          authErrorRate.add(1);
        }
      }
    }
  }
}

function productCacheFlow() {
  // 1. 인기 상품 조회 (캐시 히트 예상)
  let popularId = popularProducts[Math.floor(Math.random() * popularProducts.length)];
  let popularResponse = http.get(`${BASE_URLS.product}/product/${popularId}`, {
    tags: { service: 'product', endpoint: 'popular', cache_expected: 'hit' },
  });

  check(popularResponse, {
    'popular product status is 200': (r) => r.status === 200,
    'popular product fast response': (r) => r.timings.duration < 200, // 캐시 히트 시 빨라야 함
  }) || errorRate.add(1);

  // 2. 랜덤 상품 조회 (캐시 미스 가능)
  let randomId = productIds[Math.floor(Math.random() * productIds.length)];
  let randomResponse = http.get(`${BASE_URLS.product}/product/${randomId}`, {
    tags: { service: 'product', endpoint: 'random', cache_expected: 'mixed' },
  });

  check(randomResponse, {
    'random product status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  // 3. 상품 검색 (DB 집약적)
  let searchResponse = http.get(`${BASE_URLS.product}/product/search?keyword=스니커즈&page=1&size=10`, {
    tags: { service: 'product', endpoint: 'search' },
  });

  check(searchResponse, {
    'search status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);
}

function orderTransactionFlow() {
  // 긴 트랜잭션 시뮬레이션 (Rolling Update 시 중단 위험)
  let orderPayload = {
    userId: Math.floor(Math.random() * 5) + 1,
    productId: productIds[Math.floor(Math.random() * productIds.length)],
    quantity: Math.floor(Math.random() * 3) + 1,
    shippingAddress: {
      street: '테스트 주소 123',
      city: '서울',
      zipCode: '12345'
    }
  };

  let orderResponse = http.post(`${BASE_URLS.order}/order/create`, JSON.stringify(orderPayload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { service: 'order', endpoint: 'create' },
    timeout: '10s', // 긴 트랜잭션 허용
  });

  let orderSuccess = check(orderResponse, {
    'order creation status is 200 or 201': (r) => r.status === 200 || r.status === 201,
  });

  if (!orderSuccess) {
    errorRate.add(1);
    if (orderResponse.status >= 500) {
      serverErrorRate.add(1);
    }
  }
}

function healthCheckFlow() {
  // 모든 서비스 헬스체크
  Object.entries(BASE_URLS).forEach(([service, url]) => {
    let healthResponse = http.get(`${url}/${service}/actuator/health`, {
      tags: { service: service, endpoint: 'health' },
    });

    check(healthResponse, {
      [`${service} health is 200`]: (r) => r.status === 200,
    }) || errorRate.add(1);
  });
}

export function setup() {
  console.log('🚀 Rolling Update 문제점 재현 테스트 시작');
  console.log('📊 그라파나에서 다음 대시보드들을 모니터링하세요:');
  console.log('   - UNBOX Services Overview');
  console.log('   - Rolling Update Problems Analysis');
  console.log('⚠️  부하 테스트 실행 중 Rolling Update를 수행하세요!');
  console.log('');
  console.log('🔄 Rolling Update 명령어:');
  console.log('   kubectl set image deployment/unbox-user unbox-user=new-image:tag');
  console.log('   kubectl set image deployment/unbox-product unbox-product=new-image:tag');
  console.log('   kubectl set image deployment/unbox-order unbox-order=new-image:tag');
}

export function teardown() {
  console.log('✅ Rolling Update 문제점 재현 테스트 완료');
  console.log('📊 그라파나에서 문제점 스크린샷을 캡처하세요');
}