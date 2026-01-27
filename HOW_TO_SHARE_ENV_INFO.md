# 📤 백엔드 개발자에게 환경 정보 전달하는 방법

> 새로운 백엔드 개발자가 합류했을 때 필요한 정보를 안전하게 전달하는 가이드

---

## 📋 전달할 문서 목록

### 1. 공개 문서 (Git에 포함)
이 문서들은 이미 Git에 커밋되어 있으므로 Repository 접근 권한만 주면 됩니다.

- ✅ [BACKEND_ONBOARDING.md](./BACKEND_ONBOARDING.md) - 온보딩 메인 가이드
- ✅ [BACKEND_CHECKLIST.md](./BACKEND_CHECKLIST.md) - 온보딩 체크리스트
- ✅ [README.md](./README.md) - 프로젝트 개요

### 2. 비공개 문서 (별도 전달 필요)
민감한 정보가 포함되어 있어 Git에 커밋되지 않는 문서입니다.

- 🔒 **DEV_ENVIRONMENT_INFO.md** - Dev 환경 접근 정보
- 🔒 **AWS Access Key** - AWS CLI 접근용
- 🔒 **SSH Key** - Bastion Host 접근용 (선택)

---

## 🔐 비공개 정보 생성 및 전달

### 1단계: 환경 정보 문서 생성

```bash
# Dev 환경 정보 생성
./scripts/generate-env-info.sh dev

# Prod 환경 정보 생성 (필요시)
./scripts/generate-env-info.sh prod
```

생성된 파일:
- `DEV_ENVIRONMENT_INFO.md` (또는 `PROD_ENVIRONMENT_INFO.md`)

### 2단계: AWS IAM 사용자 생성

#### AWS Console에서 생성

1. AWS Console → IAM → Users → "Add users"
2. User name: `unbox-backend-developer-{이름}`
3. Access type: "Programmatic access" 체크
4. Permissions: 다음 그룹에 추가
   - `unbox-dev-developers` (기존 그룹)
   - 또는 아래 정책 직접 연결

#### 필요한 권한 (Policy)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:DescribeRepositories",
        "ecr:ListImages"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecs:DescribeServices",
        "ecs:DescribeTasks",
        "ecs:DescribeTaskDefinition",
        "ecs:ListTasks",
        "ecs:DescribeClusters"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:DescribeLogGroups",
        "logs:DescribeLogStreams",
        "logs:GetLogEvents",
        "logs:FilterLogEvents",
        "logs:StartQuery",
        "logs:GetQueryResults"
      ],
      "Resource": "arn:aws:logs:ap-northeast-2:632941626317:log-group:/ecs/unbox-*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:ap-northeast-2:632941626317:parameter/unbox/dev/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "rds:DescribeDBInstances"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "elasticache:DescribeCacheClusters"
      ],
      "Resource": "*"
    }
  ]
}
```

#### Access Key 생성

1. 사용자 생성 완료 후 "Download .csv" 클릭
2. Access Key ID와 Secret Access Key 저장
3. **주의**: Secret Key는 이 시점에만 확인 가능!

### 3단계: SSH 키 준비 (선택사항)

Bastion Host 접속이 필요한 경우에만 제공합니다.

```bash
# 기존 SSH 키 복사
cp ~/.ssh/unbox-bastion-aws.pem ~/Desktop/unbox-bastion-aws.pem

# 또는 새 키 페어 생성
aws ec2 create-key-pair \
  --key-name unbox-bastion-temp \
  --query 'KeyMaterial' \
  --output text > ~/Desktop/unbox-bastion-aws.pem
```

---

## 📨 안전한 전달 방법

### 방법 1: 1Password / LastPass (권장)

1. 1Password에 새 항목 생성
2. 다음 정보 입력:
   - AWS Access Key ID
   - AWS Secret Access Key
   - DEV_ENVIRONMENT_INFO.md 내용 (Secure Note)
   - SSH Key (파일 첨부)
3. 해당 항목을 팀원과 공유

### 방법 2: 암호화된 파일 전달

```bash
# 1. 정보를 하나의 파일로 묶기
cat > onboarding-package.txt << EOF
=== AWS Credentials ===
Access Key ID: AKIA...
Secret Access Key: ...

