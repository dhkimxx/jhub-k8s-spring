# 트러블슈팅 템플릿

## 0) 문서 메타데이터

- 문서 ID: `YYYY-MM-DD-<slug>`
- 작성자:
- 작성일:
- 최종 수정일:
- 상태: 초안 / 진행중 / 해결 / 재발(원인분석중)
- 영향 범위: 개발 / 스테이징 / 운영
- 심각도: 낮음 / 보통 / 높음 / 치명
- 관련 컴포넌트: (예: `Spring Boot`, `Kubernetes Client`, `Ingress`, `DB`)
- 관련 이슈/PR: (링크)

---

## 1) 요약(Executive Summary)

- 한 줄 요약:
- 사용자 영향:
- 결론(해결/미해결):

---

## 2) 증상(Symptoms)

- 관찰된 현상:
- 에러 메시지/스택트레이스(발췌):
- 재현 빈도:
- 최초 발생 시각:

---

## 3) 환경(Environment)

- 실행 환경:
  - OS:
  - JDK:
  - Spring Boot:
  - 빌드/런 명령:
- 인프라/외부 의존:
  - Kubernetes 버전/배포판(k8s/k3s/EKS 등):
  - API Server URL:
  - 네임스페이스:
  - 인증 방식: `kubeconfig` / `bearer token` / 기타
- 설정 값(민감정보 마스킹):
  - 예: `jhub.k8s.api-server-url=https://xxx:6443`
  - 예: `jhub.k8s.bearer-token=***`

---

## 4) 재현 방법(Reproduction)

- 재현 절차:
  1.
  2.
- 재현에 사용한 명령어:

```bash
# 예시
./gradlew bootRun
curl -v http://localhost:8080/api/sessions
```

---

## 5) 기대 동작(Expected)

- 기대 결과:

---

## 6) 실제 동작(Actual)

- 실제 결과:

---

## 7) 원인 분석(Root Cause Analysis)

- 근본 원인:
- 왜 발생했는지(메커니즘):
- 왜 감지/방지되지 않았는지:

---

## 8) 조사 과정(Investigation Notes)

시간 순으로 “가설 → 실험 → 결과 → 다음 가설”을 기록합니다.

- 가설 1:
  - 실험:
  - 결과:
- 가설 2:
  - 실험:
  - 결과:

---

## 9) 해결 방법(Fix)

- 코드 변경 요약:
- 변경 파일 목록:
- 핵심 diff/설명:

---

## 10) 검증(Verification)

- 확인한 API/화면:
- 테스트 케이스(있다면):
- 로그/스크린샷(있다면):

---

## 11) 재발 방지(Prevention)

- 모니터링/알람:
- 가드레일(Validation/Fail-fast):
- 문서화/런북:

---

## 12) 참고(References)

- 링크:
- 관련 문서:

---

## 13) 부록(Appendix)

- 전체 로그(필요한 경우만)
- 설정 파일 스니펫(민감정보 마스킹)
