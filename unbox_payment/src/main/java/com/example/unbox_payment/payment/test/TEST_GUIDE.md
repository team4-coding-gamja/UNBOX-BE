# 📌 상태 기반 관리자 조회 + 깊은 페이지 성능 테스트 가이드

이 문서는 관리자 결제 목록 조회 시 발생하는 성능 저하 문제를 해결하기 위해 **복합 인덱스**와 **커버링 인덱스**를 비교 분석하는 테스트 절차를 안내합니다.

---

## 1️⃣ 사전 준비 (데이터 생성)

테스트를 위해 대량의 데이터(100만 건 이상)가 필요합니다.
아래 API를 호출하여 데이터를 생성하세요.

**API 호출 (Postman 또는 cURL):**
```bash
POST /api/admin/payment/test/init-data?count=100000
```
* 한 번에 10만 건씩 생성하는 것을 권장합니다 (타임아웃 방지).
* 총 10번 호출하여 100만 건을 만드세요.

---

## 2️⃣ 테스트 시나리오 및 인덱스 설정

### 📊 테스트 쿼리
```sql
SELECT payment_id, order_id, buyer_id, amount, status, created_at
FROM p_payment
WHERE status = 'DONE'
ORDER BY created_at DESC, payment_id DESC
OFFSET :offset
LIMIT 20;
```

### ✅ Case A: 복합 인덱스 (기존 방식)
DB 클라이언트 (DBeaver, pgAdmin 등)에서 아래 SQL을 실행하세요:
```sql
-- 기존 인덱스 제거 (있다면)
DROP INDEX IF EXISTS idx_pay_status_created_id;
DROP INDEX IF EXISTS idx_pay_status_created_id_cover;

-- 복합 인덱스 생성
CREATE INDEX idx_pay_status_created_id
ON p_payment (status, created_at DESC, payment_id DESC);
```

### ✅ Case B: 커버링 인덱스 (최적화 방식)
Case A 테스트 완료 후, 아래 SQL로 인덱스를 변경하세요:
```sql
-- 복합 인덱스 제거
DROP INDEX IF EXISTS idx_pay_status_created_id;

-- 커버링 인덱스 생성
CREATE INDEX idx_pay_status_created_id_cover
ON p_payment (status, created_at DESC, payment_id DESC)
INCLUDE (order_id, buyer_id, amount);
```

---

## 3️⃣ 성능 측정 방법

아래 API를 호출하여 `EXPLAIN ANALYZE` 결과를 확인하세요.

**API 호출:**
```bash
GET /api/admin/payment/test/explain?offset={OFFSET}&limit=20
```

**테스트 케이스:**
1. **1페이지 (OFFSET 0)**
   `GET /api/admin/payment/test/explain?offset=0`
2. **중간 페이지 (OFFSET 5,000)**
   `GET /api/admin/payment/test/explain?offset=5000`
3. **깊은 페이지 (OFFSET 200,000)**
   `GET /api/admin/payment/test/explain?offset=200000`

---

## 4️⃣ 결과 분석 포인트

응답 결과(JSON)에서 아래 항목을 비교하세요:

1. **Execution Time**: 쿼리 실행 시간 (ms)
2. **Heap Fetches**: 테이블 데이터 접근 횟수 (Case B에서 0이어야 함)
3. **Index Scan vs Index Only Scan**: 실행 계획 확인

이 테스트 결과를 바탕으로 "깊은 페이지네이션 성능 개선" 경험을 이력서에 기술할 수 있습니다.
