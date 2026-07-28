# 이슈 #926 Protobuf CI coverage

## 배경

Protobuf는 IO serialization module이고 최근 protobuf codec 변경에도 module test가
포함되었지만, push CI는 `io/protobuf/**` 변경에서 `Test / IO`를 건너뛰었다.

## 결정

기존 `io` path-filter output에 `io/protobuf/**`를 추가하고, 기존 `Test / IO` test와
Kover task list에 `:bluetape4k-protobuf`를 포함한다.

## 근거

- Protobuf는 JSON, Jackson, gRPC, Tink 같은 인접 IO serialization module과 같은 그룹에 속한다.
- 별도 protobuf job은 정당화할 만큼 failure isolation을 개선하지 못한 채 workflow fanout만 늘린다.
- `test-io`를 재사용하면 기존 `coverage-report`와 `ci-status` wiring을 보존한다.

## 검증

- `actionlint .github/workflows/ci.yml`
- Backslash-single-quote guard for GitHub Actions expressions
- `git diff --check`
- Gradle project/task wiring dry-run
- `:bluetape4k-protobuf:test`
- `:bluetape4k-protobuf:koverXmlReport`

## 향후 방지책

IO module을 추가하거나 이동할 때는 path-filter pattern, Gradle test task list, Kover
task list를 같은 PR에서 동기화한다.
