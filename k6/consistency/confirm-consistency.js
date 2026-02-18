import http from "k6/http";
import exec from "k6/execution";
import { check, sleep } from "k6";
import { Counter, Rate } from "k6/metrics";
import { SharedArray } from "k6/data";

const MODE = (__ENV.MODE || "outbox").toLowerCase();
const PAYMENT_BASE_URL = (__ENV.PAYMENT_BASE_URL || "http://localhost:8085/payment").replace(/\/$/, "");
const CONFIRM_URL = `${PAYMENT_BASE_URL}/api/payment/confirm`;
const AUTH_TOKEN = __ENV.AUTH_TOKEN || "";
const DATA_PATH = __ENV.DATA_PATH || "../data.json";
const TEST_RUN_ID = __ENV.TEST_RUN_ID || "manual";

const RATE = Number(__ENV.RATE || 10);
const WARMUP_SECONDS = Number(__ENV.WARMUP_SECONDS || 10);
const STEADY_SECONDS = Number(__ENV.STEADY_SECONDS || 40);
const COOLDOWN_SECONDS = Number(__ENV.COOLDOWN_SECONDS || 10);
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || 50);
const MAX_VUS = Number(__ENV.MAX_VUS || 200);

const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || "10s";
const THINK_TIME_MS = Number(__ENV.THINK_TIME_MS || 0);

if (!["async", "outbox"].includes(MODE)) {
  throw new Error(`MODE must be one of: async | outbox. got=${MODE}`);
}

if (!AUTH_TOKEN) {
  throw new Error("AUTH_TOKEN is required.");
}

const testData = new SharedArray("confirm-test-data", function () {
  const parsed = JSON.parse(open(DATA_PATH));
  if (!Array.isArray(parsed) || parsed.length === 0) {
    throw new Error(`Invalid DATA_PATH. expected non-empty array. path=${DATA_PATH}`);
  }
  return parsed;
});

const confirmRequestsTotal = new Counter("confirm_requests_total");
const confirmSuccessTotal = new Counter("confirm_success_total");
const confirmNon2xxTotal = new Counter("confirm_non2xx_total");
const datasetReuseTotal = new Counter("dataset_reuse_total");
const confirm2xxRate = new Rate("confirm_2xx_rate");

export const options = {
  scenarios: {
    confirm_consistency: {
      executor: "ramping-arrival-rate",
      startRate: RATE,
      timeUnit: "1s",
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      stages: [
        { target: RATE, duration: `${WARMUP_SECONDS}s` },
        { target: RATE, duration: `${STEADY_SECONDS}s` },
        { target: 0, duration: `${COOLDOWN_SECONDS}s` },
      ],
    },
  },
};

function pickData() {
  const iteration = exec.scenario.iterationInTest;
  if (iteration >= testData.length) {
    datasetReuseTotal.add(1);
  }
  return testData[iteration % testData.length];
}

export default function () {
  const row = pickData();
  const payload = JSON.stringify({
    paymentId: row.paymentId,
    paymentKey: row.paymentKey,
    amount: row.amount,
  });

  confirmRequestsTotal.add(1);

  const response = http.post(CONFIRM_URL, payload, {
    timeout: REQUEST_TIMEOUT,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${AUTH_TOKEN}`,
      "X-Test-Mode": MODE,
      "X-Test-Run-Id": TEST_RUN_ID,
    },
    tags: {
      api: "payment_confirm",
      mode: MODE,
      test_run_id: TEST_RUN_ID,
    },
  });

  const ok = check(response, {
    "confirm 2xx": (r) => r.status >= 200 && r.status < 300,
  });

  confirm2xxRate.add(ok);

  if (ok) {
    confirmSuccessTotal.add(1);
  } else {
    confirmNon2xxTotal.add(1);
    console.error(
      `[confirm-fail] mode=${MODE} status=${response.status} paymentId=${row.paymentId} body=${String(
        response.body || "",
      ).slice(0, 200)}`,
    );
  }

  if (THINK_TIME_MS > 0) {
    sleep(THINK_TIME_MS / 1000);
  }
}
