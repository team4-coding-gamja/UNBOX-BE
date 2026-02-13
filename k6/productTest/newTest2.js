import http from 'k6/http';
import { check, sleep, group } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 30 },
        { duration: '1m', target: 30 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        // No-Offset은 95%가 100ms 이내로 들어오는 것이 목표
        http_req_duration: ['p(95)<100'],
    },
};

const BASE_URL = 'https://dev.un-box.click/product/api/test/products/v2';
// const BASE_URL = 'http://localhost:8082/product/api/test/products/v2';

export default function () {
    const rand = Math.random();

    group('V2 No-Offset Performance Test', function () {
        if (rand < 0.4) {
            // [시나리오 1] 첫 페이지 조회
            let res = http.get(`${BASE_URL}?category=SHOES&size=20`);
            check(res, { 'first page status 200': (r) => r.status === 200 });

        } else {
            // [시나리오 2] 무한 스크롤 시뮬레이션
            // 실제 사용자의 흐름처럼 '다음' 데이터를 호출
            let res1 = http.get(`${BASE_URL}?category=SHOES&size=20`);
            const products = res1.json().data.content;

            if (products && products.length > 0) {
                // 마지막 상품 ID를 기준으로 다음 페이지 호출
                // 이 동작은 데이터가 100만건이라도 Index Seek로 작동하여 매우 빠름
                const lastId = products[products.length - 1].productId;
                let res2 = http.get(`${BASE_URL}?category=SHOES&lastProductId=${lastId}&size=20`);

                check(res2, { 'no-offset success': (r) => r.status === 200 });
            }
        }
    });

    sleep(0.5);
}