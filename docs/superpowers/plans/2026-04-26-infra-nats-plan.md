# Plan: infra/nats 모듈 승격 (Implementation)

- **Spec**: `docs/superpowers/specs/2026-04-26-infra-nats-design.md`
- **Issue**: #139
- **Branch**: `issue-139-nats` (worktree: `.worktrees/issue-139-nats`)
- **Date**: 2026-04-26
- **Status**: Ready for execution

## 0. 결정사항 (Spec에서 확정)

| 항목 | 결정 |
|------|------|
| Spring 의존성 노출 전략 | **옵션 B** — `compileOnly(Libs.nats_spring)`만 노출, `nats_spring_cloud_stream_binder` 제외 |
| `io.nats.examples.*` 처리 | **옵션 A** — 25개 파일 전체 패키지 리네이밍 (정식 테스트 전환) |
| `chainOfCommand` 매핑 | `io.bluetape4k.nats.client.examples.chainOfCommand` (**service 하위 아님**) |
| `@Deprecated coPublish` | 즉시 제거 (binary-compat 부담 없음) |
| 7개 헬퍼 파일 (`@Test` 미보유) | 헬퍼/데이터 클래스 — 별도 @Test 추가 불필요, 구조 확인만 |
| 선결 베이스라인 | `git mv` **후** `:bluetape4k-nats:test` 첫 실행 (x-obsoleted는 settings.gradle.kts 미등록, 실행 불가) |

## 1. 실측 파일 매핑 (25개 io.nats.examples → io.bluetape4k.nats.*)

### 1.1 `io.nats.examples` (top-level, 10개) → `io.bluetape4k.nats.client.examples`

| 원본 파일 | @Test 보유 | 비고 |
|-----------|-----------|------|
| CoreReplyRequestPatterns.kt | YES | |
| EncodingExample.kt | YES | |
| FunctionalExamples.kt | YES | |
| KeyValueIntroExamples.kt | YES | |
| ObjectStoreExample.kt | YES | |
| PubSubExample.kt | YES | |
| RecreateConsumerExample.kt | YES | |
| RequestReplyExample.kt | YES | |
| ServerPoolExample.kt | YES | |
| SimplicationMigrationExample.kt | YES | |

### 1.2 `io.nats.examples.chainOfCommand` (5개) → `io.bluetape4k.nats.client.examples.chainOfCommand`

| 원본 파일 | @Test 보유 | 비고 |
|-----------|-----------|------|
| App.kt | YES | 실행 진입점 |
| Endpoint.kt | NO | 데이터 클래스 (헬퍼) |
| Input.kt | NO | 데이터 클래스 (헬퍼) |
| PublishStyleWorkers.kt | NO | 워커 헬퍼 (App.kt에서 사용) |
| RequestStyleWorkers.kt | NO | 워커 헬퍼 (App.kt에서 사용) |

### 1.3 `io.nats.examples.jetstream` (3개) → `io.bluetape4k.nats.client.examples.jetstream`

| 원본 파일 | @Test 보유 | 비고 |
|-----------|-----------|------|
| JetStreamTestUtils.kt | NO | 테스트 유틸리티 (헬퍼) |
| NatsJsPubAsync.kt | NO | 헬퍼 (NatsJsPubAsync2가 @Test 보유) |
| NatsJsPubAsync2.kt | YES | |

### 1.4 `io.nats.examples.jetstream.simple` (6개) → `io.bluetape4k.nats.client.examples.jetstream.simple`

| 원본 파일 | @Test 보유 | 비고 |
|-----------|-----------|------|
| AbstractSimpleExample.kt | NO | 추상 클래스 (헬퍼) |
| ContextExample.kt | YES | |
| FetchMessagesExample.kt | YES | |
| IterableConsumerExample.kt | YES | |
| MessageConsumerExample.kt | YES | |
| NextExample.kt | YES | |

### 1.5 `io.nats.examples.service` (1개) → `io.bluetape4k.nats.service.examples`

| 원본 파일 | @Test 보유 | 비고 |
|-----------|-----------|------|
| ServiceExample.kt | YES | |

**합계**: 25 파일 / 18 @Test 클래스 / 7 헬퍼 클래스

## 2. Task 목록 (의존성 순서)

