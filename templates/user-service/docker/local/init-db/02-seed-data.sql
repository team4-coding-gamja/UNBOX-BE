-- User Service Seed Data
-- 이 파일은 사용자 관리 서비스의 초기 데이터 삽입용입니다.

-- 개발자가 필요한 초기 데이터를 여기에 추가해주세요

/*
-- 예시: 테스트용 관리자 계정
INSERT INTO users (username, email, password_hash, full_name, status) VALUES
('admin', 'admin@unbox.com', '$2a$10$example_hashed_password', '시스템 관리자', 'ACTIVE'),
('testuser1', 'test1@unbox.com', '$2a$10$example_hashed_password', '테스트 사용자1', 'ACTIVE'),
('testuser2', 'test2@unbox.com', '$2a$10$example_hashed_password', '테스트 사용자2', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- 예시: 관리자 권한 부여
INSERT INTO user_roles (user_id, role_name) 
SELECT id, 'ADMIN' FROM users WHERE username = 'admin'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_name) 
SELECT id, 'USER' FROM users WHERE username IN ('testuser1', 'testuser2')
ON CONFLICT DO NOTHING;

-- 예시: 테스트 사용자 프로필
INSERT INTO user_profiles (user_id, nickname, bio) 
SELECT id, '관리자', '시스템 관리자 계정입니다.' FROM users WHERE username = 'admin'
ON CONFLICT DO NOTHING;
*/

-- TODO: 실제 필요한 초기 사용자 데이터를 여기에 추가해주세요
-- 예: 테스트 계정, 기본 권한, 초기 설정 등
-- 주의: 실제 운영에서는 기본 비밀번호를 반드시 변경하세요!