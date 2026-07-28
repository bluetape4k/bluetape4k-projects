# 이슈 937 IO CI coverage

## 배경

CI workflow의 IO path filter가 좁았다. 최근 `io/grpc/**`, `io/http/**`,
`io/jackson2/**`, `io/retrofit2/**` 변경은 관련 module test가 건너뛰어진 채 CI를
통과할 수 있었다.

## 결정

Full repository test fanout을 추가하지 않고 기존 targeted CI lane을 확장한다.

- `Test / IO`: Jackson2, gRPC, Tink path와 대응되는 test/Kover task를 포함한다.
- `Test / IO HTTP`: HTTP, Retrofit2, Vert.x path와 대응되는 test/Kover task를 포함한다.
- `io/protobuf/**`는 별도 open issue #926 범위로 남긴다.

## 결과

Workflow syntax와 Gradle task wiring은 local에서 검증했다. `.github/workflows/ci.yml`이
shared path filter의 일부이므로 live path-filter/job execution proof는 PR CI가 제공할
것으로 본다.

## 향후 지침

- CI path filter를 추가할 때는 matching Gradle test와 Kover task를 같은 변경에 넣는다.
- PR이 명시적으로 닫지 않는 한 별도 backlog issue는 분리해 둔다. 여기서는 protobuf가 #926 범위로 남는다.
- Workflow PR을 열기 전에는 항상 `actionlint`, escaped workflow quote scan, `git diff --check`, Gradle dry-run task validation을 실행한다.
