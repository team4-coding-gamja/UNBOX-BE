# ECR Module

이 모듈은 AWS Elastic Container Registry (ECR) 리포지토리를 생성하고 관리합니다.

## 기능

- ECR 리포지토리 생성
- 이미지 스캔 설정
- 라이프사이클 정책 적용
- 리포지토리 권한 관리
- 암호화 설정

## 사용법

```hcl
module "ecr" {
  source = "../../modules/ecr"
  
  repository_names = [
    "unbox-core-business",
    "unbox-product-service"
  ]
  
  environment = "dev"
  
  common_tags = {
    Project     = "unbox"
    Environment = "dev"
    ManagedBy   = "terraform"
  }
}
```

## 입력 변수

| 변수명 | 설명 | 타입 | 기본값 | 필수 |
|--------|------|------|--------|------|
| repository_names | 생성할 ECR 리포지토리 이름 목록 | list(string) | [] | Yes |
| environment | 환경 이름 | string | - | Yes |
| image_tag_mutability | 이미지 태그 변경 가능성 | string | "MUTABLE" | No |
| scan_on_push | 푸시 시 이미지 스캔 여부 | bool | true | No |
| encryption_type | 암호화 타입 | string | "AES256" | No |
| max_image_count | 보관할 최대 이미지 수 | number | 10 | No |
| untagged_image_days | 태그 없는 이미지 보관 일수 | number | 1 | No |

## 출력값

| 출력명 | 설명 |
|--------|------|
| repository_urls | 리포지토리 URL 맵 |
| repository_arns | 리포지토리 ARN 맵 |
| registry_id | 레지스트리 ID |
| repository_names | 생성된 리포지토리 이름 목록 |

## 라이프사이클 정책

- 최신 10개 이미지만 보관
- 태그 없는 이미지는 1일 후 삭제
- latest, v로 시작하는 태그 우선 보관