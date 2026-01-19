-- Core Business Service Seed Data
-- 이 파일은 초기 데이터 삽입용입니다.

-- 개발자가 필요한 초기 데이터를 여기에 추가해주세요

/*
-- 예시: 시스템 설정 초기 데이터
INSERT INTO system_config (config_key, config_value, description) VALUES
('app.name', 'UNBOX Core Business', '애플리케이션 이름'),
('app.version', '1.0.0', '애플리케이션 버전'),
('maintenance.mode', 'false', '점검 모드 여부')
ON CONFLICT (config_key) DO NOTHING;

-- 예시: API 라우팅 초기 설정
INSERT INTO api_routes (service_name, route_path, target_url, is_active) VALUES
('user-service', '/api/users/**', 'http://localhost:8081', true),
('product-service', '/api/products/**', 'http://localhost:8082', true),
('trade-service', '/api/trades/**', 'http://localhost:8083', true),
('order-service', '/api/orders/**', 'http://localhost:8084', true),
('payment-service', '/api/payments/**', 'http://localhost:8085', true)
ON CONFLICT DO NOTHING;
*/

-- TODO: 실제 필요한 초기 데이터를 여기에 추가해주세요
-- 예: 관리자 계정, 기본 설정값, 코드 테이블 등