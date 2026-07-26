# TODO Issue Registration Implementation Plan

> **For agentic workers:
> ** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (
> `- [ ]`) syntax for tracking.

**Goal:** `TODO.md`에서 바로 실행 가능한 항목 7개를 small issue로, 대형 항목 2개를 Epic issue로 GitHub에 한국어 제목/본문으로 등록한다.

**Architecture:** 먼저 GitHub issue 중복 여부를 검색해 기존 항목과 겹치지 않는지 확인한다. 그다음
`TODO.md`의 문맥을 바탕으로 small issue 7개와 Epic issue 2개의 제목/본문을 한국어로 구성하고, `gh issue create`로 순차 등록한 뒤 결과 번호를 기록한다.

**Tech Stack:** GitHub CLI (`gh`), git remote `bluetape4k/bluetape4k-projects`, Markdown issue body,
`TODO.md`, superpowers spec/index 문서

**Spec:** `docs/superpowers/specs/2026-04-24-todo-issue-registration-design.md`

---

## File Structure Map

### Files to Create

- 없음

### Files to Modify

- `docs/superpowers/plans/2026-04-24-todo-issue-registration-plan.md`
- `docs/superpowers/index/2026-04.md`
- `docs/superpowers/INDEX.md`

### External Systems

- GitHub repository: `bluetape4k/bluetape4k-projects`
- GitHub Issues: `gh issue list`, `gh issue create`

### Explicit Scope Rules

- 실제 code/README/test 수정은 이번 계획 범위에 포함하지 않는다.
- 기존 완료 항목인 `core 모듈 Deprecated 정리`는 issue 등록 대상에서 제외한다.
- issue 제목과 본문은 모두 한국어로 작성한다.
- small issue는 가능한 한 한 PR로 닫을 수 있는 범위를 유지한다.
- Epic issue는 하위 작업 추적용으로만 작성하고 구현 체크리스트를 과도하게 넣지 않는다.

---

### Task 1: 최종 등록 대상 확정

**Files:**

- Modify: `docs/superpowers/specs/2026-04-24-todo-issue-registration-design.md`
- Reference: `TODO.md`

- [ ] **Step 1: Small issue 최종 목록 확인**

대상은 아래 7개다.

```text
utils/science — NetCdf 지원 완성
examples/jpa-querydsl-demo — QueryDSL 쿼리 완성
io 모듈 레거시 정리
infra 모듈 정리
testing/testcontainers — HazelcastServer 수정
Spring Boot 3 / 4 동기화 유지
Redis Codec — ForyFast 지원 추가
```

- [ ] **Step 2: Epic issue 최종 목록 확인**

대상은 아래 2개다.

```text
[Epic] x-obsoleted 처리 계획
[Epic] Javers + Exposed = Event Sourcing / CQRS / DDD
```

- [ ] **Step 3: 제외 항목 확인**

아래 항목은 이번 등록 대상에서 제외한다.

```text
core 모듈 Deprecated 정리
모듈 신규 추가 검토
문서화 개선
빌드 / CI 개선
보안
성능 / 품질
```

- [ ] **Step 4: spec와 목록 일치 여부 확인**

Run:

```bash
grep -n "Small issue 후보\|Epic issue 후보\|제외" docs/superpowers/specs/2026-04-24-todo-issue-registration-design.md
```

Expected: small issue 7개, epic 2개, 제외 사유가 spec에 반영되어 있다.

---

### Task 2: 기존 GitHub issue 중복 검색

**Files:**

- Reference: `TODO.md`

- [ ] **Step 1: NetCdf / QueryDSL 관련 기존 issue 검색**

Run:

```bash
gh issue list --limit 50 --state all --search 'NetCdf OR QueryDSL in:title,body' --json number,title,state,url
```

Expected: 등록 후보와 실질적으로 중복되는 issue가 없어야 한다.

- [ ] **Step 2: Deprecated / Spring Boot 동기화 관련 기존 issue 검색**

Run:

```bash
gh issue list --limit 50 --state all --search 'Deprecated 정리 OR Spring Boot 3 / 4 동기화 in:title,body' --json number,title,state,url
```

Expected: `io 모듈 레거시 정리`, `infra 모듈 정리`, `Spring Boot 3 / 4 동기화 유지`와 중복되는 issue가 없어야 한다.

- [ ] **Step 3: HazelcastServer / ForyFast 관련 기존 issue 검색**

Run:

