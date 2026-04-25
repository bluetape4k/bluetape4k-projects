# 후속 Issue Draft — utils/science README 전체 재작성

> 본 파일은 PR #107 (NetCdf 지원 완성) 머지 후 GitHub Issue 로 등록할 후속 작업의 draft 입니다.
> spec §1.3 "비목표" 항목으로 분리되었습니다.

## 제목

`docs(utils/science): README 전체 재작성 — NetCDF/Shapefile/GIS 통합 가이드`

## 본문 (한국어)

### 배경

PR #107 에서 NetCdf 지원이 완성되면서 `utils/science/README.md` (영문) 와 `utils/science/README.ko.md` (한국어) 에 NetCdf 챕터가 추가되었습니다. 그러나 README 전체 구성은 여전히 Shapefile 중심으로 작성되어 있어, 본 모듈이 다음 4가지 영역을 모두 다룬다는 점을 적절히 표현하지 못합니다:

1. **GIS 좌표 변환** (proj4j)
2. **Shapefile 처리** (GeoTools)
3. **JTS Geometry 연산** (jts-core)
4. **PostGIS 데이터 파이프라인** (Exposed + PostGIS)
5. **NetCDF 파일 임포트** (UCAR netCDF-Java) — 신규

또한 PR #107 머지 후 README 의 "Phase 4: NetCDF Support (Planned)" 섹션이 사라졌으나, 전체 목차는 여전히 Shapefile 중심으로 흐릅니다.

### 작업 범위

`utils/science/README.md` 와 `utils/science/README.ko.md` 양쪽을 다음 구조로 전면 재작성:

```
1. 개요 (모듈이 다루는 5개 영역)
2. Architecture (Mermaid 통합 다이어그램 — 5개 영역의 관계)
3. Module Layout (패키지 구조 + 핵심 클래스)
4. Features (영역별 핵심 기능 한눈에 보기)
5. Quick Start
   5.1 GIS 좌표 변환
   5.2 Shapefile 처리
   5.3 JTS Geometry 연산
   5.4 PostGIS 데이터 파이프라인
   5.5 NetCDF 임포트
6. API 가이드 (영역별 상세 — 현재 README 의 Usage Examples 재구성)
7. 의존성 (compileOnly 정책 + 기능별 추가 의존성)
8. 테스트 (Testcontainers + PostGIS)
9. 성능 / 운영 가이드
10. 관련 모듈
```

### 완료 기준

- [ ] `README.md` (영문) + `README.ko.md` (한국어) 동기 재작성
- [ ] 언어 전환 링크 (`English | [한국어](./README.ko.md)` / `한국어 | [English](./README.md)`) 유지
- [ ] Mermaid 통합 아키텍처 다이어그램 추가
- [ ] 영역별 Quick Start 5개 모두 동작하는 예제로 작성
- [ ] CodeRabbit review 통과

### 비목표

- 코드 변경 없음 (docs only)
- 새로운 기능 추가 없음 — 기존 모듈 구조 / API 그대로 노출

### 우선순위

🟢 Low — 기능 영향 없음. NetCdf 챕터가 이미 추가되어 즉각적인 정보 부족은 없음.

### 라벨

`documentation`, `low-priority`, `utils/science`

### 참조

- 본 PR (NetCdf 지원 완성): #107
- spec: `docs/superpowers/specs/2026-04-25-netcdf-support-design.md`
- plan: `docs/superpowers/plans/2026-04-25-netcdf-support-plan.md`
