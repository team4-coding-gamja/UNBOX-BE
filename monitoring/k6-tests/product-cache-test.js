import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 정의
export let errorRate = new Rate('errors');
export let cacheHitRate = new Rate('cache_hits');
export let cacheMissRate = new Rate('cache_misses');
export let responseTime = new Trend('product_response_time');

// Product Service 캐시 성능 테스트
export let options = {
  stages: [
    { duration: '1m', target: 20 },   // 캐시 워밍업
    { duration: '2m', target: 30 },   // Rolling Update 실행 구간
    { duration: '1m', target: 10 },   // 복구 확인
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    errors: ['rate<0.1'],
    product_response_time: ['p(95)<1000'], // 캐시 히트 시 1초 이하
  },
};

const BASE_URL = 'http://localhost:8082/product';

// 인기 상품 ID들 (캐시 테스트용)
const popularProducts = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

// 검색 키워드들 (캐시 테스트용)
const searchKeywords = ['laptop', 'phone', 'tablet', 'watch', 'headphone'];

export function setup() {
  console.log('🚀 Product Service 캐시 성능 테스트 시작');
  console.log('🎯 목표: Rolling Update 시 캐시 히트율 급락 재현');
  console.log('📊 그라파나 모니터링: http://localhost:30300');
  console.log('');
  console.log('🔥 캐시 워밍업 중...');
  
  // 캐시 워밍업 - 인기 상품들을 미리 조회
  for (let productId of popularProducts) {
    let response = http.get(`${BASE_URL}/api/products/${productId}`);
    if (response.status === 200) {
      console.log(`✅ 상품 ${productId} 캐시 워밍업 완료`);
    }
  }
  
  // 검색 결과 캐시 워밍업
  for (let keyword of searchKeywords) {
    let response = http.get(`${BASE_URL}/api/products/search?keyword=${keyword}`);
    if (response.status === 200) {
      console.log(`✅ 검색 "${keyword}" 캐시 워밍업 완료`);
    }
  }
  
  console.log('');
  console.log('⚠️  이제 Rolling Update를 실행하세요!');
  console.log('🔄 Rolling Update 명령어:');
  console.log('   helm upgrade unbox-product helm/unbox-service -f helm/values-product-rolling-update.yaml');
  console.log('');
  console.log('📈 예상 결과:');
  console.log('   - 캐시 히트율: 90% → 10% 급락');
  console.log('   - 응답시간: 100ms → 1000ms+ 증가');
  console.log('   - DB 부하 급증');
}

export default function () {
  let scenario = Math.random();
  
  if (scenario < 0.6) {
    // 60% - 인기 상품 조회 (캐시 히트 예상)
    let productId = popularProducts[Math.floor(Math.random() * popularProducts.length)];
    
    let startTime = Date.now();
    let response = http.get(`${BASE_URL}/api/products/${productId}`, {
      tags: { name: 'popular_product', cache_type: 'hit_expected' },
    });
    let endTime = Date.now();
    let duration = endTime - startTime;
    
    responseTime.add(duration);
    
    let success = check(response, {
      'popular product status is 200': (r) => r.status === 200,
      'popular product response time < 500ms': (r) => duration < 500,
    });
    
    if (!success) {
      errorRate.add(1);
    }
    
    // 응답시간으로 캐시 히트/미스 추정
    if (duration < 200) {
      cacheHitRate.add(1);
      console.log(`🟢 캐시 HIT - 상품 ${productId}: ${duration}ms`);
    } else {
      cacheMissRate.add(1);
      console.log(`🔴 캐시 MISS - 상품 ${productId}: ${duration}ms`);
    }
    
  } else if (scenario < 0.8) {
    // 20% - 상품 검색 (캐시 히트 예상)
    let keyword = searchKeywords[Math.floor(Math.random() * searchKeywords.length)];
    
    let startTime = Date.now();
    let response = http.get(`${BASE_URL}/api/products/search?keyword=${keyword}`, {
      tags: { name: 'product_search', cache_type: 'hit_expected' },
    });
    let endTime = Date.now();
    let duration = endTime - startTime;
    
    responseTime.add(duration);
    
    let success = check(response, {
      'search status is 200': (r) => r.status === 200,
      'search response time < 800ms': (r) => duration < 800,
    });
    
    if (!success) {
      errorRate.add(1);
    }
    
    // 응답시간으로 캐시 히트/미스 추정
    if (duration < 300) {
      cacheHitRate.add(1);
      console.log(`🟢 캐시 HIT - 검색 "${keyword}": ${duration}ms`);
    } else {
      cacheMissRate.add(1);
      console.log(`🔴 캐시 MISS - 검색 "${keyword}": ${duration}ms`);
    }
    
  } else {
    // 20% - 새로운 상품 조회 (캐시 미스 예상)
    let newProductId = Math.floor(Math.random() * 1000) + 100;
    
    let startTime = Date.now();
    let response = http.get(`${BASE_URL}/api/products/${newProductId}`, {
      tags: { name: 'new_product', cache_type: 'miss_expected' },
    });
    let endTime = Date.now();
    let duration = endTime - startTime;
    
    responseTime.add(duration);
    
    // 404는 정상 (존재하지 않는 상품)
    let success = check(response, {
      'new product response received': (r) => r.status === 200 || r.status === 404,
    });
    
    if (!success) {
      errorRate.add(1);
    }
    
    if (response.status === 200) {
      cacheMissRate.add(1);
      console.log(`🔴 캐시 MISS - 신규 상품 ${newProductId}: ${duration}ms`);
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

  sleep(Math.random() * 2); // 0-2초 랜덤 대기
}

export function teardown() {
  console.log('✅ Product Service 캐시 성능 테스트 완료');
  console.log('📊 그라파나에서 캐시 성능 변화를 확인하세요');
  console.log('📸 Rolling Update Problems Analysis 대시보드에서 결과 확인');
  console.log('');
  console.log('🔍 확인할 메트릭:');
  console.log('   - cache_hits/cache_misses: 캐시 히트율 변화');
  console.log('   - product_response_time: 응답시간 증가');
  console.log('   - HTTP 응답시간 P95 스파이크');
  console.log('   - Redis 메트릭 변화');
}