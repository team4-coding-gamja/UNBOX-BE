// newTest2_improved.js (V2 점수 기반 No-Offset)
import http from 'k6/http';
import { check, sleep, group } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 30 }, // 부하를 더 높여봅니다
        { duration: '2m', target: 30 },
        { duration: '30s', target: 0 },
    ],
};

const BASE_URL = 'http://localhost:8082/product/api/test/products/v2';

export default function () {
    group('V2 Popularity No-Offset Test', function () {
        // [시나리오 1] 첫 페이지 조회 (대부분의 유저, Redis Hit 기대)
        let res1 = http.get(`${BASE_URL}?size=20`);
        check(res1, { 'cache hit status 200': (r) => r.status === 200 });

        const data = res1.json().data;
        if (data.content && data.content.length > 0) {
            const lastItem = data.content[data.content.length - 1];

            // [시나리오 2] 다음 페이지 조회 (무한 스크롤, 점수 기반 커서 사용)
            // lastScore와 lastId를 동시에 넘겨 정확한 위치부터 조회
            let res2 = http.get(`${BASE_URL}?lastScore=${lastItem.popularityScore}&lastId=${lastItem.productId}&size=20`);
            check(res2, { 'no-offset paging success': (r) => r.status === 200 });
        }
    });
    sleep(0.5);
}