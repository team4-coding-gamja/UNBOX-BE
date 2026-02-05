import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let transactionErrorRate = new Rate('transaction_errors');
export let longRunningTransactions = new Rate('long_transactions');

// 테스트 설정
export let options = {
  stages: [
    { duration: '2m', target: 25 }, // 워밍업
    { duration: '5m', target: 60 }, // 정상 부하
    { duration: '2m', target: 120 }, // 피크 부하 (Rolling Update 시점)
    { duration: '5m', target: 60 }, // 안정화
    { duration: '2m', target: 0 }, // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95%의 요청이 2초 이하
    errors: ['rate<0.05'], // 에러율 5% 이하
    transaction_errors: ['rate<0.02'], // 트랜잭션 에러율 2% 이하
  },
};

const BASE_URL = 'http://localhost:8083';

// 테스트 데이터
const testUsers = [
  { id: 1, email: 'test1@unbox.com' },
  { id: 2, email: 'test2@unbox.com' },
  { id: 3, email: 'seller3@unbox.com' },
];

const testProducts = [
  { id: 1, name: 'Air Jordan 1', price: 150000 },
  { id: 2, name: 'Nike Dunk Low', price: 120000 },
  { id: 3, name: 'Adidas Yeezy', price: 200000 },
];

export default function () {
  let user = testUsers[Math.floor(Math.random() * testUsers.length)];
  let product = testProducts[Math.floor(Math.random() * testProducts.length)];

  // 1. 주문 생성 (긴 트랜잭션 시뮬레이션)
  let orderPayload = {
    userId: user.id,
    productId: product.id,
    quantity: Math.floor(Math.random() * 3) + 1,
    shippingAddress: {
      street: '테스트 주소 123',
      city: '서울',
      zipCode: '12345'
    },
    paymentMethod: 'CREDIT_CARD'
  };

  let startTime = Date.now();
  
  let orderResponse = http.post(`${BASE_URL}/order/create`, JSON.stringify(orderPayload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'create_order' },
    timeout: '10s', // 긴 트랜잭션 허용
  });

  let duration = Date.now() - startTime;
  if (duration > 5000) {
    longRunningTransactions.add(1);
  }

  let orderSuccess = check(orderResponse, {
    'order creation status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'order has orderId': (r) => r.json('orderId') !== undefined,
    'order creation time < 8s': (r) => r.timings.duration < 8000,
  });

  if (!orderSuccess) {
    errorRate.add(1);
    if (orderResponse.status >= 500) {
      transactionErrorRate.add(1);
    }
  }

  let orderId = null;
  if (orderResponse.status === 200 || orderResponse.status === 201) {
    orderId = orderResponse.json('orderId');
  }

  sleep(2);

  // 2. 주문 상태 조회
  if (orderId) {
    let orderStatusResponse = http.get(`${BASE_URL}/order/${orderId}/status`, {
      tags: { name: 'order_status' },
    });

    check(orderStatusResponse, {
      'order status query is 200': (r) => r.status === 200,
      'order has status': (r) => r.json('status') !== undefined,
    }) || errorRate.add(1);
  }

  sleep(1);

  // 3. 사용자별 주문 목록 조회
  let userOrdersResponse = http.get(`${BASE_URL}/order/user/${user.id}?page=1&size=10`, {
    tags: { name: 'user_orders' },
  });

  check(userOrdersResponse, {
    'user orders status is 200': (r) => r.status === 200,
    'user orders has content': (r) => r.json('content') !== undefined,
  }) || errorRate.add(1);

  sleep(1);

  // 4. 주문 수정 (재고 확인 포함)
  if (orderId && Math.random() > 0.7) { // 30% 확률로 주문 수정
    let updatePayload = {
      quantity: Math.floor(Math.random() * 2) + 1,
      shippingAddress: {
        street: '수정된 주소 456',
        city: '부산',
        zipCode: '54321'
      }
    };

    let updateResponse = http.put(`${BASE_URL}/order/${orderId}`, JSON.stringify(updatePayload), {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'update_order' },
    });

    check(updateResponse, {
      'order update status is 200': (r) => r.status === 200,
      'order update successful': (r) => r.json('success') === true,
    }) || errorRate.add(1);
  }

  sleep(1);

  // 5. 주문 취소 (트랜잭션 롤백 테스트)
  if (orderId && Math.random() > 0.8) { // 20% 확률로 주문 취소
    let cancelResponse = http.delete(`${BASE_URL}/order/${orderId}/cancel`, {
      tags: { name: 'cancel_order' },
    });

    let cancelSuccess = check(cancelResponse, {
      'order cancel status is 200': (r) => r.status === 200,
      'order cancel confirmed': (r) => r.json('cancelled') === true,
    });

    if (!cancelSuccess && cancelResponse.status >= 500) {
      transactionErrorRate.add(1);
    }
  }

  sleep(2);

  // 6. 헬스체크 (Circuit Breaker 상태 포함)
  let healthResponse = http.get(`${BASE_URL}/order/actuator/health`, {
    tags: { name: 'health' },
  });

  let healthCheck = check(healthResponse, {
    'health status is 200': (r) => r.status === 200,
    'circuit breaker is healthy': (r) => {
      try {
        let health = r.json();
        return health.components && health.components.circuitBreakers && 
               health.components.circuitBreakers.status === 'UP';
      } catch (e) {
        return false;
      }
    },
  });

  if (!healthCheck) {
    errorRate.add(1);
  }

  sleep(1);
}

export function setup() {
  console.log('🚀 Order Service Load Test Started');
  console.log('📊 Target: Transaction Integrity & Zero-Downtime Processing');
  console.log('⚠️  Watch for 5xx errors and transaction interruptions during Rolling Updates');
  console.log('🔄 Circuit Breaker to Trade Service will be monitored');
}

export function teardown() {
  console.log('✅ Order Service Load Test Completed');
  console.log('📈 Check Grafana for transaction continuity metrics');
}