# 이슈 475 Core Null Assertion 제거

## 배경

Issue #475는 production source의 Kotlin `!!`를 작은 module slice로 제거한다. 첫 slice는
`:bluetape4k-core`만 다뤘다. Issue가 86개 occurrence를 한 PR에서 모두 바꾸지 말라고 요구했기 때문이다.

## 결정

Public exception contract를 바꾸지 않고 `!!`를 제거한다. Deprecated `assertXxx` map helper는
`AssertionError`를 유지하고, `requireXxx` map helper는 `IllegalArgumentException`을 유지한다.
KDoc example은 `!!` 대신 Elvis `error(...)`를 사용한다. `SingletonHolder`는 기존 lock 내부의
factory-consumed invariant에만 `checkNotNull`을 사용한다.

## 결과

Core target file은 production source와 example에서 더 이상 `!!`를 포함하지 않는다. Null map receiver
test를 `assertHasKey`, `assertHasValue`, `assertContains`, `requireHasKey`, `requireHasValue`,
`requireContains`에 추가해 기존 exception-type contract를 고정했다.

## 검증

- Issue #475 core target file에 대한 `rg -n '!!'`가 match 0개 반환.
- 수정한 production/test file에 대해 IntelliJ reformat/import optimization 성공.
- Indexing 이후 변경된 map helper API의 IntelliJ reference lookup 성공.
- `./gradlew :bluetape4k-core:compileKotlin :bluetape4k-core:test --console=plain --no-configuration-cache`
  1588 tests로 통과.
- Codex CLI final review: P0/P1 없음.
- Claude CLI review는 반복되는 usage-limit failure 때문에 사용자가 호출하지 말라고 지시해 생략.

## 향후 가드

#475는 module-sized PR로 계속 진행한다. `!!` 제거가 exception-type contract를 건드리면 focused test를
추가하고, broad rewrite 대신 contract-preserving local variable 또는 명시적인 `checkNotNull`/Elvis
failure를 선호한다.
