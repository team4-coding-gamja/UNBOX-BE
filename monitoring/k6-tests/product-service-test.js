import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let cacheHitRate = new Rate('cache_hits');
export let cacheMissRate = new Rate('cache_misses');

// 테스트 설정
export let options = {
  stages: [
    { duration: '2m', target: 30 }, // 캐시 워밍업
    { duration: '5m', target: 80 }, // 정상 부하
    { duration: '2m', target: 150 }, // 피크 부하 (Rolling Update 시점)
    { duration: '5m', target: 80 }, // 안정화
    { duration: '2m', target: 0 }, // 종료
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95%의 요청이 1초 이하
    http_req_duration_cache_hit: ['p(95)<200'], // 캐시 히트 시 200ms 이하
    http_req_duration_cache_miss: ['p(95)<2000'], // 캐시 미스 시 2초 이하
    errors: ['rate<0.1'], // 에러율 10% 이하
  },
};

const BASE_URL = 'http://localhost:8082';

// 테스트용 상품 ID 목록 (캐시 테스트용)
const productIds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
const popularProductIds = [1, 2, 3]; // 자주 조회되는 상품 (캐시 히트 예상)

export default function () {
  // 1. 인기 상품 조회 (캐시 히트 예상)
  let popularProductId = popularProductIds[Math.floor(Math.random() * popularProductIds.length)];
  
  let popularProductResponse = http.get(`${BASE_URL}/product/${popularProductId}`, {
    tags: { name: 'popular_product', cache_expected: 'hit' },
  });

  let popularProductSuccess = check(popularProductResponse, {
    'popular product status is 200': (r) => r.status === 200,
    'popular product has data': (r) => r.json('id') !== undefined,
    'popular product response time < 300ms': (r) => r.timings.duration < 300,
  });

  if (popularProductSuccess && popularProductResponse.timings.duration < 300) {
    cacheHitRate.add(1);
  } else if (popularProductResponse.status === 200) {
    cacheMissRate.add(1);
  } else {
    errorRate.add(1);
  }

  sleep(1);

  // 2. 랜덤 상품 조회 (캐시 미스 가능성)
  let randomProductId = productIds[Math.floor(Math.random() * productIds.length)];
  
  let randomProductResponse = http.get(`${BASE_URL}/product/${randomProductId}`, {
    tags: { name: 'random_product', cache_expected: 'mixed' },
  });

  check(randomProductResponse, {
    'random product status is 200': (r) => r.status === 200,
    'random product has data': (r) => r.json('id') !== undefined,
  }) || errorRate.add(1);

  sleep(1);

  // 3. 상품 목록 조회 (페이징)
  let page = Math.floor(Math.random() * 5) + 1;
  let size = 10;
  
  let productListResponse = http.get(`${BASE_URL}/product/list?page=${page}&size=${size}`, {
    tags: { name: 'product_list' },
  });

  check(productListResponse, {
    'product list status is 200': (r) => r.status === 200,
    'product list has content': (r) => r.json('content') !== undefined,
    'product list response time < 1s': (r) => r.timings.duration < 1000,
  }) || errorRate.add(1);

  sleep(1);

  // 4. 상품 검색 (DB 집약적 작업)
  let searchKeywords = ['한정판', '스니커즈', '콜라보', '레어', '빈티지'];
  let keyword = searchKeywords[Math.floor(Math.random() * searchKeywords.length)];
  
  let searchResponse = http.get(`${BASE_URL}/product/search?keyword=${encodeURIComponent(keyword)}&page=1&size=10`, {
    tags: { name: 'product_search' },
  });

  check(searchResponse, {
    'search status is 200': (r) => r.status === 200,
    'search has results': (r) => r.json('content') !== undefined,
  }) || errorRate.add(1);

  sleep(2);

  // 5. 상품 상세 정보 + 리뷰 (Feature Flag 테스트 대상)
  let detailProductId = popularProductIds[Math.floor(Math.random() * popularProductIds.length)];
  
  let detailResponse = http.get(`${BASE_URL}/product/${detailProductId}/detail`, {
    headers: {
      'X-Feature-Flag': Math.random() > 0.5 ? 'reviews-enabled' : 'reviews-disabled'
    },
    tags: { name: 'product_detail_with_reviews' },
  });

  check(detailResponse, {
    'detail status is 200': (r) => r.status === 200,
    'detail has product info': (r) => r.json('product') !== undefined,
  }) || errorRate.add(1);

  sleep(1);

  // 6. 헬스체크
  let healthResponse = http.get(`${BASE_URL}/product/actuator/health`, {
    tags: { name: 'health' },
  });

  check(healthResponse, {
    'health status is 200': (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(1);
}

// 캐시 워밍업 함수
export function setup() {
  console.log('🚀 Product Service Load Test Started');
  console.log('📊 Target: Cache Performance & Feature Flag Control');
  console.log('🔥 Warming up cache with popular products...');
  
  // 인기 상품들을 미리 조회하여 캐시 워밍업
  popularProductIds.forEach(id => {
    for (let i = 0; i < 3; i++) {
      http.get(`${BASE_URL}/product/${id}`);
      sleep(0.1);
    }
  });
  
  console.log('✅ Cache warmed up');
  console.log('⚠️  Watch for Cache Hit Rate drops during Rolling Updates');
}

export function teardown() {
  console.log('✅ Product Service Load Test Completed');
}