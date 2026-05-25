# HC5-First HTTP Client 설계 평가

**날짜**: 2026-05-24
**이슈**: #586
**PR**: pending

---

## 배경

`bluetape4k-http`가 5개 HTTP 클라이언트 백엔드를 지원하면서 어느 것을 "기본 권장"으로 가이드해야 하는지 명확하지 않았다. 벤치마크 데이터와 기능 매트릭스를 바탕으로 HC5를 primary로 공식화했다.

---

## 결정

**HC5를 primary recommended HTTP client로 공식 채택.** 나머지 백엔드는 생태계/호환성 옵션으로 분류.

---

## 산출물

- `docs/design/2026-05-24-hc5-first-http-client-recommendation.md` — 설계 노트 (권장, 거절 대안, 마이그레이션 위험, 벤치마크 근거)
- `io/http/README.md` — "Primary Recommendations" 섹션 추가, HTTP Client Comparison 테이블 역할 컬럼 추가
- `io/http/README.ko.md` — 동일 내용 한국어 동기화

---

## 교훈

- 설계 노트는 **결정 + 거절 대안 + 근거**를 함께 기록해야 나중에 재검토 비용이 줄어든다.
- README의 "추천" 섹션은 벤치마크 결과 섹션보다 **상단에** 두어야 찾기 쉽다 (기존 README는 벤치마크 섹션 내부에만 권장 표가 있었음).
- Primary/Compatibility/Ecosystem 분류는 API 제거 없이 명확성을 높이는 문서 전략으로 유효하다.
