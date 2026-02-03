import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// JWT 토큰
const TOKEN = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJhZG1pbklkIjoxLCJlbWFpbCI6Im1hc3RlckB1bmJveC5jb20iLCJyb2xlIjoiUk9MRV9NQVNURVIiLCJpYXQiOjE3NjgyNjc5NTAsImV4cCI6MTc2ODI3MTU1MH0.INKz8H_do9cXXB7Ecam4rdB7Buu-ssxenMq_JMEunQo';
// 메트릭 정의
const productListDuration = new Trend('product_list_duration');
const errorRate = new Rate('error_rate');

// 테스트 설정
export const options = {
    scenarios: {
        // 1. 평상시 트래픽 (Baseline)
        baseline_traffic: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
        },
        // 2. 부하 테스트 (Load Test) - 점진적 증가
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },  // 30초 동안 50명까지 증가
                { duration: '1m', target: 50 },   // 1분간 50명 유지
                { duration: '30s', target: 0 },   // 30초 동안 0명으로 감소
            ],
            startTime: '30s', // baseline 이후 실행
        },
        // 3. 스트레스 테스트 (Stress Test) - 한계점 확인
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 200 },  // 1분 동안 200명까지 급증
                { duration: '2m', target: 200 },  // 2분간 200명 유지
                { duration: '1m', target: 0 },    // 1분 동안 0명으로 감소
            ],
            startTime: '2m30s', // load_test 이후 실행
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이내여야 함
        error_rate: ['rate<0.01'],        // 에러율이 1% 미만이어야 함
    },
};

const BASE_URL = 'http://app:8080/api/products';

export default function () {
    // 랜덤한 페이지와 검색어로 조회 시뮬레이션
    const page = Math.floor(Math.random() * 20000); // 0~4 페이지 랜덤
    const categories = ['SHOES', '']; // 카테고리 랜덤 (SHOES 또는 전체)
    const category = categories[Math.floor(Math.random() * categories.length)];

    const params = {
        headers: {
            'Authorization': TOKEN,
            'Content-Type': 'application/json',
        },
        tags: {
            name: 'GetProductList',
        },
    };

    // 쿼리 파라미터 구성
    let queryParams = `?page=${page}&size=20`;
    if (category) queryParams += `&category=${category}`;

    const res = http.get(`${BASE_URL}${queryParams}`, params);

    // 메트릭 수집
    productListDuration.add(res.timings.duration);

    // 검증
    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    if (!success) {
        errorRate.add(1);
    }

    sleep(1); // 1초 대기 (사용자 행동 시뮬레이션)
}