# #1323 빌드 기준 정렬 구현 계획

> 설계: `docs/superpowers/specs/2026-08-09-issue-1323-build-standard-design.md`

**목표:** 저장소 기본 빌드 기준을 Kotlin 2.4, JDK 25, Gradle 9.7.0으로
정렬하면서 `virtualthread/jdk21`의 Java 21 호환성 계약을 보존한다.

**범위:** feature branch의 빌드 설정, workflow, 현재 기준 문서와 PR CI
복구 경로를 변경한다. PR 생성과 push는 승인된 전달 단계에서 수행하며,
merge, publish, release와 중앙 Dependabot 버전 갱신은 수행하지 않는다.

## Task 1: Gradle Wrapper 9.7.0 재생성

**Files:**

- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `gradle/wrapper/gradle-wrapper.jar`
- Modify: `gradlew`
- Modify: `gradlew.bat`

**Steps:**

1. JDK 21/Gradle 9.6 기준에서 wrapper task를 두 번 실행한다.
2. `distributionUrl`이 HTTPS Gradle 9.7.0 bin ZIP인지 확인한다.
3. `distributionSha256Sum`을 공식 값으로 고정한다.
4. wrapper JAR SHA-256을 공식 값과 대조한다.
5. `.java-version` 전환 전에 `./gradlew --version`이 9.7.0인지 확인한다.

## Task 2: 기본 compiler와 toolchain을 Kotlin 2.4/JDK 25로 전환

**Files:**

- Modify: `build.gradle.kts`
- Modify: `buildSrc/build.gradle.kts`
- Modify: `.java-version`
- Modify: `virtualthread/jdk21/build.gradle.kts`

**Steps:**

1. 루트 Kotlin language/API를 `KOTLIN_2_4`, `jvmTarget`을 `JVM_25`로 설정한다.
2. 루트 Java compile release와 Kotlin/Java toolchain 기본값을 25로 설정하고,
   `virtualthread-jdk21`, `virtualthread-api`, `logging`, `assertions`, `junit5`의
   최소 project dependency closure만 중앙 목록에서 Java/JVM 21로 선택한다.
3. `buildSrc` language/API와 JVM target을 2.4/25로 설정한다.
4. `virtualthread/jdk21`의 Kotlin JVM target을 `JVM_21`로 명시해 기존 Java
   toolchain/release/test launcher 21과 정렬한다.
5. `.java-version`을 `25`로 변경한다.
6. `jenv exec`와 `javaToolchains`로 JDK 25 기본 선택 및 JDK 21 toolchain 발견을
   확인한다.

## Task 3: mock server 애플리케이션을 JDK 25 기준으로 전환

**Files:**

- Modify: `testing/mock-web-server/build.gradle.kts`
- Modify: `testing/mock-webflux-server/build.gradle.kts`

**Steps:**

1. 두 모듈의 Java/Kotlin toolchain, Java release, test launcher를 25로 변경한다.
2. Jib base image를 `eclipse-temurin:25-jre-alpine`으로 변경한다.
3. 과거 Kotlin 2.3/Java 21 baseline 주석을 Kotlin 2.4/JDK 25 기준으로 고친다.
4. 두 모듈 test와 Jib Docker build를 순차 검증한다.

## Task 4: GitHub Actions 실행 기준 갱신

**Files:**

- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/codeql.yml`
- Modify: `.github/workflows/examples.yml`
- Modify: `.github/workflows/manual-docs.yml`
- Modify: `.github/workflows/nightly-tests.yml`
- Modify: `.github/workflows/publish-snapshot.yml`
- Modify: `.github/workflows/release.yml`
- Modify: `.github/workflows/security.yml`

**Steps:**

1. 기존 Temurin distribution과 action ref를 유지하며 Java version/name을 25로
   바꾼다.
2. CodeQL의 workflow-local Kotlin 2.3.21 치환 단계를 제거하고 중앙 catalog
   Kotlin 2.4.0을 그대로 사용한다.
3. JDK 값과 CodeQL catalog 값 외 `permissions`, trigger, secret/environment,
   fork, `pull_request_target`, action ref가 바뀌지 않았는지 diff로 확인한다.
4. `actionlint .github/workflows/*.yml`을 실행한다.

## Task 4A: JDK 25 runtime provider 정렬

**Files:**

- Modify: `bluetape4k/core/build.gradle.kts`
- Modify: `utils/workflow/build.gradle.kts`
- Modify: `virtualthread/api/build.gradle.kts`
- Modify: `testing/junit5/build.gradle.kts`
- Modify: `bluetape4k/coroutines/build.gradle.kts`
- Modify: `examples/redisson-demo/build.gradle.kts`
- Modify: `examples/virtualthreads-demo/build.gradle.kts`
- Modify: `examples/virtualthreads-demo/README.md`
- Modify: `examples/virtualthreads-demo/README.ko.md`
- Modify: `docs/manual/en/modules/bluetape4k-examples-virtualthreads-demo.md`
- Modify: `docs/manual/ko/modules/bluetape4k-examples-virtualthreads-demo.md`
- Modify: `docs/manual/en/modules/bluetape4k-junit5.md`
- Modify: `docs/manual/ko/modules/bluetape4k-junit5.md`
- Modify: `docs/manual/en/modules/bluetape4k-workflow.md`
- Modify: `docs/manual/ko/modules/bluetape4k-workflow.md`
- Modify: `infra/kafka/src/test/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaConsumerTemplateTest.kt`
- Modify: `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderSupportTest.kt`
- Modify: `io/okio/src/test/kotlin/io/bluetape4k/okio/DeflaterSinkTest.kt`

**Steps:**

1. JDK 25 기본 모듈과 example의 runtime provider를 `virtualthread-jdk25`로
   맞춘다.
2. Java 21 compatibility island의 API compile contract와 test runtime provider는
   `bluetape4k-virtualthread-jdk21`으로 유지한다. `bluetape4k-junit5`는 JDK 21
   provider를 자체 test에만 사용하고 published consumer에 강제하지 않는다.
3. Java 25에서 StructuredTaskScope를 실행하는 `examples/redisson-demo`는
   `testRuntimeOnly(":bluetape4k-virtualthread-jdk25")`를 명시한다.
4. JDK 25로 실행되는 `bluetape4k-coroutines` 테스트는 JDK 21 preview provider가
   ServiceLoader 탐색을 중단시키지 않도록 JDK 25 provider만 `testRuntimeOnly`로
   선택한다.
5. EN/KO README와 manual의 실행 JDK, provider dependency 설명을 실제 build와
   일치시킨다.
6. JDK 25에서 Kotlin이 생성하는 classfile descriptor와 호환되지 않는 테스트
   이름/로컬 클래스 조합을 안전한 테스트 이름으로 고치고, Kafka 테스트 stub의
   nullable Java platform type을 JDK 25 Kotlin compiler에 맞게 정리한다.
7. example, core, workflow, API, junit5, coroutines, Kafka, R2DBC 표적 test를 순차 실행하고
   provider 이름 및 실패 없는 StructuredTaskScope 실행을 확인한다.

## Task 4B: Gradle dependency submission 실행 경로 교체

**Files:**

- Add: `.github/workflows/dependency-submission.yml`
- Modify: `docs/lessons/2026-08-10-issue-1323-build-standard.md`

**Steps:**

1. repository-owned workflow에서 Temurin JDK 25와 Gradle dependency-submission
   action을 사용해 default branch dependency graph를 제출한다.
2. GitHub-managed Automatic Dependency Submission을 repository 설정에서
   비활성화해 JDK 21 dynamic `submit-gradle` job과 중복 실행하지 않는다.
3. `workflow_dispatch`도 `develop`만 checkout하도록 guard하고, contents write를
   사용하는 action ref는 release commit SHA로 고정한다.
4. workflow permissions와 action refs를 최소 범위로 검증하고, live setting과
   workflow dispatch 결과를 기록한다.

## Task 5: 현재 빌드 기준 문서 갱신

**Files:**

- Modify: `README.md`
- Modify: `README.ko.md`
- Modify: `AGENTS.md`
- Modify: `CLAUDE.md`
- Modify: `.github/copilot-instructions.md`
- Modify: `docs/manual/en/getting-started.md`
- Modify: `docs/manual/ko/getting-started.md`
- Modify: `docs/manual/en/modules/bluetape4k-core.md`
- Modify: `docs/manual/ko/modules/bluetape4k-core.md`
- Modify: `cache/hibernate-cache-lettuce/README.md`
- Modify: `cache/hibernate-cache-lettuce/README.ko.md`

**Steps:**

1. 현재 저장소 baseline을 Java 25/Kotlin 2.4/Gradle 9.7.0으로 맞춘다.
2. `.java-version=25`와 `virtualthread/jdk21` dependency closure의 명시적 Java 21
   예외를 설명한다.
3. `AGENTS.md`, `CLAUDE.md`, Copilot 지침은 기존 영어 instruction 문체를
   유지한다.
4. 과거 설계·계획·benchmark 환경 기록과 Java 21 기능 설명은 변경하지 않는다.

## Task 6: 구성·빌드·호환성 검증

**Steps:**

1. `jenv exec ./gradlew --no-daemon --version`, `javaToolchains`, `projects`를
   실행한다.
2. `build -x test`와 `detekt`를 `--rerun-tasks --no-configuration-cache`로
   실행한다.
3. `virtualthread/jdk21`과 `virtualthread/jdk25` test를 순차 실행하고 `--info`
   로그에서 실제 launcher를 확인한다.
4. core Kotlin, resilience4j Java, virtualthread 21/25 대표 classfile major가
   각각 기대값 69, 69, 65, 69인지 `javap -verbose`로 확인한다.
5. mock server test/Jib 검증을 순차 실행한다.
6. workflow allowlist, 문서 baseline 검색, wrapper checksum, `git diff --check`를
   확인한다.
7. 최종 diff 독립 리뷰에서 P0/P1이 없음을 확인하고 변경을 Lore 형식으로
   커밋한다.

8. JDK 25 hosted CI에서 Kafka `compileTestKotlin`, coroutines
   `StructuredConcurrencyTest`, R2DBC Spring context scan, Okio DeflaterSink의
   closed-deflater 예외 계약 실패가 재발하지 않는지 exact head 기준으로
   확인한다.

## 중단 조건

- Gradle 9.7 호환을 위해 범위 밖 plugin/dependency 변경이 필요한 경우
- JDK 21 compatibility artifact를 JDK 25로 올려야만 빌드되는 경우
- repository, credential, permissions 또는 workflow trigger 경계 변경이 필요한 경우
- Docker/Jib 또는 hosted CI가 로컬 권한·환경 때문에 실행 불가능한 경우에는 다른
  검증을 계속하고 해당 항목만 DoD의 명시적 미확인으로 남긴다.
- GitHub Automatic Dependency Submission 설정 API/UI 권한이 없으면 저장소
  소유 workflow와 로컬 검증까지 수행하고 설정 변경만 PENDING으로 남긴다.
