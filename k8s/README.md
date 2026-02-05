## 🚀 로컬 개발 환경 실행 방법

본 프로젝트는 `k3d`를 이용한 로컬 쿠버네티스 환경에서 작동합니다.

### 1) 사전 요구 사항
* Docker, k3d, helm, helmfile 설치

### 2) 클러스터 및 인프라 기동
```bash
# 1. k3d 클러스터 생성 (이미 있다면 생략)
k3d cluster create unbox-cluster --servers 1 --agents 2 -p "80:80@loadbalancer" -p "443:443@loadbalancer"

k3d kubeconfig merge unbox-cluster --kubeconfig-switch-context --overwrite
#사용자 이름 넣으세요잉
cp /Users/사용자이름/.config/k3d/kubeconfig-unbox-cluster.yaml ~/.kube/config
unset KUBECONFIG
#기본으로 세팅되는 기본 인그레스 삭제
kubectl delete crd httproutes.gateway.networking.k8s.io gateways.gateway.networking.k8s.io gatewayclasses.gateway.networking.k8s.io tcproutes.gateway.networking.k8s.io tlsroutes.gateway.networking.k8s.io udproutes.gateway.networking.k8s.io referencegrants.gateway.networking.k8s.io

# 1. 인증서 생성 도구 설치
brew install step

# 2. 루트 CA 및 발급자 인증서 생성
step certificate create root.linkerd.cluster.local ca.crt ca.key --profile root-ca --no-password --insecure
step certificate create identity.linkerd.cluster.local issuer.crt issuer.key --profile intermediate-ca --not-after 8760h --no-password --insecure --ca ca.crt --ca-key ca.key

# 3. 파일 확장자 변경 (Helmfile v1 문법 적용)
mv helmfile.yaml helmfile.yaml.gotmpl
# 2. Helmfile을 통한 전체 배포 (Kafka, DB, Apps)
# 현재 구조는 Bitnami 정책 변경으로 인해 Apache 공식 이미지를 커스텀 차트로 배포합니다.

#이미지 빌드를 안했다면?
./build-local.sh

# 배포
helmfile sync

#각 서비스 포트포워딩
./connect.sh







## 🏗️ 인프라 설계 특징 (Infrastructure)

### 🔹 Kubernetes 기반 로컬 환경 (k3d)
- 실제 운영 환경(EKS)과의 환경 차이(Configuration Drift)를 최소화하기 위해 `k3d`를 사용합니다.
- `StatefulSet`을 이용해 Kafka의 데이터 영속성을 관리합니다.

### 🔹 Kafka 커스텀 차트 (Apache Kafka 3.9)
- **이슈:** Bitnami Helm Chart의 상용화로 인해 로컬 테스트용 이미지 사용 제한 발생.
- **해결:** `apache/kafka` 공식 이미지를 사용하는 커스텀 Helm Chart를 직접 제작 (`./k8s/infra/kafka`).
- **최적화:** 로컬 싱글 노드 환경에 맞춰 Replication Factor를 `1`로 조정하여 리소스 최적화 및 무한 루프 에러 해결.


## 🔍 디버깅 가이드

### 1) Kafka 메시지 실시간 모니터링 (도청)
주문(Order) 이벤트가 발생하는지 확인하려면 다음 명령어를 사용하세요.
```bash
kubectl exec -it kafka-0 -n unbox-infra -- /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning


# 특정 서비스의 로그를 실시간으로 확인
kubectl logs -f -l app=trade-service -n unbox-app