```bash
gh issue list --limit 50 --state all --search 'HazelcastServer OR ForyFast in:title,body' --json number,title,state,url
```

Expected: `testing/testcontainers — HazelcastServer 수정`, `Redis Codec — ForyFast 지원 추가`와 중복되는 issue가 없어야 한다.

- [ ] **Step 4: Epic 후보 기존 issue 검색**

Run:

```bash
gh issue list --limit 50 --state all --search 'x-obsoleted OR Javers Exposed CQRS DDD in:title,body' --json number,title,state,url
```

Expected: Epic 2개와 같은 주제를 이미 추적 중인 issue가 없어야 한다.

- [ ] **Step 5: 중복 발견 시 제목/범위 재조정**

중복이 있으면 새 issue를 만들지 말고 아래 기준으로 조정한다.

```text
동일 범위면 신규 issue 생성 중단
부분 중복이면 제목을 더 구체화
Epic 중복이면 기존 issue 번호를 기준점으로 사용
```

---

### Task 3: Small issue 7개 본문 작성

**Files:**

- Reference: `TODO.md`

- [ ] **Step 1: `utils/science — NetCdf 지원 완성` 본문 준비**

```markdown
## 배경
`utils/science`의 NetCdf 지원에는 아직 미구현 메서드와 미완성 테스트가 남아 있다.

## 작업 범위
- `NetCdfCatalogService.kt`의 미구현 메서드 구현
- `NetCdfTableTest.kt` 테스트 완성
- UCAR `netcdfAll` 의존성 추가 후 파이프라인 검증

## 완료 기준
- [ ] NetCdf 관련 미구현 메서드가 동작한다.
- [ ] 관련 테스트가 통과한다.
- [ ] 변경 모듈 `README.md` / `README.ko.md`를 동기화한다.
- [ ] 필요 시 `docs/testlogs/YYYY-MM.md`에 기록한다.

## 참고 위치
- `TODO.md` §1.1
```

- [ ] **Step 2: `examples/jpa-querydsl-demo — QueryDSL 쿼리 완성` 본문 준비**

```markdown
## 배경
`examples/jpa-querydsl-demo`의 `MemberRepositoryImpl.kt`에는 아직 구현되지 않은 QueryDSL 예제가 남아 있다.

## 작업 범위
- `findByName()` 구현
- `findByAgeGreaterThan()` 구현
- `findByNameContaining()` 구현

## 완료 기준
- [ ] 3개 메서드의 `TODO("Not yet implemented")`가 제거된다.
- [ ] 관련 테스트 또는 예제 검증이 통과한다.
- [ ] 예제 README가 필요하면 갱신된다.
- [ ] 필요 시 `docs/testlogs/YYYY-MM.md`에 기록한다.

## 참고 위치
- `TODO.md` §1.2
```

- [ ] **Step 3: `io 모듈 레거시 정리` 본문 준비**

```markdown
## 배경
`io` 계층에는 deprecated 또는 중복 성격의 레거시 HTTP/직렬화 API가 남아 있다.
`io/crypto/` jasypt 기반 암호화 모듈은 2026-04-17에 삭제 완료되었으며,
이번 이슈는 HTTP 클라이언트와 직렬화 API 정리를 범위로 한다.

## 작업 범위
- `io/http/` — `AHC`(AsyncHttpClient), `OkHttp3`, `HC5` 레거시 HTTP 클라이언트 정리
- `io/jackson2/`, `io/jackson3/` — deprecated 직렬화 API 정리

## 범위 외 (별도 이슈)
- Retrofit2 정리는 이 이슈에 포함하지 않는다. SB3/4에서 이미 제거되었으나 `io` 모듈의 정리 범위와 영향도는 별도 이슈로 다룬다.

## 완료 기준
- [ ] `io/http/` 내 `AHC`, `OkHttp3`, `HC5` 정리 완료 (삭제 또는 대체 처리)
- [ ] `io/jackson2/`, `io/jackson3/` deprecated 직렬화 API 정리 완료
- [ ] 관련 테스트가 통과한다.
- [ ] 변경 모듈 `README.md` / `README.ko.md`를 동기화한다.
- [ ] `docs/testlogs/YYYY-MM.md`에 검증 결과를 기록한다.

## 참고 위치
- `TODO.md` §2.1
```

- [ ] **Step 4: `infra 모듈 deprecated 파일 조사 및 정리 계획 수립` 본문 준비**

