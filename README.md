# 📦 UNBOX

> **안전하고 신뢰할 수 있는 한정판 C2C 거래 플랫폼, UNBOX** 
> Team 4. 코딩감자

🚀 **Latest Deployment Trigger**: January 8, 2026

<br>

## 🛠️ Tech Stack

### Backend
<img src="https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=Java&logoColor=white"/> <img src="https://img.shields.io/badge/Spring%20Boot-3.5.9-6DB33F?style=flat-square&logo=Spring%20Boot&logoColor=white"/> <img src="https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=Spring&logoColor=white"/> <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=Spring%20Security&logoColor=white"/> <img src="https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=Gradle&logoColor=white"/>

### Database & Infra
<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=PostgreSQL&logoColor=white"/> <img src="https://img.shields.io/badge/H2-Database-blue?style=flat-square"/> <img src="https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=Amazon%20AWS&logoColor=white"/>

### Tools
<img src="https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=flat-square&logo=IntelliJ%20IDEA&logoColor=white"/> <img src="https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=GitHub&logoColor=white"/> <img src="https://img.shields.io/badge/Postman-FF6C37?style=flat-square&logo=Postman&logoColor=white"/>

<br>

## 💡 Key Features

UNBOX는 구매자와 판매자 간의 투명한 거래와 철저한 검수 시스템 제공

- **User**: 회원가입/로그인 (JWT 인증), 마이페이지
- **Admin**: 상품 등록, 검수 관리, 회원 관리
- **Product**: 상품 검색, 필터링, 상세 조회
- **Trade**: 즉시 구매/판매 (추후: 구매 입찰, 판매 입찰)
- **Order & Inspection**: 주문 생성, 전문가 검수(합격/불합격 판정), 배송 추적
- **Review**: 리뷰 작성, 조회

<br>

## 🗂️ ERD (Entity Relationship Diagram)

> 프로젝트의 DB 설계 구조

![ERD](https://github.com/user-attachments/assets/d4407808-a0b0-4f8a-a015-42860dd50bd8)

<br>

## 🧑‍💻 Team Members

|   이름    | 역할 | 담당 파트                 | GitHub |
|:-------:| :---: |:----------------------| :---: |
| **노준석** | 팀장 | `Order`, `Inspection` | [@RJ-Stony](https://github.com/RJ-Stony) |
| **김낙균** | 팀원 | `Trade`               | [@nacgyun](https://github.com/nacgyun) |
| **김현준** | 팀원 | `Product`             | [@hjun813](https://github.com/hjun813) |
| **송가현** | 팀원 | `Infra`, `Review`     | [@GahyunSongDev](https://github.com/GahyunSongDev) |
| **장경준** | 팀원 | `User`, `Auth`        | [@GyeongJoon](https://github.com/GyeongJoon) |

<br>

## 📜 Conventions

### GitHub Flow Strategy
- **`main`**: 배포 가능한 상태의 브랜치
- **`develop`**: 개발 중인 코드가 합쳐지는 브랜치
- **`feat/*`**: 새로운 기능 개발 (`feat/order-entity`)
- **`fix/*`**: 버그 수정

### Commit Message
```text
type : 제목 (50자 이내)

- 본문 내용 (구체적인 변경 사항)
- 여러 줄 작성 가능

Close #이슈번호