#!/bin/bash

# UNBOX 서비스 CI/CD 파일 생성 스크립트
# 사용법: ./scripts/generate-service-cicd.sh <service-name> <port>
# 예시: ./scripts/generate-service-cicd.sh user 8081

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 인자 확인
if [ $# -ne 2 ]; then
    echo -e "${RED}❌ 사용법: $0 <service-name> <port>${NC}"
    echo -e "${YELLOW}예시: $0 user 8081${NC}"
    exit 1
fi

SERVICE_NAME=$1
PORT=$2

# 첫 글자 대문자로 변환 (zsh/bash 호환)
SERVICE_NAME_CAPITALIZED=$(echo "$SERVICE_NAME" | awk '{print toupper(substr($0,1,1)) tolower(substr($0,2))}')

echo -e "${GREEN}🚀 $SERVICE_NAME 서비스 CI/CD 파일 생성 시작...${NC}"
echo ""

# Product 서비스 파일들을 템플릿으로 사용
TEMPLATE_SERVICE="product"
TEMPLATE_PORT="8082"

# 1. Task Definition (Dev)
echo -e "${YELLOW}📝 1/7: Dev Task Definition 생성 중...${NC}"
sed -e "s/${TEMPLATE_SERVICE}/${SERVICE_NAME}/g" \
    -e "s/${TEMPLATE_PORT}/${PORT}/g" \
    -e "s/Product/${SERVICE_NAME_CAPITALIZED}/g" \
    task-definitions/dev-${TEMPLATE_SERVICE}-service.json > task-definitions/dev-${SERVICE_NAME}-service.json
echo -e "${GREEN}✅ task-definitions/dev-${SERVICE_NAME}-service.json${NC}"

# 2. Task Definition (Prod)
echo -e "${YELLOW}📝 2/7: Prod Task Definition 생성 중...${NC}"
sed -e "s/${TEMPLATE_SERVICE}/${SERVICE_NAME}/g" \
    -e "s/${TEMPLATE_PORT}/${PORT}/g" \
    -e "s/Product/${SERVICE_NAME_CAPITALIZED}/g" \
    task-definitions/prod-${TEMPLATE_SERVICE}-service.json > task-definitions/prod-${SERVICE_NAME}-service.json
echo -e "${GREEN}✅ task-definitions/prod-${SERVICE_NAME}-service.json${NC}"

# 3. AppSpec (Prod)
echo -e "${YELLOW}📝 3/7: Prod AppSpec 생성 중...${NC}"
sed -e "s/${TEMPLATE_SERVICE}/${SERVICE_NAME}/g" \
    -e "s/${TEMPLATE_PORT}/${PORT}/g" \
    -e "s/Product/${SERVICE_NAME_CAPITALIZED}/g" \
    appspecs/prod-${TEMPLATE_SERVICE}-service.yaml > appspecs/prod-${SERVICE_NAME}-service.yaml
echo -e "${GREEN}✅ appspecs/prod-${SERVICE_NAME}-service.yaml${NC}"

# 4. Dev CI Workflow
echo -e "${YELLOW}📝 4/7: Dev CI Workflow 생성 중...${NC}"
sed -e "s/${TEMPLATE_SERVICE}/${SERVICE_NAME}/g" \
    -e "s/${TEMPLATE_PORT}/${PORT}/g" \
    -e "s/Product/${SERVICE_NAME_CAPITALIZED}/g" \
    .github/workflows/${TEMPLATE_SERVICE}-dev-ci.yml > .github/workflows/${SERVICE_NAME}-dev-ci.yml
echo -e "${GREEN}✅ .github/workflows/${SERVICE_NAME}-dev-ci.yml${NC}"

# 5. Dev CD Workflow
echo -e "${YELLOW}📝 5/7: Dev CD Workflow 생성 중...${NC}"
sed -e "s/${TEMPLATE_SERVICE}/${SERVICE_NAME}/g" \
    -e "s/${TEMPLATE_PORT}/${PORT}/g" \
    -e "s/Product/${SERVICE_NAME_CAPITALIZED}/g" \
    .github/workflows/${TEMPLATE_SERVICE}-dev-cd.yml > .github/workflows/${SERVICE_NAME}-dev-cd.yml
echo -e "${GREEN}✅ .github/workflows/${SERVICE_NAME}-dev-cd.yml${NC}"

# 6. Prod CI Workflow
echo -e "${YELLOW}📝 6/7: Prod CI Workflow 생성 중...${NC}"
sed -e "s/${TEMPLATE_SERVICE}/${SERVICE_NAME}/g" \
    -e "s/${TEMPLATE_PORT}/${PORT}/g" \
    -e "s/Product/${SERVICE_NAME_CAPITALIZED}/g" \
    .github/workflows/${TEMPLATE_SERVICE}-prod-ci.yml > .github/workflows/${SERVICE_NAME}-prod-ci.yml
echo -e "${GREEN}✅ .github/workflows/${SERVICE_NAME}-prod-ci.yml${NC}"

# 7. Prod CD Workflow
echo -e "${YELLOW}📝 7/7: Prod CD Workflow 생성 중...${NC}"
sed -e "s/${TEMPLATE_SERVICE}/${SERVICE_NAME}/g" \
    -e "s/${TEMPLATE_PORT}/${PORT}/g" \
    -e "s/Product/${SERVICE_NAME_CAPITALIZED}/g" \
    .github/workflows/${TEMPLATE_SERVICE}-prod-cd.yml > .github/workflows/${SERVICE_NAME}-prod-cd.yml
echo -e "${GREEN}✅ .github/workflows/${SERVICE_NAME}-prod-cd.yml${NC}"

echo ""
echo -e "${GREEN}🎉 $SERVICE_NAME 서비스 CI/CD 파일 생성 완료!${NC}"
echo ""
echo -e "${YELLOW}생성된 파일 목록:${NC}"
echo "  - task-definitions/dev-${SERVICE_NAME}-service.json"
echo "  - task-definitions/prod-${SERVICE_NAME}-service.json"
echo "  - appspecs/prod-${SERVICE_NAME}-service.yaml"
echo "  - .github/workflows/${SERVICE_NAME}-dev-ci.yml"
echo "  - .github/workflows/${SERVICE_NAME}-dev-cd.yml"
echo "  - .github/workflows/${SERVICE_NAME}-prod-ci.yml"
echo "  - .github/workflows/${SERVICE_NAME}-prod-cd.yml"
echo ""
echo -e "${YELLOW}💡 다음 단계:${NC}"
echo "  1. 생성된 파일들을 확인하세요"
echo "  2. 필요시 추가 수정하세요"
echo "  3. git add . && git commit -m \"feat(cicd): add ${SERVICE_NAME} service CI/CD\""
echo ""
