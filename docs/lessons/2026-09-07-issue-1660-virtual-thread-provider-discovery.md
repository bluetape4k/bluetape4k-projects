# VirtualThreadRuntime provider 탐색 격리 교훈

## 문제

`VirtualThreads`는 `ServiceLoader` provider 하나가 깨져도 다음 후보를 계속 탐색한다고 문서화했지만,
기존 loop는 `hasNext()`와 `next()`를 하나의 `runCatching`으로 묶었다. `next()`가
`ServiceConfigurationError`를 던지면 종료 조건과 구분되지 않아 뒤의 유효 provider가 가려지고
프로세스 수명 동안 `platform-fallback` 선택이 고정될 수 있었다.

## 선택

- 동일 저장소의 `StructuredTaskScopes` discovery 경계를 그대로 따른다.
- `hasNext()` 실패는 iterator 자체를 더 신뢰할 수 없으므로 원인을 로그하고 탐색을 종료한다.
- `next()` 실패는 해당 service entry만 건너뛰고 다음 provider를 계속 탐색한다.
- `isSupported()` 실패도 provider 단위로 격리한다.
- 지원되는 provider만 수집한 뒤 기존 계약대로 priority 내림차순으로 정렬한다.
- 새 범용 abstraction은 만들지 않고 `VirtualThreads` 내부 helper로 테스트 seam만 분리한다.

## JDK matrix

`virtualthread/api` 변경은 Java 21 compatibility island와 Java 25 구현 모두에 영향을 준다.
따라서 API의 broken-next/unsupported/failed-check/failed-hasNext 회귀뿐 아니라 각 모듈의 실제
`META-INF/services` entry가 `jdk21`, `jdk25` runtime을 선택하는 통합 테스트를 함께 유지한다.

## 검증

- RED: `discoverVirtualThreadRuntimes`가 없어 신규 회귀 테스트 compile 실패
- `bluetape4k-virtualthread-api`: 74 passed, failures/errors/skipped 0
- `bluetape4k-virtualthread-jdk21`: 30 passed, failures/errors/skipped 0
- `bluetape4k-virtualthread-jdk25`: 44 passed, failures/errors/skipped 0
- 세 모듈 `check`, Kover verify/XML, detekt: 성공

## Review Miss

실패를 한 `runCatching`으로 감싸면 예외를 잡았다는 사실만 보이고, 실패 지점마다 다른 복구 정책이
사라질 수 있다. 반복 탐색에서는 iterator 상태를 잃은 실패와 개별 element 생성·검증 실패를 분리해야
뒤의 정상 entry를 안전하게 보존할 수 있다.

## 다음 변경 시 지킬 점

1. `hasNext`, `next`, provider capability 검사를 하나의 실패 경계로 합치지 않는다.
2. 개별 provider 실패 로그에는 secret-bearing 설정값 대신 provider 유형과 단계만 남긴다.
3. discovery 변경은 API unit test와 JDK 21/25 실제 service integration을 함께 검증한다.
