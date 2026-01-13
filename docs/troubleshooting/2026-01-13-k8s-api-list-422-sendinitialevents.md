# 2026-01-13 Kubernetes API list 422 (sendInitialEvents 파라미터 포함)

## 0) 문서 메타데이터

- 문서 ID: `2026-01-13-k8s-api-list-422-sendinitialevents`
- 작성자: dhkim / Cascade
- 작성일: 2026-01-13
- 상태: 해결
- 영향 범위: 개발(로컬)
- 심각도: 높음(핵심 API 불가)
- 관련 컴포넌트:
  - `Spring Boot`
  - `io.kubernetes:client-java` (OpenAPI 기반)
  - `Kubernetes API Server(k3s)`

---

## 1) 요약

- 한 줄 요약:
  - Kubernetes Java Client가 **list 요청에 `sendInitialEvents` 쿼리 파라미터를 포함**시켜, k3s 환경에서 **422 Unprocessable Entity**로 거절되어 모니터링 API가 실패했다.
- 결론:
  - `sendInitialEvents` 인자를 **`false`가 아니라 `null`로 설정하여 파라미터 자체가 전송되지 않도록** 수정하여 해결.

---

## 2) 증상

- 로컬에서 `./gradlew bootRun` 후 UI/REST 호출 시 K8s 연동 API가 실패
  - 예: `/api/cluster/nodes`, `/api/sessions`
- 로그(발췌)
  - 초기에는 `Bad Request`/`Connection reset` 또는 `ApiException`으로만 보였음
  - HTTPS 전환 후에는 `PKIX path building failed`(SSLHandshake) 발생
  - SSL 우회 후 list 호출 시 422 성격의 실패가 관찰됨

---

## 3) 환경

- 설정(`application.properties`, 민감정보 마스킹)
  - `jhub.k8s.api-server-url=https://192.168.xx.xx:6443`
  - `jhub.k8s.verify-ssl=false` (개발 편의용)
  - `jhub.k8s.namespace=jhub`
  - `jhub.k8s.username-label-key=hub.jupyter.org/username`
  - `jhub.k8s.bearer-token=***`

---

## 4) 재현 방법

1. 로컬 실행

```bash
./gradlew bootRun
```

2. API 호출

```bash
curl -v http://localhost:8080/api/cluster/nodes
curl -v http://localhost:8080/api/sessions
```

---

## 5) 조사 과정(히스토리)

### 5-1. (초기) K8s API 서버 연결 실패

- 현상:
  - `Bad Request`, `Connection reset` 계열 오류
- 조치:
  - `jhub.k8s.api-server-url`을 HTTP에서 HTTPS로 정정

### 5-2. (HTTPS) SSLHandshakeException: PKIX path building failed

- 현상:
  - `javax.net.ssl.SSLHandshakeException: PKIX path building failed`
- 원인:
  - 사설 인증서/CA로 인해 JVM truststore에서 인증서를 신뢰하지 못함
- 임시 조치:
  - 개발 환경에서만 `jhub.k8s.verify-ssl=false`로 우회
- 장기 권장:
  - CA 체인을 truststore에 추가하거나, 클라이언트에 CA를 명시적으로 로드

### 5-3. (핵심) list 요청에 sendInitialEvents 파라미터가 포함되어 422

- 관찰:
  - kubectl로 동일 파라미터를 붙이면 서버가 거절

```bash
kubectl get --raw "/api/v1/namespaces/jhub/pods?sendInitialEvents=true"
# Error: ... sendInitialEvents is forbidden for list
```

- 결론:
  - 일부 Kubernetes API 서버에서는 **단순 list 요청에서 `sendInitialEvents` 파라미터 자체가 금지**
  - 값이 `false`여도 “쿼리 파라미터 존재”로 판단되어 거절될 수 있음

---

## 6) 근본 원인

- `CoreV1Api.listNamespacedPod(...)`, `CoreV1Api.listNode(...)` 호출 시,
  - `sendInitialEvents` 인자를 `Boolean.FALSE`로 넘겨도
  - 서버에는 `sendInitialEvents=false` 같은 형태로 전달될 수 있고,
  - 해당 파라미터를 금지하는 API 서버에서는 422를 반환

---

## 7) 해결 방법(Fix)

### 7-1. 코드 변경 요약

- 모든 list 호출에서 `sendInitialEvents`를 **`null`로 변경**하여 쿼리 파라미터가 전송되지 않도록 수정

### 7-2. 변경 파일

- `src/main/java/com/dhkimxx/jhub_k8s_spring/repository/k8s/KubernetesPodRepository.java`
- `src/main/java/com/dhkimxx/jhub_k8s_spring/repository/k8s/KubernetesNodeRepository.java`

### 7-3. 변경 포인트(개념)

- 기존(문제 가능):
  - `sendInitialEvents = Boolean.FALSE`
- 수정(해결):
  - `sendInitialEvents = null`

추가로, 장애 분석이 항상 가능하도록

- `ApiException`의 `code/responseBody`를 로그 및 예외 메시지에 포함시킴

---

## 8) 검증

- `/api/sessions` 호출 시:
  - 실행 중인 사용자 서버가 없으면 `[]`(빈 리스트)가 정상
- `/api/cluster/nodes` 호출 시:
  - 토큰이 cluster-scope 권한을 갖지 않으면 `403`이 나올 수 있음(별도 RBAC 필요)

---

## 9) 재발 방지

- list 호출은 기본적으로 **watch/streaming 옵션(`sendInitialEvents`)을 사용하지 않도록 가드**
- 클러스터(특히 k3s)별 API 옵션 허용 차이를 문서화
- 최소 권한 원칙에 따라
  - 노드 조회가 필요하면 ClusterRole/ClusterRoleBinding을 명시적으로 준비
  - 노드 조회가 불필요하면 노드 관련 API를 선택적으로 비활성화하는 옵션 제공 고려

---

## 10) 부록

- 부수적으로 `favicon.ico` 404가 반복 로깅될 수 있으나 기능 이슈는 아님.
  - 필요하면 `src/main/resources/static/favicon.ico` 추가로 노이즈 제거 가능
