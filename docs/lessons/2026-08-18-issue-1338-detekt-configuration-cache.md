# 이슈 #1338 - configuration-cache에서 Detekt source coverage 실행 보장

## 배경

기본 `./gradlew detekt --no-daemon --console=plain` 실행이 configuration-cache
재사용 시 `:detektSourceCoverage`에서 실패했다. task action이 Kotlin DSL 최상위
`Project` map을 캡처하고 있었기 때문에 cache entry를 역직렬화한 뒤
`Build_gradle.getDetektSourceFilesByProject()`가 `null`이 되었다. 같은 작업은
`--no-configuration-cache`를 사용하면 통과했지만, CI가 cache를 우회하면 이 회귀를
놓칠 수 있다.

## 결정

`detektSourceCoverage`의 `doLast` 구현을 `buildSrc`의
`DetektSourceCoverageTask`로 옮겼다. task action에는 `Project`나 build-script
객체를 전달하지 않고, 프로젝트 경로·소스 루트·명시적 제외 목록을 선언된
property로 전달한다. 실제 소스는 `@InputFiles` collection에서 실행 시 읽어
configuration-cache 재사용 뒤에도 module별 main/test 개수와 빈 module 검사를
현재 파일 상태에 맞게 계산한다. 보고서 형식과 빈 module fail-closed 계약은
유지하고, renderer 단위 테스트를 추가했다.

야간 Detekt job은 더 이상 `--no-configuration-cache`를 사용하지 않는다.
`--configuration-cache-problems=fail`을 함께 사용해 첫 시도의 configuration-cache
문제를 warning으로 넘긴 뒤 재시도에서 우연히 통과하는 loophole을 차단한다.

## 결과

- 기본 Detekt가 configuration-cache 문제 없이 실행된다.
- 두 번째 실행에서 `Configuration cache entry reused`를 확인하면서도
  `BUILD SUCCESSFUL`을 유지한다.
- source coverage 결과는 기존과 동일하게 75개 module, 4,151개 Kotlin 파일
  (main 1,957, test 2,194), 빈 module 0개를 기록한다.
- 기존 Detekt rule finding은 `ignoreFailures=true` 계약에 따라 보고서에 남지만,
  source coverage task와 configuration-cache 검증은 독립적으로 fail-closed 된다.

## 검증

- RED: 기본 configuration-cache 실행이 script-object 직렬화 오류와
  `Build_gradle.getDetektSourceFilesByProject()` null 오류로 실패했다.
- RED control: `--no-configuration-cache` 실행은 기존 rule finding을 출력하고
  `BUILD SUCCESSFUL`로 종료했다.
- GREEN: `./gradlew :buildSrc:test --no-daemon --console=plain`
- GREEN: `./gradlew detekt --configuration-cache-problems=fail --no-daemon --console=plain`
- CACHE HIT: 같은 Detekt 명령을 두 번째 실행해 `Configuration cache entry reused`,
  `BUILD SUCCESSFUL`, exit 0을 확인했다.
- 정적 검증: `git diff --check`, `actionlint .github/workflows/nightly-tests.yml`

## 향후 가드

configuration-cache 대상 task action에서는 Kotlin DSL 최상위 `Project`,
`Task`, script receiver 또는 그 객체를 key로 가진 collection을 캡처하지 않는다.
새 입력은 `@Input`, `@InputFiles`, `@OutputFile` 같은 task property로 선언하고,
야간 retry loop를 유지할 때는 `--configuration-cache-problems=fail`을 사용해
구성 문제를 재시도 사이에 숨기지 않는다.