```markdown
## 배경
`infra/` 계층에 deprecated 파일 12개가 확인되어 있으나 각 파일의 사용 현황, 대체 여부, 삭제 가능 여부가 아직 정리되지 않은 상태다.
이 이슈는 현황 파악과 후속 구현 이슈 분해를 목표로 하는 조사/계획 이슈다.

## 작업 범위
- `infra/` 하위 deprecated 파일 12개 전수 조사
  - 파일별 사용 현황 확인 (호출부, 테스트, 의존 모듈)
  - 파일별 처리 방향 결정: 삭제 / 대체 구현으로 교체 / 유지 (유지 시 사유 명시)
- 조사 결과를 바탕으로 후속 구현 이슈 분해 계획 수립

## 완료 기준
- [ ] deprecated 파일 12개 전체에 대한 인벤토리 문서가 작성된다.
  - 파일 경로, 현재 용도, 처리 방향(삭제/교체/유지), 유지 시 사유를 표 형식으로 정리
- [ ] 파일별 keep/delete/replace 결정이 확정되어 인벤토리 문서에 명시된다.
- [ ] 실제 코드 정리가 필요한 항목에 대해 후속 구현 이슈 분해 계획이 문서화된다.
  - 예: 삭제 대상 X개 → 별도 PR, 교체 대상 Y개 → 별도 PR
- [ ] 인벤토리 문서가 `docs/` 또는 해당 모듈 README에 기록된다.

## 이 이슈에서 하지 않는 것
- 실제 deprecated 코드 삭제 및 교체 구현 (후속 이슈에서 진행)

## 참고 위치
- `TODO.md` §2.3
```

- [ ] **Step 5: `testing/testcontainers — HazelcastServer deprecated API 수정` 본문 준비**

```markdown
## 배경
`HazelcastServer.kt`는 deprecated Hazelcast API를 사용하고 있어 Hazelcast 5.x 호환성 확보가 필요하다.

## 작업 범위
- `Config`, `NetworkConfig`, `JoinConfig`, `TcpIpConfig` 최신 API로 교체
- Hazelcast 5.x 호환성 확인

## 완료 기준
- [ ] deprecated Hazelcast API 4개가 최신 API로 교체된다.
- [ ] 관련 테스트가 통과한다.
- [ ] `testing/testcontainers` README 문서가 필요 시 갱신된다.
- [ ] `docs/testlogs/YYYY-MM.md`에 검증 결과를 기록한다.

## 참고 위치
- `TODO.md` §3
```

- [ ] **Step 6: `Spring Boot 3 / 4 동기화 유지 — 체크리스트 및 CI 구성` 본문 준비**

```markdown
## 배경
현재 Spring Boot 3 / 4 계열은 13개 모듈이 완벽한 대칭 구조로 유지되고 있다.
그러나 신규 모듈 추가 시 한쪽에만 구현되는 사고를 예방할 공식 체크리스트가 없고,
Spring Framework 7.x 대응에 따른 Spring Boot 4 BOM 업데이트를 체계적으로 추적하는 방법도 정의되어 있지 않다.
또한 `spring-boot4` 모듈이 `spring-boot3`와 독립적으로 테스트되는지 CI 수준에서 확인이 필요하다.
이 이슈는 현재 구조의 취약 지점을 보강하는 절차/문서/CI 정비를 목표로 한다.

## 작업 범위
- 신규 모듈 추가 시 SB3/SB4 양쪽 동시 구현을 강제하는 체크리스트 수립 및 PR 템플릿 반영
- Spring Boot 4 BOM 업데이트 추적 방식 정리 (Spring Framework 7.x 대응 기준 명시)
- `spring-boot4` 모듈 독립 테스트 CI 구성 확인 및 필요 시 조정

## 완료 기준
- [ ] SB3/SB4 대칭 구현 체크리스트가 PR 템플릿(`.github/pull_request_template.md`)에 추가된다.
- [ ] Spring Boot 4 BOM 업데이트 추적 방식이 `docs/` 또는 CLAUDE.md에 문서화된다.
- [ ] `spring-boot4` 독립 테스트 CI 구성 확인 결과가 코멘트 또는 `docs/` 문서에 기록된다.
  - CI가 정상이면 확인 완료 기록, 미흡하면 수정 후 재확인
- [ ] 변경된 문서/템플릿의 내용이 리뷰 가능한 상태로 PR에 포함된다.

## 참고 위치
- `TODO.md` §5
```

- [ ] **Step 7: `Redis Codec — ForyFast 지원 추가` 본문 준비**

