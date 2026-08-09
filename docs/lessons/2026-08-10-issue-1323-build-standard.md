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

## 결과

- 기본 모듈은 Java/Kotlin classfile major 69(JVM 25)를 생성한다.
- Java 21 compatibility island 다섯 모듈은 major 65를 유지한다.
- `virtualthread/jdk21`과 `virtualthread/jdk25` 테스트는 각각 실제 JDK 21과
  JDK 25 launcher로 실행된다.
- CodeQL workflow의 Kotlin 2.3.21 임시 catalog 치환을 제거하고 중앙 Kotlin
  2.4 설정을 그대로 사용한다.

## 검증

- Gradle 9.7.0/JDK 25에서 표적 Java/Kotlin compile PASS
- Java 21 compatibility island와 JDK 21/25 virtual-thread test PASS
- 전체 `build -x test` PASS, 656 tasks
- mock web server 두 모듈 test와 Jib Docker build PASS
- `detekt`, `actionlint`, wrapper checksum, `git diff --check` PASS
- 독립 최종 diff review: P0/P1/P2/P3 모두 0건

설계 경계와 전체 명령은
[`2026-08-09-issue-1323-build-standard-design.md`](../superpowers/specs/2026-08-09-issue-1323-build-standard-design.md)와
[`2026-08-09-issue-1323-build-standard-plan.md`](../superpowers/plans/2026-08-09-issue-1323-build-standard-plan.md)에 남겼다.

## 검토에서 놓친 점

최초 구현 계획은 `virtualthread/jdk21`만 Java 21 예외로 보는 가정을 충분히
검증하지 못했다. 실제 Gradle variant resolution 실패가 dependency closure를
드러냈으며, 이후 최소 island를 다시 계산하고 표적 compile과 actual launcher
검증을 추가했다. 또한 Kotlin 2.4 실험 옵션 제거는 최종 리뷰에서 의미 변경
가능성이 제기된 뒤에야 공식 안정화 근거를 명시했다.

## 향후 지침

- JVM 호환성 예외를 변경할 때는 project dependency closure, classfile major,
  실제 test launcher를 한 세트로 검증한다.
- Kotlin DSL의 중첩 receiver 안에서 project identity를 참조할 때는
  `project.name`처럼 receiver를 명시한다.
- Kotlin language version을 올릴 때 제거하는 실험 옵션은 공식 compatibility
  guide에서 기본 동작과 동일함을 확인하고 근거를 함께 기록한다.
