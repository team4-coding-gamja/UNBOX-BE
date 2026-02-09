import http from 'k6/http';
import { check, sleep, group } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 30 },
        { duration: '2m', target: 30 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<400'],
    },
};

// 🚩 URL을 v2로 변경
const BASE_URL = 'http://localhost:8082/product/api/test/products/v2';

export default function () {
    const rand = Math.random();

    group('V2 No-Offset Performance Test', function () {
        if (rand < 0.5) {
            // [시나리오 1] 일반 전체 목록 조회 (첫 페이지)
            // lastProductId가 없으면 가장 최신 데이터부터 조회
            let res = http.get(`${BASE_URL}?size=20`);
            check(res, { 'status is 200': (r) => r.status === 200 });

        } else if (rand < 0.8) {
            // [시나리오 2] 카테고리 필터링 (SHOES)
            let res = http.get(`${BASE_URL}?category=SHOES&size=20`);
            check(res, { 'filter success': (r) => r.status === 200 });

        } else {
            // [시나리오 3] 무한 스크롤 시뮬레이션 (딥 페이징 대응)
            // 1. 먼저 첫 페이지를 호출하여 마지막 ID를 획득
            let res1 = http.get(`${BASE_URL}?category=SHOES&size=20`);

            // 2. 응답 데이터에서 마지막 상품의 ID 추출 (Slice/List 구조에 따라 추출 로직 확인 필요)
            const products = res1.json().data.content; // Slice 사용 시 .content에 데이터가 있음

            if (products && products.length > 0) {
                const lastId = products[products.length - 1].productId;

                // 3. 획득한 ID를 lastProductId로 넘겨서 "다음 페이지" 조회
                // V1에서 page=50으로 발생했던 부하를 이 방식이 얼마나 효율적으로 처리하는지 확인
                let res2 = http.get(`${BASE_URL}?category=SHOES&lastProductId=${lastId}&size=20`);
                check(res2, { 'no-offset success': (r) => r.status === 200 });
            }
        }
    });

    sleep(0.5);
}