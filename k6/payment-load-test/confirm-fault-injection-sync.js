import http from "k6/http";
import exec from "k6/execution";
import { check, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";
import { SharedArray } from "k6/data";

const PAYMENT_BASE_URL = (__ENV.PAYMENT_BASE_URL || "http://localhost:8080/payment").replace(/\/$/, "");
const CONFIRM_URL = `${PAYMENT_BASE_URL}/test/api/payment/confirm/sync`;
const AUTH_TOKEN = __ENV.AUTH_TOKEN || "test_token";
const DATA_PATH = __ENV.DATA_PATH || "./data.json";
const TEST_RUN_ID = __ENV.TEST_RUN_ID || "sync-fault-case";
const FAULT_DELAY_MS = String(__ENV.FAULT_DELAY_MS || __ENV.DELAY || "5000");

const testData = new SharedArray("confirm-sync-test-data", function () {
    try {
        const parsed = JSON.parse(open(DATA_PATH));
        if (!Array.isArray(parsed) || parsed.length === 0) {
            throw new Error(`Invalid DATA_PATH. path=${DATA_PATH}`);
        }
        return parsed;
    } catch (e) {
        const dummy = [];
        for (let i = 0; i < 1000; i++) {
            dummy.push({
                paymentId: "00000000-0000-0000-0000-000000000000",
                paymentKey: `test_success_dummy_${i}`,
                amount: 10000,
            });
        }
        return dummy;
    }
});

const confirmRequestsTotal = new Counter("confirm_requests_total");
const confirmSuccessTotal = new Counter("confirm_success_total");
const confirmNon2xxTotal = new Counter("confirm_non2xx_total");
const datasetReuseTotal = new Counter("dataset_reuse_total");
const confirm2xxRate = new Rate("confirm_2xx_rate");

export const options = {
    summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
    scenarios: {
        payment_flow_sync: {
            executor: "ramping-vus",
            stages: [
                { duration: "30s", target: 10 },
                { duration: "10s", target: 30 },
                { duration: "30s", target: 30 },
                { duration: "10s", target: 50 },
                { duration: "30s", target: 50 },
                { duration: "10s", target: 100 },
                { duration: "60s", target: 100 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "10s",
            gracefulStop: "60s",
        },
    },
};

function pickData() {
    const iteration = exec.scenario.iterationInTest;
    if (iteration >= testData.length) {
        datasetReuseTotal.add(1);
        return testData[iteration % testData.length];
    }
    return testData[iteration];
}

function toTestPaymentKey(rawKey, iteration) {
    if (typeof rawKey === "string" && rawKey.startsWith("test_success_")) {
        return rawKey;
    }
    return `test_success_${TEST_RUN_ID}_${iteration}`;
}

export default function () {
    const row = pickData();
    const iteration = exec.scenario.iterationInTest;

    const payload = JSON.stringify({
        paymentId: row.paymentId,
        paymentKey: toTestPaymentKey(row.paymentKey, iteration),
        amount: row.amount,
    });

    const response = http.post(CONFIRM_URL, payload, {
        timeout: "15s",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${AUTH_TOKEN}`,
            "X-Test-Run-Id": TEST_RUN_ID,
            "X-Fault-Target": "order",
            "X-Fault-Delay-MS": FAULT_DELAY_MS,
        },
        tags: {
            scenario_mode: "sync",
        },
    });
    confirmRequestsTotal.add(1);

    const ok = check(response, {
        "confirm 2xx": (r) => r.status >= 200 && r.status < 300,
    });

    confirm2xxRate.add(ok);
    ok ? confirmSuccessTotal.add(1) : confirmNon2xxTotal.add(1);
    sleep(1);
}
