---
name: "✨ Feature Request"
about: 새로운 기능이나 API 개발 필요할 때 사용
title: "[FEAT]"
labels: enhancement
assignees: ''

---

---
name: ✨ Feature Request
about: 새로운 기능이나 API 개발 필요할 때 사용
title: "[FEAT] "
labels: enhancement
assignees: ''

---

## 📝 기능 설명
- 예: 사용자 회원가입 API 구현

## ✅ 개발 조건
- [ ] POST /api/v1/users/signup 요청 시 DB에 저장되어야 함
- [ ] 비밀번호는 BCrypt로 암호화되어야 함
- [ ] 이메일 중복 시 409 Conflict 반환

## 🛠️ 기술적 고려사항
- User 테이블에 `provider` 컬럼 추가 필요