```markdown
## 배경
`2026-04-23-redis-json-codec-design.md` 스펙에서 범위 분리.
JSON Codec(Jackson3/Fastjson2) 완료 후 후속 PR로 진행하는 고성능 Redis Codec 지원.
`ForyBinarySerializer.fast()` (SCHEMA_CONSISTENT, refTracking=false) 기반으로,
기존 Fory(COMPATIBLE) 대비 직렬화 성능이 높으나 스키마 진화가 불가능한 휘발성 캐시 전용 codec이다.

## 작업 범위
- `io/io` `BinarySerializers.kt` — `ForyFast`, `LZ4ForyFast`, `ZstdForyFast`, `SnappyForyFast` lazy 프로퍼티 추가
- `infra/redisson` `ForyFastCodec.kt` — Redisson BaseCodec 구현, fallbackCodec = Kryo5
- `infra/redisson` `RedissonCodecs.kt` — `ForyFast`, `LZ4ForyFast`, `ZstdForyFast` val 추가
- `infra/lettuce` `LettuceBinaryCodecs.kt` — `foryFast()`, `lz4ForyFast()`, `zstdForyFast()` factory 추가
- 각 Codec roundtrip 테스트 + ForyCodec 비호환 검증 테스트
- 기존 JSON/Binary Codec 벤치마크에 ForyFast 비교군 추가

## 완료 기준
- [ ] Redisson/Lettuce 양쪽에 ForyFast codec이 추가된다.
- [ ] ForyFast(SCHEMA_CONSISTENT)와 Fory(COMPATIBLE) 포맷 비호환 제약이 각 codec 클래스의 KDoc에 명시된다.
- [ ] `infra/redisson`, `infra/lettuce`, `io/io` 변경 모듈의 `README.md` / `README.ko.md`에 ForyFast codec 사용법과 제약이 추가된다.
- [ ] 테스트와 벤치마크가 통과한다.
- [ ] `docs/testlogs/YYYY-MM.md`에 검증 결과를 기록한다.

## 제약사항 (반드시 숙지)
- ForyFast(SCHEMA_CONSISTENT)와 Fory(COMPATIBLE) 포맷 상호 비호환 — fallback은 Kryo5
- 순환 참조 객체 불가, 스키마 진화 불가
- **휘발성 캐시 전용** — DB/파일 영속 데이터에 사용 금지

## 참고 위치
- `TODO.md` §12
- `docs/superpowers/specs/2026-04-23-redis-json-codec-design.md`
```

---

### Task 4: Epic issue 2개 본문 작성

**Files:**

- Reference: `TODO.md`

- [ ] **Step 1: `[Epic] x-obsoleted 레거시 모듈 처리 계획` 본문 준비**

```markdown
## 목표
`x-obsoleted/` 아래 14개 레거시 모듈을 실무 가치 기준으로 승격/통합/삭제로 재분류하고 단계적으로 정리한다.
2026-04-20 전수 조사 완료 기준으로 실행 단계를 진행한다.

## 포함할 하위 주제

### Phase 1 — 🔴 승격 강력 추천 (독립 PR)
- `lingua → utils/lingua` — **이미 완료** (TODO §4.1 `[x]` 표시, 제외 대상)
- `nats → infra/nats` (30 kt 파일, NATS JetStream + Coroutines) — **후속 이슈 대상**
- `ahocorasick → utils/text-search` (11 kt 파일) — **후속 이슈 대상**

### Phase 2 — 🟡 조건부 승격
- `bloomfilter` — InMemory 부분만 `utils/probabilistic`으로 승격, Redis 기반은 `infra/lettuce` 흡수 완료 상태
- `captcha → utils/images-captcha` (10 kt 파일)
- `logback-kafka → infra/logging-kafka` (14 kt 파일)

### Phase 3 — 🟢 완전 제거
- `mapstruct`, `mutiny-examples`, `tokenizer`, `vertx-coroutines`, `vertx-sqlclient`, `vertx-webclient`, `naivebayes` (7개)
- 사유: 구현 없음 또는 사용처 없음 또는 기존 모듈로 대체 완료

### Phase 4 — 최종 정리
- `x-obsoleted/` 디렉토리 최종 삭제 (`settings.gradle.kts` 정리 포함)

## 이번 패스 제외 범위
- `lingua` 승격: 이미 완료됨, 이 Epic의 추적 범위에서 제외
- `javers`: 별도 Epic으로 분리 (`[Epic] Javers + Exposed — Event Sourcing / CQRS / DDD 스택 구축`)
- 각 하위 모듈의 실제 구현 PR 작성 (하위 이슈로 분해하여 진행)

## 예상 후속 이슈
- `nats → infra/nats` 승격 PR
- `ahocorasick → utils/text-search` 승격 PR
- `captcha → utils/images-captcha` 승격 PR
- `logback-kafka → infra/logging-kafka` 승격 PR
- x-obsoleted 7개 모듈 삭제 PR

## 참고 위치
- `TODO.md` §4
```

