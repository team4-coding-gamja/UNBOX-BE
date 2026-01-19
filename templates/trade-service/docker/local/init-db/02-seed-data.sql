-- Trade Service Seed Data
-- 이 파일은 거래 관리 서비스의 초기 데이터 삽입용입니다.

-- 개발자가 필요한 초기 데이터를 여기에 추가해주세요

/*
-- 예시: 테스트 거래 데이터
INSERT INTO trades (seller_id, buyer_id, product_id, trade_type, status, price, quantity, trade_location, description) VALUES
(1, 2, 1, 'SELL', 'PENDING', 250000.00, 1, '서울시 강남구', '깨끗한 상태의 중고 스마트폰입니다.'),
(2, 3, 2, 'SELL', 'CONFIRMED', 1100000.00, 1, '서울시 서초구', '거의 새것 같은 노트북입니다.'),
(3, 1, 1, 'EXCHANGE', 'COMPLETED', 0.00, 1, '서울시 마포구', '물물교환으로 진행된 거래입니다.')
ON CONFLICT DO NOTHING;

-- 예시: 테스트 거래 메시지
INSERT INTO trade_messages (trade_id, sender_id, message, message_type) VALUES
(1, 2, '안녕하세요! 이 상품 구매하고 싶습니다.', 'TEXT'),
(1, 1, '네, 안녕하세요! 언제 거래 가능하신가요?', 'TEXT'),
(1, 2, '내일 오후 2시 어떠세요?', 'TEXT')
ON CONFLICT DO NOTHING;

-- 예시: 테스트 거래 리뷰
INSERT INTO trade_reviews (trade_id, reviewer_id, reviewee_id, rating, comment) VALUES
(3, 1, 3, 5, '매우 친절하고 상품 상태도 좋았습니다!'),
(3, 3, 1, 4, '약속 시간을 잘 지켜주셨어요.')
ON CONFLICT DO NOTHING;

-- 예시: 거래 상태 이력
INSERT INTO trade_status_history (trade_id, previous_status, new_status, changed_by, reason) VALUES
(1, NULL, 'PENDING', 1, '거래 생성'),
(2, 'PENDING', 'CONFIRMED', 2, '구매자가 거래 확정'),
(3, 'CONFIRMED', 'COMPLETED', 1, '거래 완료 확인')
ON CONFLICT DO NOTHING;
*/

-- TODO: 실제 필요한 초기 거래 데이터를 여기에 추가해주세요
-- 예: 테스트 거래, 샘플 메시지, 기본 설정 등