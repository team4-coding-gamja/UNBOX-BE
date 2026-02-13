# Dev Seed SQL (No DELETE)

기존 seed 파일을 건드리지 않고, dev DB에만 안전하게 추가/갱신하기 위한 스크립트입니다.

## Files

- `payment-seed-no-delete.sql`: `p_payment`, `p_pg_transaction`
- `order-seed-no-delete.sql`: `p_orders`

## How to use

1. `payment-seed-no-delete.sql`과 `order-seed-no-delete.sql`의 `seed_start`, `seed_end`를 같은 범위로 맞춥니다.
2. Order DB에 `order-seed-no-delete.sql` 실행
3. Payment DB에 `payment-seed-no-delete.sql` 실행
4. 검증 쿼리 결과가 기대 건수인지 확인

## Notes

- `DELETE`는 없습니다.
- 지정 범위의 seed만 `UPSERT`합니다.
- `payment_key = seed_ready_test_success_<gs>` 규칙을 사용하므로, 기존 서비스 로직(테스트 강제 승인 경로)과 맞습니다.
