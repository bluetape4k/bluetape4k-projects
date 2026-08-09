# 이슈 #1323 빌드 기준 전환 교훈 (2026-08-10)

## 맥락

저장소 기본 빌드 기준을 Kotlin 2.4, JDK 25, Gradle 9.7.0으로 올리면서
`virtualthread/jdk21`은 Java 21 호환성을 계속 제공해야 했다. 루트 기본값만
JDK 25로 바꾸고 이 모듈 하나만 Java 21로 덮어쓰는 방식은 Gradle variant
matching 단계에서 실패했다.

## 결정과 발견

1. 하위 호환 모듈의 JVM target은 그 모듈 하나의 설정이 아니라 project
   dependency closure의 variant 계약이다. `virtualthread/jdk21`이 소비하는
   `virtualthread-api`, `logging`과 test dependency인 `assertions`, `junit5`까지
   최소 Java 21 compatibility island로 함께 유지한다.
2. 루트 Kotlin DSL의 `pluginManager.withPlugin` 블록에서 호환성 목록을 조회할
   때는 암시적 receiver의 `name`에 기대지 않고 `project.name`을 사용한다.
   단순 `name`은 예상과 다른 receiver로 해석되어 Java 21 예외가 적용되지
   않았고, 같은 variant resolution 실패를 반복했다.
3. Java와 Kotlin의 산출물 계약을 각각 `JavaCompile.options.release`와
   Kotlin `jvmTarget`으로 명시한다. toolchain 선택만으로 classfile 호환성을
   추론하지 않는다.
4. Kotlin 2.4에서 안정화된 context parameters와 새 annotation default target
   규칙은 기존 실험 옵션과 같은 동작을 제공한다. 실험 옵션을 제거할 때는
   공식 안정화 근거와 의도를 빌드 설정에 남겨 의미 변경으로 오해되지 않게 한다.
5. JDK 25로 실행되는 `core`, `workflow`, virtual-thread example은 JDK 25
   provider를 runtime dependency로 선택해야 한다. 반대로 `virtualthread/api`는
   Java 21 compatibility island의 consumer이므로 JDK 25 provider를 연결하면
   Gradle variant matching이 실패하며 JDK 21 provider를 유지해야 한다.
6. GitHub-managed Automatic Dependency Submission은 저장소 workflow에서
   실행 JDK를 지정할 수 없고 이 저장소에서는 JDK 21로 Gradle을 실행해 JDK 25
   build logic과 충돌했다. repository-owned `dependency-submission.yml`에
   Temurin JDK 25를 명시하고 managed submission setting을 별도로 비활성화해야
   한다.

## 결과

- 기본 모듈은 Java/Kotlin classfile major 69(JVM 25)를 생성한다.
- Java 21 compatibility island 다섯 모듈은 major 65를 유지한다.
- `virtualthread/jdk21`과 `virtualthread/jdk25` 테스트는 각각 실제 JDK 21과
  JDK 25 launcher로 실행된다.
- CodeQL workflow의 Kotlin 2.3.21 임시 catalog 치환을 제거하고 중앙 Kotlin
  2.4 설정을 그대로 사용한다.

## 검증

초기 구현 wave에서 다음 baseline을 확인했다.

- Gradle 9.7.0/JDK 25에서 표적 Java/Kotlin compile PASS
- 전체 `build -x test` PASS, 656 tasks
- mock web server 두 모듈 test와 Jib Docker build PASS
- wrapper checksum PASS

이번 CI 보수 wave에서는 feature worktree에서 다음을 새로 확인했다.

- `:bluetape4k-examples-virtualthreads-demo:test`: `BUILD SUCCESSFUL`,
  `gradle_exit=0`
- `:bluetape4k-core:test`: 125 tests 실행 성공, `BUILD SUCCESSFUL`,
  `gradle_exit=0`
- `:bluetape4k-workflow:test`: `BUILD SUCCESSFUL`, `gradle_exit=0`
- `:bluetape4k-virtualthread-api:test`: Java 21 provider로
  `BUILD SUCCESSFUL`, `gradle_exit=0`
- `:bluetape4k-virtualthread-jdk21:test`와
  `:bluetape4k-virtualthread-jdk25:test`: 각각 `BUILD SUCCESSFUL`,
  `gradle_exit=0`
- `actionlint .github/workflows/*.yml`, 중앙 catalog/release policy 11개,
  `git diff --check`: PASS

이전 PR head의 hosted CI에서는 `Test Examples`가 JDK 25에서 JDK 21 provider를
소비해 3개 StructuredTaskScope 테스트를 실패했고
(`31324376319/93272283919`), GitHub-managed `submit-gradle`은 JDK 21로
`buildSrc`를 실행해 실패했다(`31324372759/93272269283`). 새 head의 hosted
CI와 managed submission setting은 push 후 exact head에서 재검증해야 하므로
아직 완료로 표시하지 않는다.

설계 경계와 전체 명령은
[`2026-08-09-issue-1323-build-standard-design.md`](../superpowers/specs/2026-08-09-issue-1323-build-standard-design.md)와
[`2026-08-09-issue-1323-build-standard-plan.md`](../superpowers/plans/2026-08-09-issue-1323-build-standard-plan.md)에 남겼다.

## 검토에서 놓친 점

최초 구현 계획은 `virtualthread/jdk21`만 Java 21 예외로 보는 가정을 충분히
검증하지 못했다. 실제 Gradle variant resolution 실패가 dependency closure를
드러냈으며, 이후 최소 island를 다시 계산하고 API는 JDK 21 provider, JDK 25
consumer는 JDK 25 provider를 선택하도록 분리했다. 또한 GitHub-managed
dependency submission이 저장소의 JDK 25 build logic과 독립적으로 JDK 21을
사용한다는 hosted CI 경계를 초기 계획에서 놓쳤다.

## 향후 지침

- JVM 호환성 예외를 변경할 때는 project dependency closure, classfile major,
  실제 test launcher를 한 세트로 검증한다.
- provider를 바꿀 때는 consumer의 Gradle Java variant와 실행 JDK를 함께
  확인하고, compatibility island 경계를 넘는 provider를 연결하지 않는다.
- GitHub-managed workflow의 실행 환경을 저장소 YAML로 추측하지 말고 실패
  로그의 JDK와 workflow ownership을 확인한 뒤 repository-owned workflow로
  대체할지 결정한다.
- Kotlin DSL의 중첩 receiver 안에서 project identity를 참조할 때는
  `project.name`처럼 receiver를 명시한다.
- Kotlin language version을 올릴 때 제거하는 실험 옵션은 공식 compatibility
  guide에서 기본 동작과 동일함을 확인하고 근거를 함께 기록한다.
