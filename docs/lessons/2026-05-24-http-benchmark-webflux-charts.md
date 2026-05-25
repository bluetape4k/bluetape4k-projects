# HTTP Benchmark WebFlux Charts

**날짜**: 2026-05-24
**이슈**: #584
**PR**: pending

---

## 배경

`io/http/README.md`에 벤치마크 수치 테이블은 있었지만 base throughput 섹션에 차트가 없었다.
high-latency (`io-http-chart-02`) 및 cache (`io-http-cache-throughput-chart-01`) 차트는 이미 존재했으나
base throughput 차트(`io-http-chart-01`)가 누락 상태였다.

---

## 결정

- 두 벤치마크 모두 이미 `BluetapeWebfluxServer`를 사용 중임을 확인 — 코드 변경 불필요.
- 기존 2026-05-21 스냅샷 데이터를 사용해 `io-http-chart-01.svg/png`를 생성.
- `io-http-chart-02.svg` 스타일을 참조하여 수작업 SVG로 작성 (Mermaid/Vega-Lite 금지).

---

## 산출물

- `docs/images/readme-diagrams/io-http-chart-01.svg` — 6.6 KB, 15개 행 horizontal bar chart
- `docs/images/readme-diagrams/io-http-chart-01.png` — 75.5 KB
- `io/http/README.md` — base throughput 테이블 아래 차트 링크 추가
- `io/http/README.ko.md` — 동일 위치 동기화

---

## 교훈

- **차트 스타일 일관성**: 기존 `io-http-chart-02.svg`를 먼저 읽고 동일한 색상 코딩(파란색=VirtualThread/Sync, 초록=Coroutines/Async, 노란색=중간 수준, 빨간색=아웃라이어)을 유지.
- **Diagram generation guide 준수**: 차트는 수치 데이터가 있는 테이블에만 사용; 테이블이 source of truth; PNG만 README에 embed, SVG는 옆에 보관.
- **링크 검증 필수**: `missing=0` 확인 후 커밋.
- **벤치마크 재실행 불필요**: 코드가 이미 올바른 서버(`BluetapeWebfluxServer`)를 사용하고, 최근 스냅샷 데이터가 있을 경우 차트 생성만 하면 됨.
