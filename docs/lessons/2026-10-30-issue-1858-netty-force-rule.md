# Issue #1858: 중앙 catalog와 Netty 강제 규칙의 버전 경계

## 현상

`data/hibernate-reactive/build.gradle.kts`가 `netty-tcnative`를 제외한 모든
`io.netty` 모듈에 `4.2.15.Final`을 강제로 적용하고 있었다. 중앙 `bt4k`
catalog의 Netty 버전과 Vert.x 5가 요청한 `4.2.17.Final`이 있어도
`compileClasspath`와 `runtimeClasspath` 모두 취약 범위인 `4.2.15.Final`을
`Selected by rule`로 선택했다.

라이브 Dependabot alert #230은 `io.netty:netty-handler`의
`CVE-2026-75595`에 대해 `4.2.17.Final`을 최초 수정 버전으로 지정한다.

## 원인

Vert.x 5 호환성을 유지하려는 전역 resolution rule이 호환성 기준인
`4.2.x`와 취약한 특정 패치 버전 고정을 혼동했다. Version catalog는
중앙 버전 소유권을 제공하지만, 하드코딩된 `useVersion`은 그 값을 덮어쓴다.

## 결정

- 비-tcnative Netty 모듈은 `bt4k.versions.netty`의 중앙 catalog 버전을
  사용한다.
- `netty-tcnative*`는 별도 2.0.x 계열을 유지한다.
- 중앙 catalog 버전의 실제 선택 결과는 compile/runtime
  `dependencyInsight`로 확인한다.
- 모듈 테스트는 런타임 `netty-handler`가 Netty 4.2 계열의 첫 수정 패치
  버전(`4.2.17.Final`) 이상인지 검증한다.

## 검증

- 수정 전 회귀 테스트: `netty-handler` 버전 조건이 `false`로 실패했다.
- 수정 후 `NettyDependencyVersionTest`: 1개 테스트 통과.
- `:bluetape4k-hibernate-reactive:test`: 77개 테스트 통과.
- compile/runtime `dependencyInsight`: 비-tcnative Netty `4.2.19.Final`.
- compile/runtime `dependencyInsight`: `netty-tcnative-classes` `2.0.83.Final`.
- publication POM 및 Gradle metadata 생성 성공.
- alert #230의 hosted 상태와 dependency-submission 결과는 merge 후
  exact SHA에서 별도로 다시 확인해야 한다.

## 향후 지침

호환성 때문에 전이 의존성을 정렬할 때 특정 취약 패치 버전을 직접
하드코딩하지 않는다. 중앙 catalog의 버전 authority를 사용하고,
`netty-tcnative`처럼 별도 version line을 가진 모듈은 이름 경계와
compile/runtime 그래프를 함께 검증한다.
