-- Payment Service Seed Data
-- 이 파일은 결제 처리 서비스의 초기 데이터 삽입용입니다.

-- 개발자가 필요한 초기 데이터를 여기에 추가해주세요

/*
-- 예시: 테스트 결제 데이터
INSERT INTO payments (payment_key, order_id, user_id, amount, payment_method, payment_status, pg_provider, pg_transaction_id, approved_at) VALUES
('test_payment_key_001', 1, 1, 302000.00, 'CARD', 'COMPLETED', 'TOSS', 'toss_txn_001', '2024-01-10 14:30:00'),
('test_payment_key_002', 2, 2, 1249000.00, 'BANK_TRANSFER', 'COMPLETED', 'TOSS', 'toss_txn_002', '2024-01-12 09:15:00'),
('test_payment_key_003', 3, 3, 142500.00, 'CARD', 'PENDING', 'KAKAO', NULL, NULL)
ON CONFLICT (payment_key) DO NOTHING;

-- 예시: 결제 상세 정보
INSERT INTO payment_details (payment_id, card_company, card_number, installment_months, approval_number, receipt_url) VALUES
(1, '신한카드', '1234-****-****-5678', 0, 'APP123456', 'https://receipt.toss.im/test001'),
(3, '국민카드', '9876-****-****-4321', 3, NULL, NULL)
ON CONFLICT DO NOTHING;

-- 예시: 테스트 환불 데이터
INSERT INTO refunds (payment_id, refund_key, refund_amount, refund_reason, refund_status, pg_refund_id, requested_by, approved_by, processed_at) VALUES
(1, 'test_refund_key_001', 50000.00, '부분 환불 요청', 'COMPLETED', 'toss_refund_001', 1, 1, '2024-01-16 10:00:00')
ON CONFLICT (refund_key) DO NOTHING;

-- 예시: 결제 이벤트 로그
INSERT INTO payment_events (payment_id, event_type, event_data, created_by) VALUES
(1, 'PAYMENT_REQUESTED', '{"amount": 302000, "method": "CARD"}', 1),
(1, 'PAYMENT_APPROVED', '{"approval_number": "APP123456", "card_company": "신한카드"}', NULL),
(2, 'PAYMENT_REQUESTED', '{"amount": 1249000, "method": "BANK_TRANSFER"}', 2),
(2, 'PAYMENT_APPROVED', '{"bank_name": "국민은행", "account_number": "****1234"}', NULL),
(3, 'PAYMENT_REQUESTED', '{"amount": 142500, "method": "CARD"}', 3),
(1, 'REFUND_REQUESTED', '{"refund_amount": 50000, "reason": "부분 환불 요청"}', 1)
ON CONFLICT DO NOTHING;
*/

-- TODO: 실제 필요한 초기 결제 데이터를 여기에 추가해주세요
-- 예: 테스트 결제, 샘플 환불 데이터, PG사 설정 등
-- 주의: 실제 결제 정보는 절대 하드코딩하지 마세요!