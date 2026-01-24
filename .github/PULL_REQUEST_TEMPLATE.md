## 📋 개요
- 개요

## 🔗 관련 이슈
- Issue Closes #

## 🛠 작업 내용
- [ ] `ItemService` 내 상품 검수 로직 추가
- [ ] PostgreSQL `items` 테이블에 `inspection_status` 컬럼 추가 (Flyway/Liquibase)
- [ ] 상품 등록 API (POST /api/v1/items) 응답 스펙 변경

## 📸 스크린샷 (Optional)
- 관련 스크린샷 첨부

## 🧪 테스트 계획
- [ ] **Unit Test**: `ItemServiceTest` 작성 및 통과
- [ ] **Integration Test**: 로컬 환경에서 상품 등록 플로우 전체 테스트 완료
- [ ] **Manual Test**: Swagger를 통해 잘못된 입력값 전송 시 400 에러 확인

## ⚠️ 특이 사항 및 주의점
- 주의점 작성
---

### ✅ 체크리스트
- [ ] 코드가 프로젝트의 스타일 가이드를 따르는가? (Java Convention 등)
- [ ] 불필요한 주석이나 디버깅 로그(System.out.println)를 제거했는가?
- [ ] 새로운 기능에 대한 테스트 코드를 작성했는가?
- [ ] (DB 변경 시) 마이그레이션 스크립트가 포함되었는가?
- [ ] (API 변경 시) Swagger/RestDocs 등 API 문서에 반영되었는가?