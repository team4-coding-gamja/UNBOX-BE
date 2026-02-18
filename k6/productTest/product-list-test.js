import http from 'k6/http';
import { check, sleep, group } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 30 }, // Warm-up (유저 30명까지)
        { duration: '2m', target: 30 },  // 유지
        { duration: '30s', target: 0 },  // 종료
    ],
    thresholds: {
        http_req_duration: ['p(95)<400'], // 95% 응답이 400ms 이내 목표
    },
};

// const BASE_URL = 'https://dev.un-box.click/product/api/test/products/v1';
const BASE_URL = 'http://localhost:8082/product/api/test/products/v1';

export default function () {
    const rand = Math.random();

    group('SHOES Category Load Test', function () {
        if (rand < 0.5) {
            // [시나리오 1] 일반 전체 목록 조회 (첫 페이지)
            let res = http.get(`${BASE_URL}?page=0&size=20`);
            check(res, { 'status is 200': (r) => r.status === 200 });

        } else if (rand < 0.8) {
            // [시나리오 2] 카테고리 필터링 (SHOES 고정)
            // 현재 유일한 카테고리이므로 인덱스 스캔 효율을 측정하기 좋음
            let res = http.get(`${BASE_URL}?category=SHOES&page=0&size=20`);
            check(res, { 'filter success': (r) => r.status === 200 });

        } else {
            // [시나리오 3] 딥 페이징 (SHOES 카테고리의 50페이지 뒤 데이터 조회)
            // OFFSET 성능 저하가 가장 잘 드러나는 구간
            let res = http.get(`${BASE_URL}?category=SHOES&page=50&size=20`);
            check(res, { 'deep paging success': (r) => r.status === 200 });
        }
    });

    sleep(0.5);
}