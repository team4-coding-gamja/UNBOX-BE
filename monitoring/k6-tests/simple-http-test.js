import http from 'k6/http';
import { sleep } from 'k6';

export let options = {
  stages: [
    { duration: '1m', target: 10 },
  ],
};

export default function () {
  // 헬스체크 요청 (HTTP 메트릭 생성용)
  http.get('http://localhost:8081/user/actuator/health');
  sleep(1);
}

export function setup() {
  console.log('📊 HTTP 메트릭 생성용 간단한 테스트 시작');
}