### Phase A: 디렉토리 이동 + 빌드 스크립트

> **[high #1 수정]**: `x-obsoleted`는 settings.gradle.kts 미등록 → `:x-obsoleted-nats:test` 실행 불가.
> 베이스라인은 `git mv` 후 `:bluetape4k-nats:test` 첫 실행으로 대체한다.

#### Task 1. `git mv x-obsoleted/nats infra/nats`
- **complexity**: low
- **dependency**: -
- **action**:
  - `git mv x-obsoleted/nats infra/nats`
  - `git log --follow infra/nats/build.gradle.kts` 로 history 보존 확인
- **verification**: `git status` 에서 rename 인식 확인

#### Task 2. `build.gradle.kts` 재작성 (Spec §5.3)
- **complexity**: medium
- **dependency**: Task 1
- **action**:
  - `infra/nats/build.gradle.kts` 를 spec §5.3 초안 기준으로 작성
  - `api(Libs.nats_spring)` → `compileOnly(Libs.nats_spring)` 변경
  - `nats_spring_cloud_stream_binder` 제외 확인 (기존에 있다면 제거)
  - `kotlin("plugin.spring")` 추가하지 않음 (Spring을 require하지 않음)
  - infra/kafka 의존성 그룹 주석 형식과 정렬
  - `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }` 유지
- **verification**: `./gradlew :bluetape4k-nats:dependencies --configuration compileClasspath` 실행 후 spring 잔여 의존성 확인

#### Task 3. Spring transitive 의존성 검증 (Spec §5.3 M1)
- **complexity**: low
- **dependency**: Task 2
- **action**:
  - `./gradlew :bluetape4k-nats:dependencies --configuration compileClasspath` 출력 → spring 관련 항목 확인
  - 심각한 충돌 발견 시 spec §5.3 옵션 C(모듈 분리)로 escalation, plan 갱신
- **verification**: spring-context/spring-messaging 등이 compileClasspath에 노출되지 않음을 확인

### Phase B: 코드 정리 (`@Deprecated` 제거)

#### Task 4. `coPublish` 전수 grep + 제거
- **complexity**: low
- **dependency**: Task 1
- **action**:
  - `rg "coPublish" --type kt` 전체 worktree 실행
  - `infra/nats/src/main/kotlin/io/bluetape4k/nats/client/JetStream.kt` `@Deprecated coPublish` 함수 제거
  - `infra/nats/` 내 참조 → `publishSuspending`으로 치환
  - **infra/nats 외부에서 매치 발견 시**: 별도 commit으로 분리 (본 PR 포함 or 별도 PR 결정)
- **verification**: `rg "coPublish" --type kt` 결과 0건

### Phase C: 패키지 리네이밍 (25개 파일)

#### Task 5. `io.nats.examples` → `io.bluetape4k.nats.client.examples` 디렉토리 이동
- **complexity**: medium
- **dependency**: Task 1
- **action**:
  - `git mv` 로 순서대로 이동 (하위 → 상위 순):
    1. `src/test/kotlin/io/nats/examples/jetstream/simple/` → `src/test/kotlin/io/bluetape4k/nats/client/examples/jetstream/simple/` (6개)
    2. `src/test/kotlin/io/nats/examples/jetstream/` (나머지 3개) → `src/test/kotlin/io/bluetape4k/nats/client/examples/jetstream/`
    3. `src/test/kotlin/io/nats/examples/chainOfCommand/` (5개) → `src/test/kotlin/io/bluetape4k/nats/client/examples/chainOfCommand/`
    4. top-level 10개 파일 → `src/test/kotlin/io/bluetape4k/nats/client/examples/`
    5. **별도 step**: `src/test/kotlin/io/nats/examples/service/ServiceExample.kt` (1개) → `src/test/kotlin/io/bluetape4k/nats/service/examples/ServiceExample.kt`
       - 이 파일만 상위 패키지(`service/examples/`)가 달라 단일 파일 이동으로 처리
  - 빈 디렉토리 정리: `io/nats/examples/` + 부모 `io/nats/` (`git rm -r` 또는 `git status`로 확인)
- **verification**: 25개 파일이 새 경로에 존재, history 보존 (`git log --follow`)

#### Task 6. 25개 파일 `package` 선언 일괄 치환
- **complexity**: medium
- **dependency**: Task 5
- **action**:
  - `package io.nats.examples` → `package io.bluetape4k.nats.client.examples` (10개)
  - `package io.nats.examples.chainOfCommand` → `package io.bluetape4k.nats.client.examples.chainOfCommand` (5개)
  - `package io.nats.examples.jetstream` → `package io.bluetape4k.nats.client.examples.jetstream` (3개)
  - `package io.nats.examples.jetstream.simple` → `package io.bluetape4k.nats.client.examples.jetstream.simple` (6개)
  - `package io.nats.examples.service` → `package io.bluetape4k.nats.service.examples` (1개)
  - `ast-grep` 또는 IntelliJ refactor 사용
- **verification**: `rg "^package io\.nats\.examples" infra/nats/src/test/` → 0건

#### Task 7. 7개 헬퍼 파일 구조 확인
- **complexity**: low
- **dependency**: Task 6
- **action**: 다음 7개 헬퍼 파일이 다른 @Test 클래스에서 참조되는지 확인 (사용처 grep)
  - `chainOfCommand/Endpoint.kt`, `chainOfCommand/Input.kt`, `chainOfCommand/PublishStyleWorkers.kt`, `chainOfCommand/RequestStyleWorkers.kt` → `chainOfCommand/App.kt` 에서 사용
  - `jetstream/JetStreamTestUtils.kt` → `NatsJsPubAsync2.kt` 또는 simple/* 에서 사용
  - `jetstream/NatsJsPubAsync.kt` → `NatsJsPubAsync2.kt` 에서 참조
  - `jetstream/simple/AbstractSimpleExample.kt` → simple/* @Test 클래스가 상속
- **verification**: 각 헬퍼가 최소 1개 @Test 클래스의 의존성에 포함됨을 확인 (orphan이 없음)

#### Task 8. import 일괄 치환 (`io.nats.examples.*` → 새 패키지)
- **complexity**: medium
- **dependency**: Task 6
- **action**:
  - `rg "import io\.nats\.examples"` 전수 검색
  - 각 import 문을 새 패키지로 치환 (디렉토리 매핑과 동일)
  - IntelliJ `ide_optimize_imports` 활용 권장
- **verification**: `rg "import io\.nats\.examples" infra/nats/` → 0건

### Phase D: 테스트 리소스 + 컴파일

#### Task 9. `src/test/resources` 필수 파일 확인/생성
- **complexity**: low
- **dependency**: Task 1
- **action**:
  - `infra/nats/src/test/resources/junit-platform.properties` 존재 확인 (없으면 생성)
  - `infra/nats/src/test/resources/logback-test.xml` 존재 확인 (없으면 생성)
  - 다른 infra 모듈(예: infra/kafka) 의 동일 파일을 reference로 사용
- **verification**: 두 파일 모두 존재, 콘텐츠 정상

#### Task 10. `compileKotlin` 통과
- **complexity**: low
- **dependency**: Task 2, Task 4
- **action**: `./gradlew :bluetape4k-nats:compileKotlin`
- **verification**: BUILD SUCCESSFUL, 0 errors

#### Task 11. `compileTestKotlin` 통과
- **complexity**: medium
- **dependency**: Task 8, Task 9, Task 10
- **action**: `./gradlew :bluetape4k-nats:compileTestKotlin`
- **verification**: BUILD SUCCESSFUL, 0 errors. 잔여 import 오류 발견 시 Task 8로 회귀

### Phase E: 테스트 실행 + 정적 분석

#### Task 12. `:bluetape4k-nats:test` 전수 통과
- **complexity**: high
- **dependency**: Task 11
- **action**:
  - `./bin/repo-test-summary -- ./gradlew :bluetape4k-nats:test`
  - R3 호환성 확인: NatsServer 2.12 ↔ jnats 2.25.1
  - 실패 발견 시 → 옵션 1: NatsServer tag 업데이트, 옵션 2: `@Disabled("NatsServer 2.12 호환성 이슈")` 마킹
- **verification**: passing count + duration 기록 (testlog에 첨부)

#### Task 13. `:bluetape4k-nats:detekt` 통과
- **complexity**: low
- **dependency**: Task 11
- **action**: `./gradlew :bluetape4k-nats:detekt`
- **verification**: BUILD SUCCESSFUL; baseline 미존재 시 `./gradlew :bluetape4k-nats:detektBaseline` 후 issue 검토 및 fix

### Phase F: 문서

#### Task 14. README.md 작성 (영어, Mermaid UML 포함)
- **complexity**: medium
- **dependency**: Task 12
- **action**:
  - `infra/nats/README.md` 작성
  - 제목 직후 언어 전환 링크: `English | [한국어](./README.ko.md)`
  - 구조: **Architecture → UML → Features → Examples → References**
  - Mermaid classDiagram (spec §8.2) + sequence diagram (Pub/Sub, Request/Reply, JetStream Stream→Consumer→Subscriber)
  - Dependency 섹션: Spring 통합은 사용자가 명시적으로 가져옴 (compileOnly 안내)
  - Examples 섹션 경로: `src/test/kotlin/io/bluetape4k/nats/client/examples/...`
- **verification**: Mermaid 코드블록 렌더링 확인, 모든 링크 검증

#### Task 15. README.ko.md 작성 (한국어 동기 버전)
- **complexity**: medium
- **dependency**: Task 14
- **action**:
  - `infra/nats/README.ko.md` 작성, README.md와 섹션 구조·Mermaid 코드블록 동일
  - 제목 직후 `[English](./README.md) | 한국어`
  - 본문은 한국어
- **verification**: README.md와 섹션 1:1 매칭, Mermaid 동일

#### Task 16. 루트 `CLAUDE.md` infra 모듈 표 업데이트
- **complexity**: low
- **dependency**: Task 15
- **action**:
  - `CLAUDE.md` Module Groups 표의 `infra/` 행에 `nats` 추가
- **verification**: `nats` 키워드가 infra/ 행에 포함됨

#### Task 17. testlog + superpowers index 업데이트
- **complexity**: low
- **dependency**: Task 12
- **action**:
  - `docs/testlogs/2026-04.md` 맨 위에 새 행 추가 (테스트 결과: passing/duration)
  - `docs/superpowers/index/2026-04.md` 맨 위에 본 작업 entry 추가
  - `docs/superpowers/INDEX.md` count 갱신
- **verification**: 두 파일 갱신 확인

### Phase G: 리뷰 + 커밋 + PR

#### Task 18. bluetape4k-patterns 체크리스트 적용
- **complexity**: low
- **dependency**: Task 12, Task 13
- **action**:
  - top-level extension function 파일은 companion object KLogging 불필요 (정상)
  - `requireNotBlank` 검증: 기존 파일 확인 (main 소스 grep)
  - `atomicfu` class-level only: 해당 사항 없음 확인
  - `@Synchronized` 사용 없음 확인 (Virtual Thread 고려)
  - `rg "requireNotBlank\|@Synchronized" infra/nats/src/main/`
- **verification**: 위반 사항 0건 또는 발견 시 fix

#### Task 19. Code review 실행
- **complexity**: high
- **dependency**: Task 12, Task 13, Task 14, Task 15, Task 16, Task 17, Task 18
- **action**:
  - `oh-my-claudecode:code-reviewer` 에이전트 실행
  - HIGH/CRITICAL 이슈 해소
  - KDoc 누락 여부 점검 (상위 수준 extension function 파일)
  - publishing dry-run: `./gradlew :bluetape4k-nats:publishToMavenLocal`
- **verification**: HIGH/CRITICAL = 0, publish 성공

#### Task 20. 커밋 (Korean + `feat:` prefix)
- **complexity**: low
- **dependency**: Task 19
- **action**:
  - logical 분할 커밋: `git mv` + `build.gradle.kts` → commit 1 / `coPublish` 제거 + 패키지 리네임 → commit 2 / README + CLAUDE.md → commit 3
  - 한국어 커밋 메시지, `feat:` prefix
- **verification**: `git log` 확인

#### Task 21. PR 생성
- **complexity**: low
- **dependency**: Task 20
- **action**:
  - `gh pr create --json` 비대화 모드
  - PR 본문에 CLAUDE.md "Before Creating a PR (MANDATORY)" 체크리스트 7개 모두 체크:
    - 로컬 테스트 전수 통과 (passing count + duration)
    - Code review 실행 완료 (HIGH/CRITICAL 0)
    - README.md + README.ko.md 업데이트
    - KDoc 추가/업데이트 확인
    - worktree 사용 확인 (`.worktrees/issue-139-nats`)
    - superpowers index 업데이트
    - 검증 명령 목록 + Spec/Plan 링크
- **verification**: PR URL 반환, CI green 대기

## 3. Task 요약 표 (complexity 레이블)

| # | Task | complexity | dependency |
|---|------|-----------|------------|
| 1 | `git mv x-obsoleted/nats infra/nats` | low | - |
| 2 | `build.gradle.kts` 재작성 | medium | 1 |
| 3 | Spring transitive 의존성 검증 | low | 2 |
| 4 | `coPublish` 전수 grep + 제거 | low | 1 |
| 5 | 25개 파일 디렉토리 이동 (`git mv`, ServiceExample.kt 별도 step) | medium | 1 |
| 6 | 25개 파일 `package` 선언 치환 | medium | 5 |
| 7 | 7개 헬퍼 파일 구조 확인 | low | 6 |
| 8 | import 일괄 치환 | medium | 6 |
| 9 | `src/test/resources` 필수 파일 확인/생성 | low | 1 |
| 10 | `compileKotlin` 통과 | low | 2, 4 |
| 11 | `compileTestKotlin` 통과 | medium | 8, 9, 10 |
| 12 | `:bluetape4k-nats:test` 전수 통과 | high | 11 |
| 13 | `:bluetape4k-nats:detekt` 통과 | low | 11 |
| 14 | README.md 작성 (Mermaid UML) | medium | 12 |
| 15 | README.ko.md 작성 (동기) | medium | 14 |
| 16 | 루트 CLAUDE.md infra 표 업데이트 | low | 15 |
| 17 | testlog + superpowers index 업데이트 | low | 12 |
| 18 | bluetape4k-patterns 체크리스트 적용 | low | 12, 13 |
| 19 | Code review 실행 (HIGH/CRITICAL 해소) | high | 12–18 |
| 20 | 커밋 (Korean + `feat:` prefix) | low | 19 |
| 21 | PR 생성 (`gh pr create`, MANDATORY 체크리스트 포함) | low | 20 |

**Complexity 분포**: high 2 / medium 6 / low 13 / **총 21개**

## 4. 검증 명령 모음

```bash
# Phase A (Task 1-3)
git mv x-obsoleted/nats infra/nats
git log --follow infra/nats/build.gradle.kts
./gradlew :bluetape4k-nats:dependencies --configuration compileClasspath | rg spring

# Phase B (Task 4)
rg "coPublish" --type kt                                 # → 수정 후 0건

# Phase C (Task 5-8)
rg "^package io\.nats\.examples" infra/nats/src/test/   # → 0건
rg "import io\.nats\.examples" infra/nats/              # → 0건

# Phase D (Task 9-11)
./gradlew :bluetape4k-nats:compileKotlin
./gradlew :bluetape4k-nats:compileTestKotlin

# Phase E (Task 12-13)
./bin/repo-test-summary -- ./gradlew :bluetape4k-nats:test
./gradlew :bluetape4k-nats:detekt                        # baseline: detektBaseline

# Phase G (Task 18)
rg "requireNotBlank\|@Synchronized" infra/nats/src/main/  # → 확인

# Phase G/H (Task 19-21)
./gradlew :bluetape4k-nats:publishToMavenLocal
gh pr create --json
```

## 5. 실패 시 분기 (R3 escalation)

Task 12(테스트 전수)에서 실패 시:
- **옵션 1**: `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/mq/NatsServer.kt` tag 2.12 → 최신 버전 (별도 commit, 본 PR 포함)
- **옵션 2**: 실패 테스트만 `@Disabled("NatsServer 2.12 호환성 이슈, 후속 PR")` 마킹 후 진행

## 6. 본 PR 범위 외 (후속 작업)

- Issue 등록: `infra/nats-spring` 분리 (옵션 C)
- Spring Boot Starter (`spring-boot3-nats`, `spring-boot4-nats`)
- Reactor NATS 통합
- Micrometer / OpenTelemetry 통합
- jnats 버전 업그레이드
- 단위 테스트 (MockK 기반) 보강