=== SSH Key ===
$(cat ~/Desktop/unbox-bastion-aws.pem)

=== Environment Info ===
$(cat DEV_ENVIRONMENT_INFO.md)
EOF

# 2. 파일 암호화 (macOS/Linux)
openssl enc -aes-256-cbc -salt -in onboarding-package.txt -out onboarding-package.enc

# 3. 암호화된 파일 전달 (Slack DM, Email 등)
# 4. 비밀번호는 별도 채널로 전달 (전화, 문자 등)

# 수신자가 복호화하는 방법:
openssl enc -aes-256-cbc -d -in onboarding-package.enc -out onboarding-package.txt
```

### 방법 3: AWS Secrets Manager (고급)

```bash
# 1. Secret 생성
aws secretsmanager create-secret \
  --name unbox/onboarding/developer-name \
  --description "Onboarding info for new developer" \
  --secret-string file://onboarding-package.txt \
  --region ap-northeast-2

# 2. 개발자에게 Secret ARN 전달
# 3. 개발자가 직접 조회
aws secretsmanager get-secret-value \
  --secret-id unbox/onboarding/developer-name \
  --region ap-northeast-2 \
  --query SecretString \
  --output text
```

---

## 📧 전달 메시지 템플릿

### Discord 메시지

```

개발 환경 설정을 위해 다음 단계를 진행해주세요:

1️⃣ GitHub Repository 접근
- Repository: https://github.com/team4-coding-gamja/UNBOX-BE
- 초대 이메일을 확인하고 수락해주세요

2️⃣ 온보딩 문서 확인
- BACKEND_ONBOARDING.md - 메인 가이드
- BACKEND_CHECKLIST.md - 체크리스트

3️⃣ AWS 접근 정보 (1Password 공유)
- 1Password 항목: "Unbox - {이름} - Dev Access"
- 포함 내용: AWS Credentials, SSH Key, 환경 정보


```

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📚 온보딩 문서
- GitHub Repository: https://github.com/team4-coding-gamja/UNBOX-BE
- 메인 가이드: BACKEND_ONBOARDING.md
- 체크리스트: BACKEND_CHECKLIST.md

🔐 접근 정보
- 1Password 공유 항목: "Unbox - {이름} - Dev Access"
  (별도 초대 이메일을 확인해주세요)

포함 내용:
✓ AWS Access Key & Secret Key
✓ SSH Key (Bastion Host 접속용)
✓ Dev 환경 상세 정보 (URL, DB 접속 등)


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

환경 설정 중 문제가 발생하면 언제든지 연락주세요!

감사합니다.
```

---

## ✅ 전달 체크리스트

온보딩 정보를 전달하기 전에 확인하세요:

- [ ] GitHub Repository 초대 완료
- [ ] AWS IAM 사용자 생성 및 Access Key 발급
- [ ] DEV_ENVIRONMENT_INFO.md 생성 완료
- [ ] SSH Key 준비 (필요시)
- [ ] 1Password 또는 안전한 방법으로 정보 공유
- [ ] Discord 서버 초대
- [ ] 온보딩 미팅 일정 조율
- [ ] 멘토 배정 (선택)

---

## 🔄 정보 업데이트

환경 정보가 변경되었을 때:

```bash
# 1. 최신 정보로 문서 재생성
./scripts/generate-env-info.sh dev

# 2. 팀원들에게 업데이트 공지
# Discord #dev-backend 채널에 공지

# 3. 1Password 항목 업데이트 (필요시)
```

---

## 🚨 보안 사고 발생 시

Access Key가 유출되었거나 의심되는 경우:

```bash
# 1. 즉시 Access Key 비활성화
aws iam update-access-key \
  --access-key-id AKIA... \
  --status Inactive \
  --user-name unbox-backend-developer-name

# 2. 새 Access Key 발급
aws iam create-access-key \
  --user-name unbox-backend-developer-name

# 3. 팀원에게 새 키 전달
# 4. 이전 키 삭제
aws iam delete-access-key \
  --access-key-id AKIA... \
  --user-name unbox-backend-developer-name
```

---

