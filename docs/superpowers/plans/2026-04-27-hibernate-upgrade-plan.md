# Hibernate 7.x 업그레이드 구현 Plan

- 작성일: 2026-04-27
- 이슈: [#179](https://github.com/debop/bluetape4k-projects/issues/179)
- 워크트리: `.worktrees/feat/hibernate-upgrade`
- 브랜치: `feat/hibernate-upgrade`
- 연계 Spec: [`docs/superpowers/specs/2026-04-27-hibernate-upgrade-design.md`](../specs/2026-04-27-hibernate-upgrade-design.md)
- 작성자: bluetape4k-design (plan 단계)

---

## 0. 개요

### 0.1 2-phase 분리 (Spec §4.3)

| Phase | PR 목표 | 범위 |
|-------|---------|------|
| **Phase 1** | H7 마이그레이션 (이슈 #179-phase1) | Libs 갱신 + ORM/Reactive/Cache SPI 정합 + 기존 테스트 전수 통과 |
| **Phase 2** | 커버리지 70%+ (이슈 #179-phase2) | baseline 측정 → GAP 보강 → Kover measure-only 게이트 |

> 본 plan은 두 phase 모두를 task로 정의하지만, **Phase 1 머지 후 Phase 2를 별도 PR로 진행**한다.

### 0.2 핵심 위험 핀포인트 (Spec §8 R1~R10)

- R1 / R3: H7 SPI 패키지 이동 → `HibernateInternals.kt` adapter 격리
- R2: Reactive 3.x Mutiny/Stage 시그니처 변경 → 함수 단위 매핑 후 정합
- R6: `currentVertxDispatcher()` runTest 비호환 → Phase 2에서 dispatcher 파라미터 주입으로 해결
- R10: Micrometer Statistics silent drop → `generate_statistics=true` + counter 검증 테스트

### 0.3 Phase 1 의존성 그래프

```
T0 (사전 조사) ──┬─> T1 (Libs.kt) ──┬─> T2 (data/hibernate)        ──┐
                 │                   ├─> T3 (cache-lettuce)         ──┼─> T5 (테스트) ─> T6 ─> T7 ─> T8 ─> T9
                 │                   └─> T4 (hibernate-reactive)    ──┘
                 └─> (시그니처 diff)
```

T2/T3/T4는 T1 완료 후 **병렬 진행 가능**. T5는 T2~T4 모두 컴파일 통과 후 시작.

### 0.4 Phase 2 의존성 그래프

```
Phase 1 머지 ─> T10 (baseline) ──┬─> T11 (data/hibernate)         ──┐
                                  ├─> T12 (hibernate-reactive)     ──┼─> T14 (Kover gate)
                                  └─> T13 (cache-lettuce)          ──┘
```

T11/T12/T13 병렬 가능.

---

## 1. Phase 1 — H7 마이그레이션 PR

### T0. 사전 조사 (H7 source jar 추출 + SPI diff)

- **complexity**: medium
- **예상 소요**: 1.5h
- **의존**: 없음
- **목적**: 본격적인 코드 변경 전, H7 SPI 시그니처 변경 현황을 확정한다. R1/R3 위험을 사전에 정량화한다.

#### 작업 항목

1. H7 ORM 7.2.4.Final source jar 추출
   - 위치: `.claude/lib-sources/hibernate-7.2.4/`
   - 대상 라이브러리:
     - `org.hibernate.orm:hibernate-core:7.2.4.Final` (sources)
     - `org.hibernate.orm:hibernate-jcache:7.2.4.Final` (sources)
   - 추출 후 `org/hibernate/cache/`, `org/hibernate/engine/spi/`, `org/hibernate/cfg/` 디렉토리 위주로 확인
2. Hibernate Reactive 3.x 최신 stable 버전 결정
   - MVNRepository / Maven Central 에서 `org.hibernate.reactive:hibernate-reactive-core` 의 최신 stable 확인
   - 현재 spec 기준 `3.2.x.Final` 추정 — 실제 버전 픽스 후 plan 본문에 기재
3. SPI 시그니처 diff 표 작성 (아래 §1.T0.출력 영역에 inline 채움)
   - `RegionFactoryTemplate.prepareForUse(SessionFactoryOptions, Map)` H6 → H7 시그니처 비교
   - `org.hibernate.cache.internal.*` 클래스 재배치 현황 (특히 `BasicCacheKeyImplementation`, `DefaultCacheKeysFactory`)
   - `SessionImplementor.getFactory()` deprecate/제거 여부
   - `Type` 시스템 패키지 (`JavaType`/`JdbcType`) 이동 여부
4. Hibernate Reactive 3.x — Mutiny/Stage 변경 사항 확인
   - `Mutiny.SessionFactory.withSession(Function<Mutiny.Session, Uni<T>>)` 시그니처 유지 여부
   - `Stage.SessionFactory.withTransaction(BiFunction<Stage.Session, Stage.Transaction, CompletionStage<T>>)` 변경 여부

#### 대상 파일

- `.claude/lib-sources/hibernate-7.2.4/` (신규 디렉토리, jar 추출 결과)
- 본 plan 파일의 §1.T0.출력 영역 inline 갱신

#### 검증 방법

- jar 추출 후 `eza .claude/lib-sources/hibernate-7.2.4/` 로 디렉토리 트리 확인
- Spec §6.1 의 영향 매트릭스와 실제 H7 시그니처 일치 여부 검증
- diff 표가 T2/T3/T4 코드 변경의 사전 근거로 충분한지 셀프 체크

#### T0 출력 — 시그니처 diff 표 (T0 완료 후 채움)

> ⚠️ T0 작업 완료 시 아래 표를 실제 값으로 채운다. 현재는 placeholder.

| API | H6.6.44.Final | H7.2.4.Final | 영향 모듈 |
|-----|---------------|--------------|-----------|
| `RegionFactoryTemplate.prepareForUse` | TBD | TBD | hibernate-cache-lettuce |
| `org.hibernate.cache.internal.DefaultCacheKeysFactory` | TBD | TBD | hibernate-cache-lettuce |
| `SessionImplementor.getFactory()` | TBD | TBD | data/hibernate |
| `JavaType`/`JdbcType` 패키지 | TBD | TBD | data/hibernate Converter |
| `Mutiny.SessionFactory.withSession` | TBD | TBD | hibernate-reactive |
| `Stage.SessionFactory.withTransaction` | TBD | TBD | hibernate-reactive |
| Reactive 3.x 정확한 버전 | — | TBD | Libs.kt |

---

### T1. Libs.kt 버전 갱신

- **complexity**: low
- **예상 소요**: 15min
- **의존**: T0 (Reactive 정확한 버전 확정)

#### 작업 항목

1. `buildSrc/src/main/kotlin/Libs.kt` 에서 다음 상수 갱신
   - `hibernate = "6.6.44.Final"` → `hibernate = "7.2.4.Final"`
   - `hibernate_reactive = "2.4.11.Final"` → `hibernate_reactive = "{T0 결과}"`
2. 변경 전 버전을 `// previous: 6.6.44.Final` 주석으로 보존 (롤백 대비, Spec §8-R)
3. `hibernate_validator = "9.1.0.Final"` 유지 확인 (변경 없음)

#### 대상 파일

- `buildSrc/src/main/kotlin/Libs.kt`

#### 검증 방법

- `./gradlew --stop && ./gradlew help` — buildSrc 재컴파일 확인
- `./gradlew :bluetape4k-hibernate:dependencies --configuration runtimeClasspath | rg hibernate-core` — 7.2.4 resolve 확인

---

### T2. data/hibernate ORM SPI 정합

- **complexity**: high
- **예상 소요**: 4h
- **의존**: T1
- **병렬 가능**: T3, T4 와 병렬

#### 작업 항목

1. **`HibernateInternals.kt` 신규 작성** (Spec §6.3 어댑터 패턴)
   - 경로: `data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/internal/HibernateInternals.kt`
   - 책임: H7 internal SPI 사용을 한 곳에 격리. 향후 H8 마이그레이션 시 단일 수정 지점.
   - 노출 내부 어댑터 (예시):
     - `internal fun Session.unwrapSessionImpl(): SessionImplementor`
     - `internal fun EntityManager.currentJdbcConnection(): java.sql.Connection`
     - `internal fun SessionFactory.unwrapSessionFactoryImpl(): SessionFactoryImpl`
2. **`EntityManagerSupport.kt` 수정**
   - `currentConnection()` → `HibernateInternals.currentJdbcConnection()` 호출로 변경
   - `asSessionImpl()` → `HibernateInternals.unwrapSessionImpl()` 호출로 변경
   - 직접 `org.hibernate.engine.spi.*` 임포트 제거
3. **`SessionFactorySupport.kt` 수정**
   - `SessionFactoryImpl` 캐스팅을 `HibernateInternals.unwrapSessionFactoryImpl()` 로 위임
4. **`HibernateEntityListener.kt` 수정**
   - H7 의 nullable event parameter 계약 정합 (T0 diff 표 결과 반영)
   - `PreInsertEvent? -> Unit` 등 nullable 파라미터 적용
5. **Converter 8종 임포트 재매핑** (필요 시)
   - `JavaType`/`JdbcType` 패키지 이동이 T0에서 확인되면 임포트 갱신
   - 대상 파일: `*Converter.kt` (data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/)
6. **deprecated 경고 zero 보장**
   - `lsp_diagnostics` 로 deprecated 경고 확인 → quick fix 적용 (CLAUDE.md Kotlin Edit Workflow)

#### 대상 파일

- `data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/internal/HibernateInternals.kt` (신규)
- `data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/EntityManagerSupport.kt`
- `data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/SessionFactorySupport.kt`
- `data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/listeners/HibernateEntityListener.kt`
- `data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/converters/*.kt` (필요 시)

#### 검증 방법

- `./gradlew :bluetape4k-hibernate:compileKotlin` 통과
- `./gradlew :bluetape4k-hibernate:build -x test` (CP2)
- `lsp_diagnostics_directory data/hibernate/src/main/kotlin` — error/deprecated 0건
- 직접 `org.hibernate.engine.spi.*` 임포트는 `HibernateInternals.kt` 한 파일에만 존재함을 `rg "org.hibernate.engine.spi"` 로 확인

---

### T3. data/hibernate-cache-lettuce RegionFactory SPI 정합

- **complexity**: high
- **예상 소요**: 3h
- **의존**: T1
- **병렬 가능**: T2, T4 와 병렬

#### 작업 항목

1. **`LettuceNearCacheRegionFactory.kt` 수정**
   - `prepareForUse(SessionFactoryOptions, Map)` 시그니처가 H7에서 `Map`/`Properties` 중 어느 쪽인지 T0 결과 반영
   - `start()` / `stop()` lifecycle override 시그니처 점검
2. **`LettuceNearCacheStorageAccess.kt` 수정**
   - `org.hibernate.cache.internal.*` 임포트 H7 재배치 매핑 (R3 핵심)
     - 예: `org.hibernate.cache.internal.DefaultCacheKeysFactory` 위치 변경 시 새 패키지로 임포트
   - `BasicCacheKeyImplementation` 등 cache key 직렬화 클래스 위치 확인
3. **`LettuceNearCacheRegion.kt` (있는 경우) — Region 인터페이스 변경 점검**
4. **cache 키 직렬화 호환성 점검** (R7)
   - H6/H7 노드 혼재 시 key 포맷이 달라지면 충돌 → 만약 차이 있을 경우, README 에 "rolling upgrade 시 cache 플러시 필요" 문구 추가
5. **deprecated 경고 zero 보장**

#### 대상 파일

- `data/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt`
- `data/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt`
- `data/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/*.kt` (해당 시)

#### 검증 방법

- `./gradlew :bluetape4k-hibernate-cache-lettuce:compileKotlin` 통과
- `./gradlew :bluetape4k-hibernate-cache-lettuce:build -x test` (CP3)
- `rg "org.hibernate.cache.internal" data/hibernate-cache-lettuce/src/main/kotlin` — 모든 임포트가 H7 패키지로 매핑됐는지 확인

---

### T4. data/hibernate-reactive Mutiny/Stage 3.x 정합

- **complexity**: high
- **예상 소요**: 4h
- **의존**: T1
- **병렬 가능**: T2, T3 와 병렬

#### 작업 항목

1. **`mutiny/SessionFactorySupport.kt`, `mutiny/SessionSupport.kt` 수정**
   - `Mutiny.SessionFactory.withSession()` 시그니처 정합
   - `Mutiny.Session.withTransaction()` 시그니처 정합
   - `Uni<T>` 반환 타입 변경 여부 점검
2. **`stage/SessionFactorySupport.kt`, `stage/SessionSupport.kt` 수정**
   - `Stage.SessionFactory.withSession()` 시그니처 정합
   - `Stage.SessionFactory.withTransaction(BiFunction)` 시그니처 정합 (R2)
   - `CompletionStage<T>` 반환 타입 변경 여부 점검
3. **`withTransactionSuspending(1-arg)` `inline`/`crossinline` 정합**
   - 코루틴 어댑터에서 람다 캡처 제약 (`crossinline` vs `noinline`) 재검증
   - `awaitSuspending`, `coAwait` 의 동작 변경 여부 (Mutiny 2.7+ 호환)
4. **Vert.x 4.5+ 호환성 점검** (Spec §6.2)
   - 테스트 컨테이너 환경에서만 영향 → 컴파일 단계에서는 영향 적음
5. **Phase 1 범위 — `currentVertxDispatcher()` 는 그대로 유지** (R6 는 Phase 2 에서 해결)
6. **deprecated 경고 zero 보장**

#### 대상 파일

- `data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionFactorySupport.kt`
- `data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionSupport.kt`
- `data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionFactorySupport.kt`
- `data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionSupport.kt`

#### 검증 방법

- `./gradlew :bluetape4k-hibernate-reactive:compileKotlin` 통과
- `./gradlew :bluetape4k-hibernate-reactive:build -x test` (CP4)
- public API surface (suspend wrapper) 함수명/시그니처 변경 없음 확인 (binary backward-compat 확인은 Phase 2)

---

### T5. 기존 테스트 전수 통과

- **complexity**: medium
- **예상 소요**: 2h (실패 시 +bugfix-workflow 시간)
- **의존**: T2, T3, T4 모두 컴파일 통과

#### 작업 항목

1. 모듈별 전수 테스트 실행
   - `./gradlew :bluetape4k-hibernate:test`
   - `./gradlew :bluetape4k-hibernate-cache-lettuce:test`
   - `./gradlew :bluetape4k-hibernate-reactive:test`
2. 실패 시:
   - **bugfix-workflow** 스킬 실행 (CLAUDE.md 지침)
   - 실패 패턴 분류:
     - SPI 변경 누락 → T2/T3/T4 추가 수정
     - cache 직렬화 회귀 → T3 추가 점검
     - Reactive 비동기 timing → T4 + Vert.x dispatcher 점검
3. **testlog 파일 작성 생략** (사용자 지시) — 결과 (passing/skipped/failed count + duration) 만 보고

#### 검증 방법

- 각 모듈 테스트 결과: `passing N / skipped M / failed 0`
- Testcontainers PostgreSQL/MySQL 통합 테스트 fail rate < 10% (Spec §8-R 롤백 트리거 기준)

---

### T6. Micrometer Statistics 검증 (R10)

- **complexity**: low
- **예상 소요**: 30min
- **의존**: T5

#### 작업 항목

1. `data/hibernate` 테스트 환경에서 `hibernate.generate_statistics=true` 활성화 확인
   - 대상: `src/test/resources/hibernate.properties` 또는 `application-test.yml`
2. Statistics counter 검증 테스트 추가 또는 기존 테스트 확인
   - `sessionOpen` count 증가 검증
   - `transactionCount` 증가 검증
   - 위치: `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/StatisticsVerificationTest.kt` (신규 또는 기존 보강)
3. silent metrics drop 회귀 안전망 확보

#### 대상 파일

- `data/hibernate/src/test/resources/hibernate.properties` (또는 application-test.yml)
- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/StatisticsVerificationTest.kt`

#### 검증 방법

- `./gradlew :bluetape4k-hibernate:test --tests "*StatisticsVerificationTest*"` 통과
- counter 증가가 0 이 아님 확인

---

### T7. 의존 모듈 검증

- **complexity**: medium
- **예상 소요**: 1.5h
- **의존**: T5

#### 작업 항목

1. **`spring-boot3/hibernate-lettuce`**
   - `build.gradle.kts` 의 `force resolution` 을 H7.2.4 로 갱신 (Spec §7.1)
     ```kotlin
     configurations.all {
         resolutionStrategy {
             force("org.hibernate.orm:hibernate-core:7.2.4.Final")
         }
     }
     ```
   - `./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:test`
2. **`spring-boot4/hibernate-lettuce`** (이미 H7 force 있음)
   - BOM 정합 확인 — force 제거 가능성 검토
   - `./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:test`
3. **`examples/jpa-querydsl-demo`**
   - `./gradlew :jpa-querydsl-demo:build` 시도
   - 실패 시:
     - 단기: build 에서 모듈 skip 주석 + README 에 "QueryDSL 5.1.0 + H7 비호환, 6.0+ 마이그레이션 필요" 주석
     - 장기: 별도 spec (`Phase 3` Out of Scope, Spec §4.3)
4. 결과 보고: 각 모듈의 passing/skipped/failed + duration

#### 대상 파일

- `spring-boot3/hibernate-lettuce/build.gradle.kts`
- `spring-boot4/hibernate-lettuce/build.gradle.kts` (검토만)
- `examples/jpa-querydsl-demo/build.gradle.kts` (필요 시 skip)

#### 검증 방법

- 각 모듈 `:test` 통과 또는 명시적 skip 주석
- transitive 의존성 충돌 점검: `./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:dependencies --configuration runtimeClasspath | rg hibernate`

---

### T8. 보안 감사 (R8)

- **complexity**: low
- **예상 소요**: 1h
- **의존**: T5

#### 작업 항목

1. `@Cache` 어노테이션 적용 엔티티 목록 확인
   - `rg "@Cache" data/hibernate/src/test/kotlin data/hibernate-cache-lettuce/src/test/kotlin` 또는 `ide_search_text`
   - 테스트 fixture 외 production code 에는 직접적인 `@Cache` 엔티티 없음 확인
2. PII 필드 포함 여부 확인 (Spec §9-A.2)
   - 테스트 엔티티에 PII 가 평문 캐시되는 케이스가 있다면 README 경고 추가
3. Kryo/Fory codec 클래스 허용 목록 (allowlist) 정책 확인
   - `infra/cache-lettuce` 또는 `data/hibernate-cache-lettuce` 의 codec 설정에서 `acceptClass` / `allowlist` 적용 여부 점검
   - 정책이 명시되지 않았다면 README 에 "프로덕션 환경에서는 allowlist 강제 필수" 안내 추가

#### 대상 파일

- `data/hibernate-cache-lettuce/README.md` + `README.ko.md`
- (필요 시) production codec 설정 파일

#### 검증 방법

- `@Cache` 적용 엔티티가 PII 미포함임을 셀프 체크 + 보고
- allowlist 정책 문서화 또는 기존 정책 확인 결과를 보고

---

### T9. README + 마이그레이션 가이드

- **complexity**: low
- **예상 소요**: 1.5h
- **의존**: T7, T8

#### 작업 항목

1. **모듈 README 갱신** — 영문/한국어 동기화 (CLAUDE.md 필수)
   - `data/hibernate/README.md` + `README.ko.md` — H7 업그레이드 사실 + 주요 H6→H7 변경 기재
   - `data/hibernate-reactive/README.md` + `README.ko.md` — Reactive 3.x 정합 노트
   - `data/hibernate-cache-lettuce/README.md` + `README.ko.md` — RegionFactory/StorageAccess 변경 + cache 직렬화 호환성 (R7) 안내
2. **BREAKING_CHANGES 문서**
   - 루트 `BREAKING_CHANGES.md` 또는 각 모듈 README 의 "H6→H7 공개 API 변경 목록" 섹션 추가
   - 외부 소비자가 마이그레이션할 때 참조할 수 있도록 변경된 public API 나열
3. **변경된 public API 에 KDoc 갱신** (`@Deprecated(replaceWith=...)` 또는 KDoc 보강)
4. **wiki 인덱스 등록**
   - `/wiki-update` 스킬 실행 (Spec §9-A.3)
   - spec + plan 파일이 wiki/Obsidian 에 인덱싱됐는지 확인

#### 대상 파일

- `data/hibernate/README.md` + `README.ko.md`
- `data/hibernate-reactive/README.md` + `README.ko.md`
- `data/hibernate-cache-lettuce/README.md` + `README.ko.md`
- `BREAKING_CHANGES.md` (루트, 신규 또는 갱신)

#### 검증 방법

- README 두 언어 버전 모두 H7 정보 포함 확인
- `BREAKING_CHANGES.md` 섹션이 H6→H7 변경을 enum 으로 나열
- `/wiki-update` 후 Obsidian 검색으로 spec/plan 노출 확인

---

### Phase 1 완료 체크리스트 (Spec §9-A 정합)

- [ ] T0 시그니처 diff 표 채움
- [ ] T1 Libs.kt 갱신 + buildSrc 재컴파일
- [ ] T2/T3/T4 컴파일 통과 (CP2/CP3/CP4)
- [ ] T5 기존 테스트 전수 통과 (CP5)
- [ ] T6 Micrometer Statistics 카운터 증가 검증
- [ ] T7 의존 모듈 (spring-boot3/4 + examples) 통과
- [ ] T8 보안 감사 결과 보고
- [ ] T9 README/BREAKING_CHANGES + wiki 갱신
- [ ] `oh-my-claudecode:code-reviewer` HIGH/CRITICAL 0건
- [ ] PR description: 변경 요약, 마이그레이션 노트, 테스트 결과, 검증 명령

---

## 2. Phase 2 — 커버리지 70%+ PR

> Phase 1 머지 후 별도 PR 로 진행. baseline 측정 후 GAP 영역 보강.

### T10. baseline coverage 측정

- **complexity**: low
- **예상 소요**: 30min
- **의존**: Phase 1 머지

#### 작업 항목

1. 모듈별 Kover HTML 리포트 생성
   - `./gradlew :bluetape4k-hibernate:koverHtmlReport`
   - `./gradlew :bluetape4k-hibernate-reactive:koverHtmlReport`
   - `./gradlew :bluetape4k-hibernate-cache-lettuce:koverHtmlReport`
2. 각 모듈의 line coverage 실측값 기록 (Spec §5.1 의 추정치 갱신)
   - `build/reports/kover/html/index.html` → line coverage % 확인
3. Spec §5.2 의 우선 보강 영역 대비 GAP 매핑

#### 대상 파일

- (보고 전용) — 본 plan 의 §2.T10.출력 영역에 결과 inline 갱신

#### 검증 방법

- 3개 모듈 모두 baseline % 확정
- 70% 까지의 gap 이 보강 가능한 범위인지 셀프 체크

#### T10 출력 — baseline 표 (T10 완료 후 채움)

| 모듈 | baseline line % | gap to 70% | 우선 보강 영역 |
|------|-----------------|------------|----------------|
| `bluetape4k-hibernate` | TBD | TBD | Converter round-trip, Listener lifecycle, HibernateExtensions 분기 |
| `bluetape4k-hibernate-reactive` | TBD | TBD | Mutiny/Stage success/error/cancel, withSession 코루틴 |
| `bluetape4k-hibernate-cache-lettuce` | TBD | TBD | RegionFactory start/stop, StorageAccess put/get/evict/contains |

---

### T11. data/hibernate 커버리지 보강

- **complexity**: medium
- **예상 소요**: 4h
- **의존**: T10
- **병렬 가능**: T12, T13 과 병렬

#### 작업 항목 (Spec §5.2)

1. `SessionFactorySupport` 분기 — null factory / 캐스팅 실패 케이스
2. Converter 8종 round-trip 테스트
   - `*Converter.kt` 각각에 대해 `convertToDatabaseColumn` ↔ `convertToEntityAttribute` 양방향 검증
3. `AbstractEntityLifecycleListener` lifecycle phase 별 호출 검증
   - `@PrePersist`, `@PostPersist`, `@PreUpdate`, `@PostUpdate`, `@PreRemove`, `@PostRemove`, `@PostLoad`
4. `HibernateExtensions.kt` — `withSession`, `withStatelessSession`, `inTransaction` 분기
   - 정상 / 예외 / 롤백 경로
5. `JpaConsts` / `HibernateConsts` 상수 클래스 검증 (낮은 가중치, fast)

#### 대상 파일

- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/SessionFactorySupportTest.kt`
- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/converters/*ConverterTest.kt`
- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/listeners/EntityLifecycleListenerTest.kt`
- `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/HibernateExtensionsTest.kt`

#### 검증 방법

- `./gradlew :bluetape4k-hibernate:koverHtmlReport` → line coverage ≥ 70%
- 신규/수정 public API 100% covered (Spec §9-B.1)

---

### T12. data/hibernate-reactive 커버리지 보강 (R6 해결 포함)

- **complexity**: high
- **예상 소요**: 5h
- **의존**: T10
- **병렬 가능**: T11, T13 과 병렬

#### 작업 항목 (Spec §5.2 + R6 완화)

1. **`currentVertxDispatcher()` → `dispatcher` 파라미터 기본값 주입** (backward compatible)
   - 시그니처 변경 (예시):
     ```kotlin
     suspend fun <T> Mutiny.SessionFactory.withSessionSuspending(
         dispatcher: CoroutineDispatcher = currentVertxDispatcher(),
         block: suspend (Mutiny.Session) -> T,
     ): T
     ```
   - 기존 호출자는 영향 없음 — 테스트는 `dispatcher = StandardTestDispatcher()` 등으로 주입
2. `runTest` 로 suspend 경로 검증 (Spec §9-B.1)
   - `MutinySessionExtensionsTest` — `awaitSuspending`, `coAwait` 의 success / error / cancel
   - `StageSessionExtensionsTest` — `CompletionStage` 변환 분기 (success / error / cancel)
3. `withSession` / `withTransaction` 코루틴 어댑터 롤백 경로
   - 트랜잭션 내 예외 발생 → 롤백 검증
   - 트랜잭션 내 cancellation → 롤백 검증

#### 대상 파일

- `data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionFactorySupport.kt` (시그니처 추가)
- `data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/mutiny/SessionSupport.kt`
- `data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionFactorySupport.kt`
- `data/hibernate-reactive/src/main/kotlin/io/bluetape4k/hibernate/reactive/stage/SessionSupport.kt`
- `data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/mutiny/MutinySessionExtensionsTest.kt`
- `data/hibernate-reactive/src/test/kotlin/io/bluetape4k/hibernate/reactive/stage/StageSessionExtensionsTest.kt`

#### 검증 방법

- `./gradlew :bluetape4k-hibernate-reactive:koverHtmlReport` → line coverage ≥ 70%
- 기존 호출자 (Phase 1 코드) 가 변경 없이 컴파일 통과 (backward compat)
- `runTest` 기반 테스트 통과

---

### T13. data/hibernate-cache-lettuce 커버리지 보강

- **complexity**: medium
- **예상 소요**: 3h
- **의존**: T10
- **병렬 가능**: T11, T12 와 병렬

#### 작업 항목 (Spec §5.2)

1. `LettuceNearCacheRegionFactory` start/stop 라이프사이클 검증
   - 다중 start/stop 안정성
   - factory shutdown 시 connection close 검증
2. `LettuceNearCacheStorageAccess` 시나리오
   - `put` / `get` / `evict` / `contains` 각각 단위 테스트
   - TTL / 만료 검증
3. 분산 동기화 (lettuce pub/sub) 통합 테스트 — Testcontainers Redis

#### 대상 파일

- `data/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactoryTest.kt`
- `data/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccessTest.kt`

#### 검증 방법

- `./gradlew :bluetape4k-hibernate-cache-lettuce:koverHtmlReport` → line coverage ≥ 70%
- Testcontainers Redis 통합 테스트 통과

---

### T14. Kover 70% measure-only 게이트 설정

- **complexity**: low
- **예상 소요**: 30min
- **의존**: T11, T12, T13 모두 70% 도달

#### 작업 항목

1. 3개 모듈의 `build.gradle.kts` 에 Kover 70% measure-only verification rule 설정
   ```kotlin
   kover {
       reports {
           verify {
               rule {
                   minBound(70, coverageUnits = LINE)
                   // measure-only: fail-on-violation 은 별도 이슈에서
               }
           }
       }
   }
   ```
2. fail-on-violation 활성화는 본 PR 범위 밖 — 별도 이슈 (Spec §5.3)

#### 대상 파일

- `data/hibernate/build.gradle.kts`
- `data/hibernate-reactive/build.gradle.kts`
- `data/hibernate-cache-lettuce/build.gradle.kts`

#### 검증 방법

- `./gradlew :bluetape4k-hibernate:koverVerify` 실행 — 70% 충족 시 통과
- CI 에서 measure-only 게이트가 build 를 깨뜨리지 않음 확인 (Spec §4.1 위험 5 완화)

---

### Phase 2 완료 체크리스트 (Spec §9-B 정합)

- [ ] T10 baseline 표 채움
- [ ] T11 `data/hibernate` line coverage ≥ 70%
- [ ] T12 `data/hibernate-reactive` line coverage ≥ 70% + `dispatcher` 파라미터 주입
- [ ] T13 `data/hibernate-cache-lettuce` line coverage ≥ 70%
- [ ] T14 Kover measure-only 게이트 적용
- [ ] 신규/수정 public API 100% covered
- [ ] `oh-my-claudecode:code-reviewer` HIGH/CRITICAL 0건

---

## 3. 진행 규칙 및 메모

### 3.1 작업 순서 요약

**Phase 1**:
1. T0 → T1
2. T2 / T3 / T4 (병렬)
3. T5 (전수 테스트)
4. T6 → T7 → T8 → T9
5. code-reviewer → PR

**Phase 2** (Phase 1 머지 후):
1. T10 (baseline)
2. T11 / T12 / T13 (병렬)
3. T14 (게이트)
4. code-reviewer → PR

### 3.2 testlog 정책

- 사용자 지시: **`docs/testlogs/` 파일 작성 생략**
- 테스트 실행 결과 (passing / skipped / failed count + duration) 는 보고에 포함

### 3.3 롤백 사전 준비 (Spec §8-R)

- T1 에서 변경 전 버전을 주석으로 보존 (이미 작업 항목에 포함)
- Phase 1 PR 머지 후 회귀 발생 시 `git revert <merge-commit>` 1회로 롤백 가능하도록 단일 feature branch 유지

### 3.4 위험 — 핫스팟 점검 빈도

| 위험 | 점검 시점 |
|------|-----------|
| R1 (SPI 컴파일) | T0 / T2 / T3 / T4 |
| R2 (Reactive Mutiny/Stage) | T0 / T4 |
| R3 (cache.internal 임포트) | T0 / T3 |
| R4 (QueryDSL 비호환) | T7 |
| R5 (Spring Boot 3 transitive 충돌) | T7 |
| R6 (currentVertxDispatcher) | T12 (Phase 2) |
| R7 (rolling upgrade key 충돌) | T3 / T9 (README 안내) |
| R8 (Kryo/Fory allowlist) | T8 |
| R9 (Spring Data JPA 3.x + H7) | T7 |
| R10 (Statistics silent drop) | T6 |

### 3.5 PR 분리 원칙 (Spec §4.3)

- Phase 1 PR 과 Phase 2 PR 은 **반드시 분리**
- Phase 1 PR 머지 후 안정화 (1주 권장) 를 거친 뒤 Phase 2 시작
- 두 PR 모두 동일 worktree `feat/hibernate-upgrade` 에서 진행하되, 머지 후 `git pull --rebase` 또는 별도 sub-branch 운영

---

## 4. 다음 단계

- T0 사전 조사 진행 → diff 표 채우기
- 그 결과를 본 plan 파일에 inline 갱신
- T1 ~ T9 순차/병렬 진행 후 Phase 1 PR 생성
- Phase 1 머지 후 T10 ~ T14 진행 후 Phase 2 PR 생성
