import http from 'k6/http';
import { check, sleep } from 'k6';

// 베이스라인 수집용 가벼운 부하 테스트
export let options = {
  stages: [
    { duration: '2m', target: 10 }, // 가벼운 부하
    { duration: '5m', target: 20 }, // 안정적인 부하 유지
    { duration: '2m', target: 0 },  // 종료
  ],
};

const services = [
  { name: 'user', port: 8081, endpoints: ['/user/actuator/health'] },
  { name: 'product', port: 8082, endpoints: ['/product/actuator/health'] },
  { name: 'order', port: 8083, endpoints: ['/order/actuator/health'] },
  { name: 'trade', port: 8084, endpoints: ['/trade/actuator/health'] },
  { name: 'payment', port: 8085, endpoints: ['/payment/actuator/health'] },
];

export default function () {
  // 각 서비스의 헬스체크 엔드포인트 호출
  services.forEach(service => {
    service.endpoints.forEach(endpoint => {
      let response = http.get(`http://localhost:${service.port}${endpoint}`, {
        tags: { service: service.name, endpoint: endpoint },
      });
      
      check(response, {
        [`${service.name} health check is 200`]: (r) => r.status === 200,
      });
    });
  });
  
  sleep(2);
}

export function setup() {
  console.log('🔍 베이스라인 메트릭 수집 시작');
  console.log('📊 그라파나에서 "UNBOX Services Overview" 대시보드를 확인하세요');
}

export function teardown() {
  console.log('✅ 베이스라인 메트릭 수집 완료');
}