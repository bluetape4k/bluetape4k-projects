# Issue #563: Virtual Thread API 패키지 소유권 경계 명세

## 문제

`bluetape4k-core`와 `bluetape4k-virtualthread-api`가 모두
`io.bluetape4k.concurrent.virtualthread`를 published JAR에 포함하고
있었다. 두 automatic module을 함께 module-path에 놓으면
`java --validate-modules`가 split-package 충돌로 실패한다. graph 저장소의
[#563](https://github.com/bluetape4k/bluetape4k-graph/issues/563)가 이
경계를 추적한다.

## 결정

- `bluetape4k-core`가 `io.bluetape4k.concurrent.virtualthread`의 소유자로
  core utility와 `VirtualFuture` ABI를 계속 제공한다.
- `bluetape4k-virtualthread-api`의 Java 21 호환 타입
  (`VirtualThreads`, `VirtualThreadRuntime`, `StructuredTaskScopes`, scope
  contracts, `TaskContext`)를
  `io.bluetape4k.concurrent.virtualthread.api`로 이동한다.
- JDK21/JDK25 provider import와 ServiceLoader descriptor도 `.api` 계약으로
  갱신한다.
- 기존 패키지에 compatibility bridge를 남기거나 shading으로 충돌을
  감추지 않는다. 두 선택 모두 published package ownership을 모호하게
  만들고 module validation을 다시 깨뜨릴 수 있다.

## 호환성

이는 source/import 및 JVM binary package migration이다. 이전 패키지의 API
class를 직접 참조하는 소비자는 import를 바꾸고 재컴파일해야 한다. API
모듈은 Java 21 target을 유지하고 core의 Java 25 dependency를 끌어오지
않는다. 기존 core utility import는 변경하지 않는다.

## 검증 기준

1. API/JDK21/JDK25/core compile 및 test가 통과한다.
2. 생성된 core와 API JAR이 각각 하나의 owner package만 포함한다.
3. 두 JAR을 module-path에 둔 `java --validate-modules`가 exit 0이다.
4. provider descriptor가 `.api.VirtualThreadRuntime` 및
   `.api.StructuredTaskScopeProvider`를 사용한다.
5. README, CHANGELOG, WIP와 migration 기록이 import 재컴파일 요구를
   설명한다.
