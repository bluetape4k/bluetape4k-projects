# #1323 빌드 기준 정렬 구현 계획

> 설계: `docs/superpowers/specs/2026-08-09-issue-1323-build-standard-design.md`

**목표:** 저장소 기본 빌드 기준을 Kotlin 2.4, JDK 25, Gradle 9.7.0으로
정렬하면서 `virtualthread/jdk21`의 Java 21 호환성 계약을 보존한다.

**범위:** 로컬 feature branch의 빌드 설정, workflow, 현재 기준 문서만 변경한다.
PR, push, merge, publish, release와 Dependabot 의존성 갱신은 수행하지 않는다.

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
2. 루트 Java compile release와 Kotlin/Java toolchain 기본값을 25로 설정한다.
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

## Task 5: 현재 빌드 기준 문서 갱신

**Files:**

- Modify: `README.md`
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
2. `.java-version=25`와 `virtualthread/jdk21`의 명시적 Java 21 예외를 설명한다.
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

## 중단 조건

- Gradle 9.7 호환을 위해 범위 밖 plugin/dependency 변경이 필요한 경우
- JDK 21 compatibility artifact를 JDK 25로 올려야만 빌드되는 경우
- repository, credential, permissions 또는 workflow trigger 경계 변경이 필요한 경우
- Docker/Jib 또는 hosted CI가 로컬 권한·환경 때문에 실행 불가능한 경우에는 다른
  검증을 계속하고 해당 항목만 DoD의 명시적 미확인으로 남긴다.
