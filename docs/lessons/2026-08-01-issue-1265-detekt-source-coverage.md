# 이슈 #1265 Kotlin 서브프로젝트 Detekt source coverage

## 배경

기존 `./gradlew detekt`는 root 프로젝트에 Kotlin source가 없어 `:detekt NO-SOURCE`로
성공했다. 실제 library Kotlin source가 subproject에 있다는 사실을 task 결과가 검증하지
못했으므로 Nightly static-analysis gate가 false-green 상태였다.

## 결정

- 실제 library 분석 대상 subproject에만 `dev.detekt` plugin과 `detekt` task를 등록한다.
- examples·demo·benchmark·workshop source와 metadata-only/umbrella project는 명시적인
  사유와 함께 제외한다. 문서화된 `exposed-jdbc-tests` 예외도 receipt에 기록한다.
- root `detektSourceCoverage` task가 대상별 main/test Kotlin file 수를 계산하고, 대상
  module의 source가 하나라도 비어 있으면 실패한다.
- root `detekt`는 module task를 orchestration하고 `detektReportMerge`가 Checkstyle XML을
  `build/reports/detekt/merged.xml`로 병합한다. 분석 범위 영수증은
  `build/reports/detekt/source-coverage.md`에 저장한다.
- 현재 저장소의 기존 Detekt rule finding 정리는 이 이슈의 source-coverage 범위를 넘는다.
  finding은 module/merged report에 보존하고, source가 비어 있는 경우의 실패 보장은
  별도의 guard로 유지한다.

Detekt 2.0.0-alpha.5는 현재 repository Kotlin 2.4 toolchain과 호환되므로 legacy
`io.gitlab.arturbosch.detekt` plugin 대신 `dev.detekt` plugin과 `checkstyle` report API를
사용한다.

## 검증

실제 worktree에서 다음 명령을 실행했다.

```bash
./gradlew detekt --rerun-tasks --no-configuration-cache --console=plain
```

결과:

- `BUILD SUCCESSFUL`
- 분석 대상 75개 module
- main 1,945개 + test 2,156개 = Kotlin source 4,101개
- empty included module 0개
- module Checkstyle XML 75개와 root merged XML 1개 생성
- root task output은 module task를 집계하는 `SKIPPED` orchestration boundary이며, 실제
  source scope 로그는 각 module task에서 출력된다.

Nightly workflow는 source-coverage receipt를 검사하고 XML/receipt를
`nightly-detekt-report` artifact로 보관한다.

## 향후 방지책

Detekt task를 추가하거나 분석 제외 범위를 바꿀 때는 `source-coverage.md`의 module 수,
source file 수, `Empty included modules: 0`을 함께 확인한다. 새로운 rule cleanup은
merged report의 finding을 기준으로 별도 issue에서 좁은 범위로 처리한다.

## 참고

- Detekt Gradle plugin: https://detekt.dev/docs/gettingstarted/gradle/
- Detekt 2.0 migration: https://detekt.dev/docs/introduction/migration/
