# #1323 빌드 기준 정렬 설계

## 문제

`bluetape4k-projects`는 중앙 catalog에서 Kotlin `2.4.0`을 이미 사용하지만,
루트 compiler 설정은 `languageVersion`과 `apiVersion`을 `2.3`으로 제한한다.
기본 Java toolchain과 GitHub Actions는 JDK 21을 사용하고, Gradle Wrapper는
`9.6.0`이다. 이 상태는 상위 작업
[`bluetape4k-dependencies#182`](https://github.com/bluetape4k/bluetape4k-dependencies/issues/182)의
Kotlin 2.4, JDK 25, Gradle 9.7.0 기준과 일치하지 않는다.

## 목표

- 기본 Kotlin compiler language/API 수준을 2.4로 정렬한다.
- 기본 Java toolchain, 로컬 Java/jenv 선택, CI 실행 JDK를 25로 정렬한다.
- Gradle Wrapper의 properties, JAR, Unix/Windows 스크립트를 9.7.0으로 갱신한다.
- JDK 21 호환성을 명시적으로 소유하는 `virtualthread/jdk21`과 그 project
  dependency closure만 toolchain/release 21 예외를 유지한다. 현재 “workspace
  baseline”을 이유로 21을 고정한 mock server 애플리케이션은 기본 기준과 함께
  25로 전환한다.
- 빌드 기준을 설명하거나 검사하는 문서와 workflow를 실제 설정과 일치시킨다.
- JDK 25로 실행되는 consumer와 example은 JDK 21 preview provider를 runtime에
  끌어오지 않도록 JDK 25 provider를 사용한다. JDK 21 compatibility island의
  compile contract와 전용 provider 테스트는 유지한다.
- JDK 25에서 실행되는 `bluetape4k-coroutines` 테스트는 JDK 21 preview provider를
  test runtime에 함께 올리지 않고 JDK 25 provider만 선택한다. 그래야 ServiceLoader가
  JDK 21 preview classfile을 먼저 읽다가 탐색을 중단하지 않는다.
- JDK 25/Kotlin 2.4 classfile descriptor와 충돌하는 괄호 포함 테스트명 내부의
  로컬 클래스는 테스트 동작을 바꾸지 않는 안전한 이름으로 정리하고, Java platform
  nullable 반환값을 사용하는 Kafka test stub은 명시적 non-null test contract로
  고정한다.
- JDK 21과 JDK 25가 닫힌 `java.util.zip.Deflater`에 서로 다른 예외 타입을
  던질 수 있으므로, Okio `DeflaterSink` 테스트는 두 플랫폼 예외가 `IOException`
  원인으로 보존되는 공통 계약만 검증한다.

## 현재 증거

- `build.gradle.kts`는 `jvmToolchain(21)`, `KOTLIN_2_3` language/API를 설정한다.
- `buildSrc/build.gradle.kts`도 `KOTLIN_2_3` language/API를 설정한다.
- `gradle/wrapper/gradle-wrapper.properties`는 Gradle `9.6.0`을 사용한다.
- `.github/workflows`의 CI, 예제, CodeQL, 보안, 문서, Nightly, snapshot,
  release workflow는 JDK 21을 사용한다.
- CodeQL workflow는 중앙 catalog의 Kotlin `2.4.0`을 확인한 뒤 local catalog를
  `2.3.21`로 치환한다. 이는 Kotlin 2.4 compiler 기준을 실제 CodeQL build에
  적용하지 않으며, Kotlin 공식 호환성 표에서 KGP 2.3.20–2.3.21의 완전 지원
  Gradle 상한은 9.3.0이다.
  ([공식 문서](https://kotlinlang.org/docs/gradle-configure-project.html))
- 이 pin을 추가한 `d4849c14e`는 CodeQL extractor가 Kotlin 2.4.x를 지원하면
  제거하라는 directive를 남겼다. 이후 CodeQL CLI 2.26.0이 Kotlin 2.4.0 분석을
  공식 지원했고, `github/codeql-action` v4.37.0부터 해당 2.26.0 bundle을 기본으로
  사용한다. 현재 v4.37.6은 더 최신인 2.26.2 bundle을 사용한다.
  ([CodeQL 2.26.0](https://codeql.github.com/docs/codeql-overview/codeql-changelog/codeql-cli-2.26.0/),
  [codeql-action v4.37.0](https://github.com/github/codeql-action/releases/tag/v4.37.0))
- `.java-version`은 현재 `21`이며 jenv가 이 파일을 사용해 JDK 21을 선택한다.
- `virtualthread/jdk21/build.gradle.kts`와 `virtualthread/jdk25/build.gradle.kts`는
  각 호환성 line의 toolchain을 이미 독립적으로 고정한다.
- 변경 전 `./gradlew projects --no-configuration-cache`는 Gradle 9.6.0/JDK 21에서
  성공했다.

## 선택한 접근

저장소의 루트 기본값을 JDK 25/Kotlin 2.4로 올리고, 호환성 목적의 하위 모듈이
자체 설정으로 기본값을 재정의하는 현재 구조를 유지한다.

1. 루트 `build.gradle.kts`의 기본 `jvmToolchain`을 25로, Kotlin language/API를
   `KOTLIN_2_4`로 변경한다. Kotlin `jvmTarget`은 `JVM_25`, Java compile
   `options.release`는 25로 명시해 toolchain과 bytecode 계약을 일치시킨다.
   Java 21 호환성 섬은 중앙 project 이름 목록으로 21을 선택한다. Kotlin 2.4에서
   context parameters와 새 annotation use-site target 규칙이 안정화되므로 기존
   `-Xcontext-parameters`, `-Xannotation-default-target=param-property` 실험 옵션은
   같은 언어 동작을 유지한 채 제거한다.
2. `buildSrc/build.gradle.kts`의 Kotlin language/API와 `jvmTarget`도
   `KOTLIN_2_4`/`JVM_25`로 맞춘다.
3. 현재 JDK 21/Gradle 9.6 기준에서 공식 Gradle Wrapper task를 두 번 실행해
   9.7.0 Wrapper 전체 파일을 먼저 재생성한다. 공식 distribution checksum
   `84fbba45c7f4c64abc77460e1c00f541e9f960e3c7ed2538f1ede19eacd873ae`를
   `distributionSha256Sum`으로 고정하고, wrapper JAR checksum
   `7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d`와
   대조한다.
4. Wrapper bootstrap이 성공한 다음 `.java-version`을 `25`로 바꾼다.
5. 모든 실행 workflow의 기본 JDK를 25로 바꾼다. CodeQL의 중앙 catalog Kotlin
   `2.4.0` 확인은 유지하고, Gradle 9.7/Kotlin 2.4 build와 충돌하는 extractor용
   workflow-local `2.3.21` 치환 단계는 제거한다. 기존 `github/codeql-action@v4`
   action ref는 유지하며, hosted 실행에서 CodeQL bundle 2.26.0 이상을 확인한다.
   repository-owned dependency submission은 `develop` branch만 checkout하고
   `contents: write` action ref를 release commit SHA로 고정한다.
6. `README.md`, `README.ko.md`, `AGENTS.md`, `.github/copilot-instructions.md`의
   기본 빌드 기준을 갱신한다. JDK 21 전용 모듈 설명은 호환성 계약이므로 유지한다.
7. `testing/mock-web-server`와 `testing/mock-webflux-server`의 toolchain, release,
   test launcher, Jib base image를 JDK 25로 맞춘다. 이 고정은 공개 JDK 21
   호환성 계약이 아니라 과거 workspace baseline의 복제이므로 예외로 남기지 않는다.
8. 루트의 명시적 `JVM_25` 기본값이 하위 모듈에 상속되므로
   `virtualthread/jdk21`에는 Kotlin `jvmTarget=JVM_21`을 명시해 Java/Kotlin
   classfile 21 예외를 완결한다.

## Java 21 호환성 예외

Gradle variant matching은 Java 21 consumer가 Java 25 project artifact에 의존하는
것을 거부한다. 따라서 `virtualthread/jdk21`만 21로 두면 main/test dependency
resolution이 실패한다. 실제 project dependency closure를 따라 다음 다섯 모듈을
최소 호환성 섬으로 유지한다.

- `bluetape4k-virtualthread-jdk21`: Java 21 runtime 구현체
- `bluetape4k-virtualthread-api`: 두 runtime 구현체가 공유하는 API
- `bluetape4k-logging`: API와 JDK 21 구현체의 main dependency
- `bluetape4k-assertions`, `bluetape4k-junit5`: 위 모듈들의 test dependency closure

이 예외는 루트 `java21CompatibilityProjects` 한 곳에서 소유한다. 나머지 모듈은
JVM 25 variant를 게시한다. 향후 `virtualthread/jdk21`을 제거하거나 별도 release
line으로 분리할 때만 이 목록을 축소하며, 변경 전 project dependency closure와
JDK 21 test runtime을 다시 검증한다.

단, JDK 25로 실행되는 `bluetape4k-core`, `utils/workflow`, example의 runtime
dependency는 `virtualthread-jdk25`를 선택한다. `virtualthread/api`는 Java 21
compatibility island의 consumer이므로 test runtime도 `virtualthread-jdk21`을
유지한다. `bluetape4k-junit5`도 자체 test에만 JDK 21 provider를 연결하고,
published consumer에는 provider를 노출하지 않는다. Java 25에서
`StructuredTaskScopeTester`를 사용하는 `examples/redisson-demo`는 JDK 25
provider를 test runtime에 직접 추가한다. Java 21 호환성은 provider 자체와 JDK
21 launcher 테스트로 증명하고, JDK 25 실행 경로는 JDK 25 provider와 함께
검증한다.

이 방식은 전체 모듈에 JDK 설정을 중복하지 않고, 기본 기준과 명시적 예외의
소유권을 구분한다.

## 로컬 전제조건

- jenv가 설치되어 있고 이 worktree에서 tracked `.java-version`을 해석해야 한다.
- JDK 25는 기본 빌드용으로, JDK 21은 `virtualthread/jdk21`의 실제 launcher와
  호환성 검증용으로 모두 설치되어 있어야 한다.
- 변경 후 `.java-version`의 기대값은 `25`이며, `jenv version`과
  `jenv exec java -XshowSettings:properties -version`이 JDK 25의 vendor,
  runtime version, home을 보여야 한다.
- 어느 JDK도 해석되지 않으면 빌드 설정을 우회하지 않고 설치/등록 문제로 분리해
  중단한다.

## 검토한 대안

### 모든 모듈에 JDK 25를 개별 명시

기본값이 분산되고 신규 모듈이 누락될 수 있어 채택하지 않는다. 또한
`virtualthread/jdk21`의 호환성 계약을 깨뜨릴 위험이 있다.

### 기본 JDK 21을 유지하고 CI에 JDK 25 matrix만 추가

#1323의 “기본 Java toolchain 및 CI 실행 버전 JDK 25” 완료 조건을 충족하지
못하므로 채택하지 않는다.

### `virtualthread/jdk21`까지 JDK 25로 일괄 전환

Java 21용 artifact라는 공개 호환성 의미와 `--release 21` 계약을 훼손하므로
채택하지 않는다.

## 변경 경계

### 빌드 설정

- `build.gradle.kts`
- `buildSrc/build.gradle.kts`
- `.java-version`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradlew`
- `gradlew.bat`
- `testing/mock-web-server/build.gradle.kts`
- `testing/mock-webflux-server/build.gradle.kts`
- `virtualthread/jdk21/build.gradle.kts`
- `bluetape4k/core/build.gradle.kts`
- `utils/workflow/build.gradle.kts`
- `virtualthread/api/build.gradle.kts`
- `testing/junit5/build.gradle.kts`
- `examples/redisson-demo/build.gradle.kts`
- `examples/virtualthreads-demo/build.gradle.kts`

### CI

- `.github/workflows/ci.yml`
- `.github/workflows/codeql.yml`
- `.github/workflows/examples.yml`
- `.github/workflows/manual-docs.yml`
- `.github/workflows/nightly-tests.yml`
- `.github/workflows/publish-snapshot.yml`
- `.github/workflows/release.yml`
- `.github/workflows/security.yml`
- `.github/workflows/dependency-submission.yml`

### 문서와 agent-facing 기준

- `README.md`, `README.ko.md`: 기본 요구사항을 Java 25와 Gradle Wrapper 9.7.0으로 표시하고,
  `.java-version=25` 및 `virtualthread/jdk21` dependency closure의 Java 21
  호환성 예외를 설명한다.
- `AGENTS.md`: build configuration 기준을 Java 25와 Kotlin 2.4로 맞추되
  `virtualthread/jdk21` 예외 규칙은 유지한다.
- `.github/copilot-instructions.md`: Kotlin 2.4/Java 25 기본값과 JDK 21 전용 모듈
  예외를 일관되게 기록한다.
- `CLAUDE.md`: agent-facing build configuration을 Java 25/Kotlin 2.4로 맞추고
  `virtualthread/jdk21`의 명시적 Java 21 예외를 유지한다. 지침 문체는 기존대로
  영어를 사용한다.
- `docs/manual/en/getting-started.md`, `docs/manual/ko/getting-started.md`: 저장소의
  현재 Java/Kotlin 기본 기준을 25/2.4로 갱신한다.
- `docs/manual/en/modules/bluetape4k-core.md`,
  `docs/manual/ko/modules/bluetape4k-core.md`: core의 현재 compile 기준을
  Java 25/Kotlin 2.4로 갱신한다.
- `docs/manual/en/modules/bluetape4k-junit5.md`,
  `docs/manual/ko/modules/bluetape4k-junit5.md`: JDK provider를 consumer가
  선택하고 JUnit 5 module은 자체 test에만 provider를 사용하는 경계를 기록한다.
- `docs/manual/en/modules/bluetape4k-workflow.md`,
  `docs/manual/ko/modules/bluetape4k-workflow.md`: workflow의 JDK 25 provider를
  실제 build와 일치시킨다.
- `bluetape4k/coroutines/build.gradle.kts`: JDK 25 test runtime provider를
  직접 선택해 JDK 21 preview provider 전이를 차단한다.
- `infra/kafka/src/test/kotlin/io/bluetape4k/kafka/spring/core/SuspendKafkaConsumerTemplateTest.kt`:
  Kotlin 2.4/JDK 25 compiler의 Java platform type 추론에 맞춘 test stub을 둔다.
- `data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/query/QueryBuilderSupportTest.kt`:
  Spring ASM classpath scan에서 해석 가능한 테스트 이름을 사용한다.
- `io/okio/src/test/kotlin/io/bluetape4k/okio/DeflaterSinkTest.kt`: 닫힌 Deflater의
  JDK 21 `NullPointerException`/JDK 25 `IllegalStateException` 차이를 허용하되
  `IOException` wrapping 계약을 유지한다.
- `cache/hibernate-cache-lettuce/README.md`,
  `cache/hibernate-cache-lettuce/README.ko.md`: 저장소 기본 정책 참조를 JDK 25+로
  갱신한다.

과거 설계·계획·benchmark 환경 기록과 Java 21 자체 기능을 설명하는 문서는 당시
증거나 모듈 의미이므로 일괄 치환하지 않는다.

## 제외 범위

- 중앙 catalog의 외부 라이브러리 또는 Dependabot 버전 갱신
- production API와 runtime 동작 변경
- `virtualthread/jdk21`의 Java 21 호환성 제거
- tag, publish, release, PR 생성, merge
- Kotlin/JDK/Gradle 전환과 무관한 workflow 또는 문서 정리

## 실패 모드와 대응

변경 전 `git status`, 대상 파일 hash, JDK 21/Gradle 9.6 버전, `projects` 성공
결과를 기준선으로 보존한다. Wrapper 생성이나 JDK 25 구성 검사가 실패하면 해당
단계에서 중단하고 feature worktree의 이번 단계 대상 파일만 `HEAD` 상태로 되돌린
뒤 원인을 기록한다. plugin/dependency downgrade, repository/mirror 변경, 검증 비활성화,
workflow 권한 완화는 이 이슈의 복구 수단으로 허용하지 않는다.

1. **Gradle 9.7.0에서 plugin 또는 build script가 구성되지 않음**
   - `./gradlew projects --no-configuration-cache`로 가장 먼저 탐지한다.
   - 실패하면 원인을 먼저 분리하고 승인 범위의 Gradle 9.7 호환성 수정만 수행한다.
     repository allowlist, dependency verification, workflow 권한, credential 경계는
     변경하지 않으며 범위 밖 plugin/dependency 변경이 필요하면 중단한다.
2. **JDK 25 기본값이 `virtualthread/jdk21` bytecode/runtime 계약을 덮어씀**
   - 해당 모듈의 Java/Kotlin toolchain과 `options.release=21`을 유지한다.
   - JDK 21과 JDK 25 모듈 테스트를 `--no-daemon --rerun-tasks --info`로 별도
     순차 실행해 실제 launcher/toolchain 경로를 확인한다.
   - fresh compile 산출물의 classfile major version이 JDK 21은 65, JDK 25는 69인지
     `javap -verbose`로 확인한다.
   - 일반 Kotlin 모듈과 Java source가 있는 일반 모듈도 toolchain-derived JVM
     target/release가 25인지 compiler task와 classfile major 69로 확인한다.
3. **일부 workflow에 JDK 21 또는 Kotlin 2.3 검사가 남음**
   - workflow와 build 문서를 제한된 패턴으로 재검색하고 `actionlint`로 구문을 검사한다.
   - workflow diff는 JDK env/setup 이름과 CodeQL catalog 값만 허용한다. action ref,
     `permissions`, trigger, secret/environment, fork, `pull_request_target` 경계가
     바뀌지 않았는지 별도 diff 검사를 수행한다.
   - CodeQL은 중앙 catalog 확인값 `2.4.0`을 그대로 build에 사용해야 하며,
     workflow-local `2.3.21` 치환이나 검사는 남아 있으면 실패로 처리한다.
4. **로컬 jenv가 JDK 25를 해석하지 못함**
   - `.java-version`, `jenv version`, `jenv versions`를 확인하고, 설치된 JDK 25에서
     Gradle을 직접 실행해 설정 문제와 설치 문제를 구분한다.
   - `jenv exec java -XshowSettings:properties -version`으로 `java.vendor`,
     `java.runtime.version`, `java.home`을 기록한다. CI distribution은 기존
     `temurin`을 유지한다.
5. **Wrapper 일부 파일만 갱신됨**
   - Wrapper task를 두 번 실행하고 네 파일의 diff, HTTPS `distributionUrl`,
     `distributionSha256Sum`, 공식 wrapper JAR SHA-256 및 `./gradlew --version`을
     확인한다.

## 검증 설계

설정 변경은 production 함수 추가가 아니므로 별도 단위 테스트 대신 재실행 가능한
계약 검사를 RED/GREEN 증거로 사용한다.

- RED: 기존 파일에서 Kotlin 2.3, JDK 21, Gradle 9.6.0을 확인한다.
- GREEN: 변경 후 제한된 검색으로 기본 설정이 2.4/25/9.7.0인지 확인한다.
- `jenv exec java -XshowSettings:properties -version`
- `./gradlew --stop` 후 `jenv exec ./gradlew --no-daemon --version`
- `jenv exec ./gradlew --no-daemon javaToolchains`
- `jenv exec ./gradlew --no-daemon projects --no-configuration-cache --console=plain`
- `jenv exec ./gradlew --no-daemon --rerun-tasks build -x test --no-configuration-cache`
- `jenv exec ./gradlew --no-daemon --rerun-tasks detekt --no-configuration-cache`
- 위 고정 build/detekt matrix의 wall-clock 결과를 기록해 전환으로 인한
  명백한 구성·daemon 회귀가 없는지 검토한다. 이 이슈는 성능 최적화가 아니므로
  별도 peak-memory 또는 정량 회귀 threshold는 완료 조건으로 두지 않는다.
- `jenv exec ./gradlew --no-daemon --rerun-tasks :bluetape4k-virtualthread-jdk21:test --info`
- `jenv exec ./gradlew --no-daemon --rerun-tasks :bluetape4k-virtualthread-jdk25:test --info`
- 양쪽 모듈의 대표 classfile을 `javap -verbose`로 읽어 major version 65/69 확인
- `:bluetape4k-core:compileKotlin`과 `:bluetape4k-resilience4j:compileJava`의
  toolchain/compiler 인자를 확인하고 대표 classfile major version 69 확인
- mock server 두 모듈의 test를 순차 실행하고, 각각
  `jibDockerBuild --no-configuration-cache`를 순차 실행해 JDK 25 base image와
  container build를 검증한다.
- `actionlint .github/workflows/*.yml`을 실행한다. 도구가 없으면 사용 가능한 동등
  YAML parser로 전체 파일을 검사하고 대체 명령과 결과를 DoD에 기록한다.
- workflow diff allowlist와 action ref/permissions/trigger/secret 경계 보존 검사
- 제한된 검색으로 위 문서 변경 경계가 Java 25/Kotlin 2.4/Gradle 9.7.0 및
  JDK 21 예외 계약을 파일별 기준대로 반영하는지 확인한다. 과거 기록과 Java 21
  기능 설명은 검색 결과를 개별 분류해 현재 baseline 오기만 실패로 처리한다.
- `git diff --check`
- 최종 diff에서 P0/P1 검토 결과 0건

PR 전달 후 GitHub-hosted workflow 실행은 exact head 기준으로 검증한다. GitHub
managed dependency submission은 JDK 21로 실행되어 JDK 25 build logic과
충돌하므로 repository-owned JDK 25 workflow로 대체하고, live setting에서
managed action이 비활성화됐음을 확인한다.
향후 PR 전달 시 변경된 CI, examples, CodeQL, security, manual-docs, nightly-tests,
snapshot, release workflow의 실행 또는 해당 trigger에 맞는 check 결과를 확인하고,
모든 실행이 JDK 25 설정 단계와 후속 Gradle task를 성공한 때에만 이 항목을 완료로
전환한다. 특히 CodeQL `java-kotlin` job 로그에서 CLI/bundle 2.26.0 이상과 Kotlin
2.4.0 extraction/analyze 성공을 확인해야 하며, 미달하거나 실패하면 local pin을
되살리지 않고 현재 extractor 경계를 다시 조사한다.

## 완료 조건

- #1323의 Kotlin 2.4, JDK 25, Gradle 9.7.0 완료 기준이 모두 fresh 증거로 확인된다.
- `.java-version`이 jenv에서 JDK 25를 선택한다.
- Wrapper distribution/JAR가 공식 Gradle 9.7.0 checksum과 일치한다.
- JDK 21 전용 모듈 예외가 보존되고 문서와 설정이 모순되지 않는다.
- 변경 파일이 승인 범위에 한정된다.
- PR·merge·release 없이 검증된 로컬 feature branch 상태로 전달한다.
