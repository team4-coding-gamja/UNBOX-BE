import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let paymentSuccessRate = new Rate('payment_success');
export let paymentFailureRate = new Rate('payment_failure');
export let kafkaEventRate = new Rate('kafka_events');

// 테스트 설정
export let options = {
  stages: [
    { duration: '2m', target: 15 }, // 워밍업
    { duration: '5m', target: 40 }, // 정상 부하
    { duration: '2m', target: 80 }, // 피크 부하 (Blue-Green 전환 시점)
    { duration: '5m', target: 40 }, // 안정화
    { duration: '2m', target: 0 }, // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<4000'], // 95%의 요청이 4초 이하
    errors: ['rate<0.02'], // 에러율 2% 이하 (결제는 더 엄격)
    payment_success: ['rate>0.98'], // 결제 성공률 98% 이상
  },
};

const BASE_URL = 'http://localhost:8085';

// 테스트 데이터
const paymentMethods = ['CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'DIGITAL_WALLET'];
const currencies = ['KRW', 'USD'];
const testOrders = [
  { orderId: 'ORD001', amount: 150000, currency: 'KRW' },
  { orderId: 'ORD002', amount: 120000, currency: 'KRW' },
  { orderId: 'ORD003', amount: 200000, currency: 'KRW' },
];

export default function () {
  let order = testOrders[Math.floor(Math.random() * testOrders.length)];
  let paymentMethod = paymentMethods[Math.floor(Math.random() * paymentMethods.length)];

  // 1. 결제 요청 (Toss Payments 연동)
  let paymentPayload = {
    orderId: `${order.orderId}_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
    amount: order.amount,
    currency: order.currency,
    paymentMethod: paymentMethod,
    customerInfo: {
      name: '테스트 고객',
      email: 'test@unbox.com',
      phone: '010-1234-5678'
    },
    productInfo: {
      name: '한정판 스니커즈',
      category: 'SHOES'
    }
  };

  let paymentResponse = http.post(`${BASE_URL}/payment/process`, JSON.stringify(paymentPayload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'process_payment' },
    timeout: '15s', // 결제는 시간이 오래 걸릴 수 있음
  });

  let paymentSuccess = check(paymentResponse, {
    'payment status is 200': (r) => r.status === 200,
    'payment has transactionId': (r) => r.json('transactionId') !== undefined,
    'payment processing time < 12s': (r) => r.timings.duration < 12000,
    'payment amount matches': (r) => r.json('amount') === paymentPayload.amount,
  });

  if (paymentSuccess) {
    paymentSuccessRate.add(1);
    kafkaEventRate.add(1); // 성공 시 Kafka 이벤트 발행
  } else {
    errorRate.add(1);
    paymentFailureRate.add(1);
  }

  let transactionId = null;
  if (paymentResponse.status === 200) {
    transactionId = paymentResponse.json('transactionId');
  }

  sleep(3);

  // 2. 결제 상태 조회
  if (transactionId) {
    let statusResponse = http.get(`${BASE_URL}/payment/${transactionId}/status`, {
      tags: { name: 'payment_status' },
    });

    check(statusResponse, {
      'payment status query is 200': (r) => r.status === 200,
      'payment has status': (r) => r.json('status') !== undefined,
    }) || errorRate.add(1);
  }

  sleep(1);

  // 3. 결제 검증 (Toss Payments 웹훅 시뮬레이션)
  if (transactionId && Math.random() > 0.3) { // 70% 확률로 검증
    let verifyPayload = {
      transactionId: transactionId,
      externalTransactionId: `toss_${Math.random().toString(36).substr(2, 10)}`,
      status: 'COMPLETED',
      verificationCode: Math.random().toString(36).substr(2, 8)
    };

    let verifyResponse = http.post(`${BASE_URL}/payment/${transactionId}/verify`, JSON.stringify(verifyPayload), {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'verify_payment' },
    });

    let verifySuccess = check(verifyResponse, {
      'payment verification is 200': (r) => r.status === 200,
      'payment verified': (r) => r.json('verified') === true,
    });

    if (verifySuccess) {
      kafkaEventRate.add(1); // 검증 완료 시 Kafka 이벤트 발행
    } else {
      errorRate.add(1);
    }
  }

  sleep(2);

  // 4. 결제 환불 (복잡한 비즈니스 로직)
  if (transactionId && Math.random() > 0.9) { // 10% 확률로 환불
    let refundPayload = {
      reason: 'CUSTOMER_REQUEST',
      refundAmount: order.amount,
      refundMethod: 'ORIGINAL_PAYMENT_METHOD'
    };

    let refundResponse = http.post(`${BASE_URL}/payment/${transactionId}/refund`, JSON.stringify(refundPayload), {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'refund_payment' },
      timeout: '10s',
    });

    let refundSuccess = check(refundResponse, {
      'refund status is 200': (r) => r.status === 200,
      'refund processed': (r) => r.json('refunded') === true,
      'refund time < 8s': (r) => r.timings.duration < 8000,
    });

    if (refundSuccess) {
      kafkaEventRate.add(1); // 환불 완료 시 Kafka 이벤트 발행
    } else {
      errorRate.add(1);
    }
  }

  sleep(1);

  // 5. 결제 히스토리 조회
  let historyResponse = http.get(`${BASE_URL}/payment/history?orderId=${paymentPayload.orderId}`, {
    tags: { name: 'payment_history' },
  });

  check(historyResponse, {
    'history status is 200': (r) => r.status === 200,
    'history has transactions': (r) => r.json('transactions') !== undefined,
  }) || errorRate.add(1);

  sleep(1);

  // 6. 결제 통계 조회 (DB 집약적)
  let statsResponse = http.get(`${BASE_URL}/payment/stats/daily?date=${new Date().toISOString().split('T')[0]}`, {
    tags: { name: 'payment_stats' },
  });

  check(statsResponse, {
    'stats status is 200': (r) => r.status === 200,
    'stats has data': (r) => r.json('totalAmount') !== undefined,
  }) || errorRate.add(1);

  sleep(1);

  // 7. Kafka 이벤트 상태 확인 (Consumer Lag 모니터링용)
  let kafkaHealthResponse = http.get(`${BASE_URL}/payment/kafka/health`, {
    tags: { name: 'kafka_health' },
  });

  check(kafkaHealthResponse, {
    'kafka health is 200': (r) => r.status === 200,
    'kafka consumer is healthy': (r) => {
      try {
        let health = r.json();
        return health.consumerLag !== undefined && health.consumerLag < 100;
      } catch (e) {
        return false;
      }
    },
  }) || errorRate.add(1);

  sleep(1);

  // 8. 헬스체크 (DB 커넥션 포함)
  let healthResponse = http.get(`${BASE_URL}/payment/actuator/health`, {
    tags: { name: 'health' },
  });

  let healthSuccess = check(healthResponse, {
    'health status is 200': (r) => r.status === 200,
    'database is healthy': (r) => {
      try {
        let health = r.json();
        return health.components && health.components.db && 
               health.components.db.status === 'UP';
      } catch (e) {
        return false;
      }
    },
  });

  if (!healthSuccess) {
    errorRate.add(1);
  }

  sleep(2);
}

export function setup() {
  console.log('🚀 Payment Service Load Test Started');
  console.log('📊 Target: Transaction Integrity & Blue-Green Zero-Downtime');
  console.log('💳 Toss Payments integration will be tested');
  console.log('📨 Kafka event publishing will be monitored');
  console.log('⚠️  Watch for payment data consistency during Blue-Green switches');
  console.log('🔒 Zero payment failures should occur during deployment');
}

export function teardown() {
  console.log('✅ Payment Service Load Test Completed');
  console.log('💰 Check payment success rate and transaction integrity');
  console.log('📊 Verify Kafka consumer lag remained stable');
}