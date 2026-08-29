# Issue 1534 MongoDB Backoff Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사용자 소유 `ReactiveMongoOperations`가 있으면 legacy-only URI 검사까지 포함한 library auto-configuration 전체가 backoff하도록 #1534를 수정한다.

**Architecture:** `@ConditionalOnMissingBean(ReactiveMongoOperations::class)`을 Bean 메서드에서 auto-configuration class로 이동해 fallback 소유권과 validation 실행 경계를 일치시킨다. 기존 예외와 fallback 생성 계약은 유지하고 `ApplicationContextRunner` consumer test, KDoc, 영문·한글 README로 경계를 고정한다.

**Tech Stack:** Kotlin 2.4, Spring Boot 4.1 auto-configuration, `ApplicationContextRunner`, MockK, bluetape4k assertions, Gradle 9.7.0

---

### Task 1: 사용자 소유 operations 소비자 회귀 RED

**Files:**
- Modify: `spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/ReactiveMongoAutoConfigurationTest.kt`

- [ ] **Step 1: 사용자 operations와 legacy-only URI를 결합한 테스트를 추가한다**

```kotlin
@Test
fun `사용자 ReactiveMongoOperations가 있으면 legacy URI 검사를 backoff한다`() {
    val operations = mockk<ReactiveMongoOperations>(relaxed = true)

    autoConfigurationRunner
        .withBean(ReactiveMongoOperations::class.java, Supplier { operations })
        .withPropertyValues("spring.data.mongodb.uri=mongodb://127.0.0.1:27018/legacy")
        .run { context ->
            context.getStartupFailure() shouldBeEqualTo null
            context.getBeansOfType<ReactiveMongoOperations>().values.single() shouldBeSameInstanceAs operations
            context.getBeansOfType<ReactiveMongoTemplate>().shouldBeEmpty()
        }
}
```

- [ ] **Step 2: 새 테스트만 실행해 RED를 확인한다**

Run:

```bash
./gradlew :bluetape4k-spring-boot-mongodb:test \
  --tests 'io.bluetape4k.spring.mongodb.ReactiveMongoAutoConfigurationTest.사용자 ReactiveMongoOperations가 있으면 legacy URI 검사를 backoff한다' \
  --no-build-cache
```

Expected: context startup failure가 `LEGACY_URI_MESSAGE`를 포함해 테스트가 실패한다.

### Task 2: fallback 소유권 경계로 조건 이동

**Files:**
- Modify: `spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt`

- [ ] **Step 1: missing-bean 조건을 class에 적용한다**

```kotlin
@ConditionalOnClass(ReactiveMongoOperations::class)
@ConditionalOnMissingBean(ReactiveMongoOperations::class)
class ReactiveMongoAutoConfiguration(
    environment: Environment,
) {
```

- [ ] **Step 2: Bean 메서드의 중복 조건을 제거한다**

```kotlin
@Bean
fun reactiveMongoTemplate(
    databaseFactory: ReactiveMongoDatabaseFactory,
    mongoConverter: MongoConverter,
): ReactiveMongoTemplate = ReactiveMongoTemplate(databaseFactory, mongoConverter)
```

- [ ] **Step 3: 회귀 테스트를 다시 실행해 GREEN을 확인한다**

Run: Task 1 Step 2와 같은 명령

Expected: 새 테스트 `PASS`.

- [ ] **Step 4: auto-configuration test class 전체를 실행한다**

```bash
./gradlew :bluetape4k-spring-boot-mongodb:test \
  --tests 'io.bluetape4k.spring.mongodb.ReactiveMongoAutoConfigurationTest' \
  --no-build-cache
```

Expected: 기존 10개와 새 회귀 테스트를 합한 11개 테스트가 통과한다.

### Task 3: KDoc과 README locale 계약 동기화

**Files:**
- Modify: `spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt`
- Modify: `spring-boot/mongodb/README.md`
- Modify: `spring-boot/mongodb/README.ko.md`

- [ ] **Step 1: KDoc에 fallback 참여 시에만 legacy 검사가 실행된다고 명시한다**
- [ ] **Step 2: README 두 locale에서 사용자 소유 operations의 backoff와 legacy-only fail-fast 범위를 같은 의미로 설명한다**
- [ ] **Step 3: README 두 locale에 rollback 좌표를 추가한다**

```kotlin
implementation(platform("io.github.bluetape4k:bluetape4k-bom:1.12.1"))
implementation("io.github.bluetape4k:bluetape4k-spring-boot-mongodb:1.12.1")
```

- [ ] **Step 4: 예외 문자열, artifact, BOM, version을 locale 간 대조한다**

Expected: 두 README가 같은 경계와 좌표를 설명하고 KDoc이 source behavior와 일치한다.

### Task 4: module 검증과 문서 검증

**Files:**
- Verify: `spring-boot/mongodb/**`

- [ ] **Step 1: module 전체 테스트를 순차 실행한다**

```bash
./gradlew :bluetape4k-spring-boot-mongodb:cleanTest \
  :bluetape4k-spring-boot-mongodb:test --no-build-cache
```

Expected: `BUILD SUCCESSFUL`, JUnit failures 0, skipped 0.

- [ ] **Step 2: module detekt를 실행한다**

```bash
./gradlew :bluetape4k-spring-boot-mongodb:detekt
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: 문서 용어와 locale parity를 확인한다**

```bash
node ~/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  spring-boot/mongodb/README.ko.md \
  docs/superpowers/specs/2026-08-29-issue-1534-mongodb-backoff-design.md \
  docs/superpowers/plans/2026-08-29-issue-1534-mongodb-backoff-plan.md
rg -n '1\.12\.1|ReactiveMongoOperations|spring\.data\.mongodb\.uri' \
  spring-boot/mongodb/README.md spring-boot/mongodb/README.ko.md
git diff --check
```

Expected: 설명되지 않은 용어 충돌, locale drift, whitespace 오류가 없다.

### Task 5: 검토, commit, PR 전달

**Files:**
- Review: 이 계획에 포함된 source, test, README, spec, plan diff
- Create when reusable: `docs/lessons/2026-08-29-issue-1534-mongodb-backoff.md`

- [ ] **Step 1: final Kotlin checklist와 독립 code review에서 P0=0/P1=0을 확인한다**
- [ ] **Step 2: 재사용 가능한 교훈 여부를 판정하고 필요하면 한국어 lesson을 작성한다**
- [ ] **Step 3: Lore protocol을 따르는 한국어 commit을 생성한다**
- [ ] **Step 4: `fix/issue-1534-mongodb-backoff`를 push하고 local/remote exact head를 대조한다**
- [ ] **Step 5: `bluetape4k/bluetape4k-projects`, base `develop`, head `fix/issue-1534-mongodb-backoff`로 PR을 생성한다**
- [ ] **Step 6: #1534의 milestone, assignee, labels를 PR에 반영하고 본문을 `## DoD Status`로 끝낸다**
- [ ] **Step 7: exact-head CI와 최신 review/thread를 확인해 merge-ready에서 중단한다**

Expected: PR metadata와 exact-head CI가 live read-back되고 merge는 최신 별도 승인 전까지 실행하지 않는다.

