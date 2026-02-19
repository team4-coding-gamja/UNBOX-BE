import http from 'k6/http';
import { check, sleep, group } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 30 }, // 유저 50명까지 점진적 증가
        { duration: '1m', target: 30 },  // 50명 유지
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        // 95%의 요청이 500ms 이내여야 함 (Deep Paging 시 실패할 가능성 높음)
        http_req_duration: ['p(95)<500'],
    },
};

const BASE_URL = 'https://dev.un-box.click/product/api/test/products/v1';
// const BASE_URL = 'http://localhost:8082/product/api/test/products/v1';

export default function () {
    const rand = Math.random();

    group('V1 Offset Performance Test', function () {
        if (rand < 0.3) {
            // [시나리오 1] 첫 페이지 조회 (빠름)
            let res = http.get(`${BASE_URL}?category=SHOES&page=0&size=20`);
            check(res, { 'first page status 200': (r) => r.status === 200 });

        } else if (rand < 0.6) {
            // [시나리오 2] 중간 페이지 조회 (약간 느려짐)
            // 10만건 데이터 중 약 절반 지점
            let res = http.get(`${BASE_URL}?category=SHOES&page=2500&size=20`);
            check(res, { 'middle page status 200': (r) => r.status === 200 });

        } else {
            // [시나리오 3] 딥 페이징 (매우 느림)
            // 9만번째 이후 데이터 조회 (Offset 90,000)
            // DB는 9만건을 읽어서 메모리에 올린 후 버려야 함
            let res = http.get(`${BASE_URL}?category=SHOES&page=4500&size=20`);
            check(res, { 'deep paging status 200': (r) => r.status === 200 });
        }
    });

    sleep(0.5); // 부하 밀도를 높이기 위해 sleep 단축
}