# CodeQL에서 `bluetape4k-testcontainers`를 제외하는 이유

## 배경

Issue #999는 `:bluetape4k-testcontainers:compileKotlin`을 CodeQL Java/Kotlin
수동 build에 다시 포함할 수 있는지 검증했다. 일반 Gradle 환경에서는 이
모듈이 정상적으로 컴파일되지만, CodeQL의 compiler tracing 환경에서는 단일
task도 scheduled analysis의 실용적인 시간 안에 끝나지 않았다.

## 근거

- 과거 run
  [28970095586](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/28970095586)은
  단일 `:bluetape4k-testcontainers:compileKotlin` task가 31분 넘게 build 단계에
  머문 뒤 취소됐다.
- 중앙 catalog pin을 수정한 통제 run
  [29648755583](https://github.com/bluetape4k/bluetape4k-projects/actions/runs/29648755583)은
  같은 task에 20분 step timeout을 적용했다. task는 2026-07-18 14:54:00 UTC에
  시작했지만 15:09:29 UTC까지 완료되지 않아 build가 timeout됐다.
- 같은 exact source에서 `--rerun-tasks --no-configuration-cache`로 실행한 로컬
  compile은 cache 없이 19초에 성공했다. 따라서 일반 Gradle 성능 문제가 아니라
  CodeQL tracing 환경에서만 나타나는 증폭이다.
- 같은 run의 다른 scope는 모두 성공했다. Java/Kotlin scope의 전체 job 시간은
  `testing-core` 6분 44초, `infra` 9분 13초, `data-io` 11분 28초,
  `foundation` 11분 33초, `frameworks` 12분 6초였다.
- `testing/testcontainers` main source는 68개 파일, 10,993줄이며
  `build.gradle.kts`에 107개의 dependency declaration이 있다. 여러 DB,
  메시징, 검색, cloud SDK를 하나의 optional integration module에 모은 넓은
  compile classpath가 Kotlin extractor tracing 비용을 크게 증폭한다.

## 판단

CodeQL은 [compiled language의 manual build](https://docs.github.com/en/code-security/how-tos/find-and-fix-code-vulnerabilities/manage-your-configuration/codeql-for-compiled-languages)에서
실제 compiler invocation을 추적한다. 이 모듈은 소스 크기 자체보다 넓은 optional integration classpath와
Kotlin extractor tracing의 결합 때문에 일반 Gradle compile과 전혀 다른
시간 특성을 보인다. 특정 library 하나가 원인이라는 증거는 없으므로 dependency를
임의로 제거하거나 분석 시간을 무제한으로 늘리지 않는다.

따라서 `testing/testcontainers`는 scheduled CodeQL matrix에서 계속 제외한다.
나머지 production scope의 분석을 안정적으로 완료하는 것이 우선이며, 이 결정은
소스/의존성 구조나 CodeQL Kotlin extractor 성능이 유의미하게 바뀔 때만 다시
검토한다.

## 재검토 조건

다음 조건을 모두 충족할 때 별도 issue와 단 한 번의 bounded dispatch로 재검증한다.

1. `testing/testcontainers`가 backend별 module로 분리되거나 compile classpath가
   의미 있게 축소됐다.
2. CodeQL release note에 Kotlin extractor의 Gradle tracing 성능 개선이 명시됐다.
3. 비교 run은 모듈 단일 `compileKotlin` task와 20분 이하 timeout을 유지한다.
4. 다른 matrix scope의 성공 여부와 exact head SHA를 함께 기록한다.

## 검증 규칙

`scripts/test_codeql_workflow_policy.py`는 이 exclusion의 run 링크와 lesson 경로를
workflow에 남기고, `testing-containers` scope가 실수로 다시 추가되지 않도록
검사한다.