- [ ] **Step 2: `[Epic] Javers + Exposed = Event Sourcing / CQRS / DDD` 본문 준비**

```markdown
## 목표
`x-obsoleted/javers/*`를 `data/` 트리로 승격하고 Exposed 생태계와 통합해 JPA 대체 가능한 Event Sourcing 기반 DDD 스택의 기반을 마련한다.

## 포함할 하위 주제
- Phase 1: `data/javers-*` 이관, 빌드/테스트 복구, Kotlin 2.3 / JVM 21 대응
- Phase 2: `ExposedCdoSnapshotRepository` 등 Exposed 통합 구현
- Phase 3: DDD 패턴 헬퍼 추가
- Phase 4: CQRS / Event Sourcing 예제 및 Spring Boot 3/4 자동 구성

## 이번 패스 제외 범위
- 즉시 전체 구현 착수
- JPA Envers 대체 마이그레이션 가이드 완성

## 예상 후속 issue
- `x-obsoleted/javers → data/javers-*` 기반 이관
- Exposed snapshot/commit 저장소 구현
- `examples/javers-exposed-ddd` 예제 추가

## 참고 위치
- `TODO.md` §11
```

---

### Task 5: GitHub issue 실제 등록

**Files:**

- External: GitHub Issues

- [ ] **Step 1: Small issue 7개 순차 등록**

각 본문을 임시 파일에 저장한 뒤 아래 패턴으로 등록한다.

```bash
gh issue create --title "utils/science — NetCdf 지원 완성" --body-file /tmp/issue-netcdf.md
```

Expected: issue 번호와 URL이 반환된다.

- [ ] **Step 2: Epic issue 2개 순차 등록**

```bash
gh issue create --title "[Epic] x-obsoleted 처리 계획" --body-file /tmp/issue-x-obsoleted.md
```

Expected: issue 번호와 URL이 반환된다.

- [ ] **Step 3: 등록 결과 수집**

아래 형식으로 번호와 제목을 정리한다.

```text
#123 utils/science — NetCdf 지원 완성
#124 examples/jpa-querydsl-demo — QueryDSL 쿼리 완성
...
#131 [Epic] Javers + Exposed = Event Sourcing / CQRS / DDD
```

- [ ] **Step 4: 생성 후 중복/누락 확인**

Run:

```bash
gh issue list --limit 20 --state open --json number,title,url
```

Expected: 새로 생성한 9개 issue가 보인다.

---

### Task 6: Plan/index 문서 정리

**Files:**

- Modify: `docs/superpowers/index/2026-04.md`
- Modify: `docs/superpowers/INDEX.md`

- [ ] **Step 1: 월별 인덱스의 plan 링크 추가**

`2026-04-24` 행의 `Plan` 열을 아래처럼 수정한다.

```markdown
[plan](../plans/2026-04-24-todo-issue-registration-plan.md)
```

- [ ] **Step 2: 상태 갱신 준비**

issue 등록이 끝나면 상태를 `✅`로 변경하고 완료 메모에 생성된 issue 번호를 적는다.

```markdown
9개 issue 등록 완료 — #NNN ~ #NNN
```

- [ ] **Step 3: 허브 인덱스 수치 유지 확인**

Run:

```bash
python - <<'PY'
from pathlib import Path
import re
root=Path('docs/superpowers')
text=(root/'INDEX.md').read_text()
print(text)
PY
```

Expected: summary/월별 건수가 최신 상태와 모순되지 않는다.

---

## Self-Review Checklist

- spec의 등록 대상 7개 small issue + 2개 epic issue가 모두 plan에 반영되어 있다.
- `core 모듈 Deprecated 정리` 제외 결정이 plan에 반영되어 있다.
- 각 small issue와 epic issue 본문 초안이 plan 안에 실제 Markdown으로 들어 있다.
- 중복 issue 검색 단계가 들어 있다.
- 실제 등록 명령과 기대 결과가 포함되어 있다.
