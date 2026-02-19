import http from "k6/http";
import { check, sleep } from "k6";
import { Rate } from "k6/metrics";
import exec from "k6/execution";

// ==============================
// 실행 모드 (sync / async)
// ==============================
const MODE = (__ENV.MODE || "").trim().toLowerCase(); // 서버에서 분기용
if (MODE !== "sync" && MODE !== "async") {
    throw new Error(`Invalid MODE='${__ENV.MODE}'. Use -e MODE=sync or -e MODE=async`);
}


// ==============================
// 환경 변수
// ==============================
const PAYMENT_BASE_URL =
    __ENV.PAYMENT_BASE_URL || "http://localhost:8085/payment";
const DATA_PATH = __ENV.DATA_PATH || "./data.json";

// ==============================
// 타임아웃 / 지연 설정
// ==============================
// 클라이언트(사용자) 최대 대기 시간(고정)
const CONFIRM_TIMEOUT = __ENV.CONFIRM_TIMEOUT || "15s";

// Fault Injection: Order 지연(기본 3s)
const ORDER_DELAY_MS = Number(__ENV.ORDER_DELAY_MS || 2500);
const FAULT_TARGET = ((__ENV.FAULT_TARGET !== undefined ? __ENV.FAULT_TARGET : "order") || "").toLowerCase();

// think time
const SLEEP_MS = Number(__ENV.SLEEP_MS || 50);

// ==============================
// API
// ==============================
const CONFIRM_URL = `${PAYMENT_BASE_URL}/api/payment/confirm`;

// ==============================
// 커스텀 메트릭
// ==============================
const failRate = new Rate("confirm_fail_rate");

// ==============================
// 테스트 데이터
// ==============================
const data = JSON.parse(open(DATA_PATH));

// ==============================
// k6 옵션 (ramping-vus)
// ==============================
export const options = {
    scenarios: {
        payment_flow: {
            executor: "ramping-vus",
            stages: [
                { duration: "10s", target: 10 },   // warm-up
                { duration: "10s", target: 50 },    // ramp
                { duration: "20s", target: 50 },   // sustain
                { duration: "10s", target: 100 },    // ramp
                { duration: "20s", target: 100 },   // sustain
                { duration: "10s", target: 0 },    // ramp-down
            ],
            gracefulRampDown: "10s",
        },
    },
    thresholds: {
        // ✅ 실험 목적이면 "통과/실패" 기준은 빡세게 두지 않는 걸 추천
        // 그래도 기존 형태 유지하되, 필요하면 env로 끄고(삭제) 사용해도 됨.
        confirm_fail_rate: ["rate<0.99"], // (실험용) 거의 제한 없음에 가깝게
        http_req_duration: ["p(95)<15000"],
    },
};

// ===============================
// VU/iteration 기반 데이터 분배 (ramping 환경 대응)
// ===============================
function pick(arr) {
    // 전체 테스트 기준 iteration index를 사용해 데이터 중복을 최소화
    const idx = exec.scenario.iterationInTest % arr.length;
    return arr[idx];
}

// ==============================
// 메인 테스트 로직
// ==============================
export default function () {
    const d = pick(data);
    const headers = {
        "Content-Type": "application/json",
        "X-Test-Mode": MODE, // sync / async 분기
        "X-Test-User-ID": String(d.buyerId ?? 1),
    };

    const tags = {
        api: "payment_confirm",
        mode: MODE,
    };

    if (FAULT_TARGET) {
        headers["X-Fault-Target"] = FAULT_TARGET;
        headers["X-Fault-Delay-MS"] = String(ORDER_DELAY_MS);
        tags.fault_target = FAULT_TARGET;
        tags.order_delay_ms = String(ORDER_DELAY_MS);
    }

    const res = http.post(
        CONFIRM_URL,
        JSON.stringify({
            paymentId: d.paymentId,
            paymentKey: d.paymentKey,
            amount: d.amount,
        }),
        {
            timeout: CONFIRM_TIMEOUT,
            headers,
            tags,
        }
    );

    const ok = check(res, {
        "confirm 2xx": (r) => r.status >= 200 && r.status < 300,
    });

    failRate.add(!ok);

    if (!ok) {
        console.error(
            `[FAIL] mode=${MODE} status=${res.status} vu=${exec.vu.idInTest} ` +
            `body=${(res.body || "").slice(0, 200)}`
        );
    }

    sleep(SLEEP_MS / 1000);
}
