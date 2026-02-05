import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let tradeSuccessRate = new Rate('trade_success');
export let tradeFailureRate = new Rate('trade_failure');
export let externalApiErrorRate = new Rate('external_api_errors');

// 테스트 설정
export let options = {
  stages: [
    { duration: '2m', target: 20 }, // 워밍업
    { duration: '5m', target: 50 }, // 정상 부하
    { duration: '2m', target: 100 }, // 피크 부하 (Canary 배포 시점)
    { duration: '3m', target: 80 }, // Canary 분석 기간
    { duration: '3m', target: 50 }, // 안정화
    { duration: '2m', target: 0 }, // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'], // 95%의 요청이 3초 이하
    errors: ['rate<0.05'], // 에러율 5% 이하
    trade_success: ['rate>0.95'], // 거래 성공률 95% 이상
  },
};

const BASE_URL = 'http://localhost:8084';

// 테스트 데이터
const tradeTypes = ['BUY', 'SELL'];
const tradeStatuses = ['PENDING', 'COMPLETED', 'CANCELLED'];
const testUsers = [1, 2, 3, 4, 5];
const testProducts = [1, 2, 3, 4, 5];

export default function () {
  let userId = testUsers[Math.floor(Math.random() * testUsers.length)];
  let productId = testProducts[Math.floor(Math.random() * testProducts.length)];
  let tradeType = tradeTypes[Math.floor(Math.random() * tradeTypes.length)];

  // 1. 거래 생성 (외부 API 호출 포함)
  let tradePayload = {
    userId: userId,
    productId: productId,
    tradeType: tradeType,
    quantity: Math.floor(Math.random() * 5) + 1,
    price: Math.floor(Math.random() * 100000) + 50000,
    externalMarketId: `market_${Math.random().toString(36).substr(2, 8)}`
  };

  let tradeResponse = http.post(`${BASE_URL}/trade/create`, JSON.stringify(tradePayload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'create_trade' },
    timeout: '8s',
  });

  let tradeSuccess = check(tradeResponse, {
    'trade creation status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'trade has tradeId': (r) => r.json('tradeId') !== undefined,
    'trade creation time < 5s': (r) => r.timings.duration < 5000,
  });

  if (tradeSuccess) {
    tradeSuccessRate.add(1);
  } else {
    errorRate.add(1);
    tradeFailureRate.add(1);
    if (tradeResponse.status >= 500) {
      externalApiErrorRate.add(1);
    }
  }

  let tradeId = null;
  if (tradeResponse.status === 200 || tradeResponse.status === 201) {
    tradeId = tradeResponse.json('tradeId');
  }

  sleep(2);

  // 2. 거래 상태 조회
  if (tradeId) {
    let tradeStatusResponse = http.get(`${BASE_URL}/trade/${tradeId}/status`, {
      tags: { name: 'trade_status' },
    });

    check(tradeStatusResponse, {
      'trade status query is 200': (r) => r.status === 200,
      'trade has status': (r) => r.json('status') !== undefined,
    }) || errorRate.add(1);
  }

  sleep(1);

  // 3. 거래 매칭 시뮬레이션 (복잡한 비즈니스 로직)
  let matchingPayload = {
    tradeType: tradeType === 'BUY' ? 'SELL' : 'BUY',
    productId: productId,
    maxPrice: tradePayload.price + 10000,
    minQuantity: 1
  };

  let matchingResponse = http.post(`${BASE_URL}/trade/match`, JSON.stringify(matchingPayload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'trade_matching' },
    timeout: '10s',
  });

  let matchingSuccess = check(matchingResponse, {
    'matching status is 200': (r) => r.status === 200,
    'matching has results': (r) => r.json('matches') !== undefined,
    'matching time < 8s': (r) => r.timings.duration < 8000,
  });

  if (!matchingSuccess) {
    errorRate.add(1);
    if (matchingResponse.status >= 500) {
      externalApiErrorRate.add(1);
    }
  }

  sleep(2);

  // 4. 거래 실행 (외부 결제 시스템 연동)
  if (tradeId && Math.random() > 0.6) { // 40% 확률로 거래 실행
    let executePayload = {
      paymentMethod: 'CREDIT_CARD',
      escrowEnabled: true,
      externalTransactionId: `ext_${Math.random().toString(36).substr(2, 10)}`
    };

    let executeResponse = http.post(`${BASE_URL}/trade/${tradeId}/execute`, JSON.stringify(executePayload), {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'execute_trade' },
      timeout: '12s',
    });

    let executeSuccess = check(executeResponse, {
      'trade execution status is 200': (r) => r.status === 200,
      'trade execution confirmed': (r) => r.json('executed') === true,
      'execution time < 10s': (r) => r.timings.duration < 10000,
    });

    if (executeSuccess) {
      tradeSuccessRate.add(1);
    } else {
      errorRate.add(1);
      tradeFailureRate.add(1);
      if (executeResponse.status >= 500) {
        externalApiErrorRate.add(1);
      }
    }
  }

  sleep(1);

  // 5. 사용자별 거래 히스토리 조회
  let historyResponse = http.get(`${BASE_URL}/trade/user/${userId}/history?page=1&size=10`, {
    tags: { name: 'trade_history' },
  });

  check(historyResponse, {
    'history status is 200': (r) => r.status === 200,
    'history has content': (r) => r.json('content') !== undefined,
  }) || errorRate.add(1);

  sleep(1);

  // 6. 거래 취소 (복잡한 롤백 로직)
  if (tradeId && Math.random() > 0.85) { // 15% 확률로 거래 취소
    let cancelResponse = http.delete(`${BASE_URL}/trade/${tradeId}/cancel`, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'cancel_trade' },
    });

    let cancelSuccess = check(cancelResponse, {
      'trade cancel status is 200': (r) => r.status === 200,
      'trade cancel confirmed': (r) => r.json('cancelled') === true,
    });

    if (!cancelSuccess && cancelResponse.status >= 500) {
      externalApiErrorRate.add(1);
    }
  }

  sleep(2);

  // 7. 시장 데이터 조회 (외부 API 의존성)
  let marketDataResponse = http.get(`${BASE_URL}/trade/market/data?productId=${productId}`, {
    tags: { name: 'market_data' },
    timeout: '5s',
  });

  check(marketDataResponse, {
    'market data status is 200': (r) => r.status === 200,
    'market data has prices': (r) => r.json('currentPrice') !== undefined,
  }) || errorRate.add(1);

  sleep(1);

  // 8. 헬스체크
  let healthResponse = http.get(`${BASE_URL}/trade/actuator/health`, {
    tags: { name: 'health' },
  });

  check(healthResponse, {
    'health status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(1);
}

export function setup() {
  console.log('🚀 Trade Service Load Test Started');
  console.log('📊 Target: Automated Analysis & Rollback Detection');
  console.log('⚠️  Intentional errors may be introduced for Canary analysis');
  console.log('🔄 Watch for automatic rollback when success rate drops below 95%');
  console.log('📈 External API dependencies will be stressed');
}

export function teardown() {
  console.log('✅ Trade Service Load Test Completed');
  console.log('📊 Check AnalysisRun results in ArgoCD');
  console.log('📈 Verify automatic rollback behavior in Grafana');
}