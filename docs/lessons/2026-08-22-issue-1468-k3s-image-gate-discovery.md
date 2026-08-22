# Issue #1468 K3s image gate test-discovery lesson

## 배경

PR #1467 exact head `bcdbf1707d8eddce09f1dfd8f30cd22ccaac958c`의 hosted
image gate run `32488642030`은 정상 family 51개를 통과시켰지만 K3s를
`infrastructure_failure`로 보고했습니다. K3s attempt의 실제 Gradle 오류는
`No tests found for given includes: [io.bluetape4k.testcontainers.infra.K3sServerTest](--tests filter)`였습니다.

## 원인과 예상 밖의 점

`K3sServerTest`는 privileged runner 전용 `@Tag("k8s")` 테스트입니다. 기본
`:bluetape4k-testcontainers:test` task는 이 태그를 제외하고, 저장소는 이미 전용
`:bluetape4k-testcontainers:k8sTest` task를 제공합니다. image gate manifest는
test class만 선언하고 실행 task를 표현하지 않아 K3s 테스트를 발견하지 못했습니다.

같은 Gradle stdout에는 선행 Jib task의 정상 진행 문구
`building image to Docker daemon`가 포함됐습니다. 실패 분류가 전체 출력의
`docker daemon` substring을 먼저 사용하면서 결정적인 test-discovery 실패를
인프라 장애로 덮었습니다. 진단 artifact의 Docker event와 종료 코드는 현재 Gradle
invocation이 K3s 테스트를 실행했다는 증거가 아니었습니다.

## 결정

- family별 예외 task가 필요할 때 manifest의 선택적 `testTask`가 기존 Gradle task
  path를 지정하게 합니다.
- K3s entry는 기존 `:bluetape4k-testcontainers:k8sTest`를 재사용합니다.
- `Execution failed for task`와 `No tests found for given includes`가 함께 나타나는
  결정적인 Gradle/test-discovery 실패는 일반적인 Docker 출력 marker보다 먼저
  `product_failure`로 분류합니다.
- manifest의 선택적 task는 검증된 allowlist에 있는 기존 task만 허용합니다.
- 실제 K3s container 검증은 privileged hosted runner에서 순차 실행합니다.

## 검증

- 회귀 테스트 3건: task override, test-discovery 분류 우선순위, K3s manifest task를
  각각 RED로 재현하고 GREEN으로 전환했습니다.
- 잘못되거나 알려지지 않은 `testTask` manifest 값: RED/GREEN으로 task path와
  allowlist 검증을 고정했습니다.
- incidental test-discovery 문구와 Docker 인프라 오류의 충돌 회귀도 고정했습니다.
- runner/manifest Python 테스트: 19건 PASS
- `k8sTest --tests K3sServerTest --dry-run`: `BUILD SUCCESSFUL`, 전용 task 선택 확인
- hosted full image gate `52/52`: PENDING

## 향후 지침

manifest 기반 실행기는 test class만으로 충분하다고 가정하지 않습니다. 테스트가
JUnit tag, 별도 source set, 전용 Gradle task, profile/property로 격리되면 그 실행
경계를 manifest와 회귀 테스트에 함께 선언해야 합니다. 실패 분류는 구체적인 현재
invocation 오류를 먼저 판정하고, 선행 task나 진단 출력에 흔히 나타나는 일반
substring은 그 뒤에 적용합니다.

## SPW 감사

- SPW-01: PASS — Issue #1468, run `32488642030`, exact head, source/task와 실제 오류를 고정했습니다.
- SPW-02: PASS — 배경, 원인, 예상 밖의 점, 결정, 검증, 향후 지침을 포함했습니다.
- SPW-03: PASS — 한국어 기술 문체를 사용하고 Gradle/JUnit 식별자와 exact error를 보존했습니다.
- SPW-04: PASS — hosted artifact, `build.gradle.kts`, manifest, runner와 회귀 테스트를 대조했습니다.
- SPW-05: PASS — 최종 Markdown, terminology audit, `git diff --check`를 read-back합니다.
