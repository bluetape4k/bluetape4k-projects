# 반복 SNAPSHOT publication을 기능 handoff에서 분리하기

## 맥락

이슈 #1578의 Snapshot source identity 검토에서 반복적인 SNAPSHOT publication과
기능 handoff를 같은 workflow에 묶어 두었던 경계를 재평가했다. 반복 publication은
검증된 기능을 다음 담당자에게 넘기는 단계가 아니라, 사용자가 실행을 승인한
`workflow_dispatch`의 source를 그대로 배포하는 운영 동작이어야 한다.

## 결정

- `Publish Snapshot`은 입력이 없는 `workflow_dispatch` 전용 workflow로 유지한다.
- 단일 `publish` job이 `ref: ${{ github.sha }}`와 `fetch-depth: 0`으로 dispatch SHA를
  checkout한다.
- publication 직전에 `git rev-parse HEAD`를
  `EXPECTED_HEAD_SHA=${{ github.sha }}`와 비교해 실제 source identity를 증명한다.
- 마지막 summary에는 `workflow_dispatch`, `SOURCE_SHA=${{ github.sha }}`,
  `repeated publication allowed`를 기록한다.
- 따라서 routine repeated Snapshot은 dispatch SHA를 사용하며 feature handoff가
  아니다.

## 유지한 경계

- 보호된 `maven-central-release` environment와
  `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `SIGNING_KEY_ID`, `SIGNING_KEY`,
  `SIGNING_PASSWORD` secrets
- `actions/checkout`, `actions/setup-java`, `gradle/actions/setup-gradle`의
  immutable commit SHA pins
- 기존 publication metadata validation과 exact Maven Snapshot publication command
- stable Release workflow의 exact-candidate 검증 경계

## 제거한 결합

- 수동 `verified_ci_run_id`, `expected_head_sha`, `handoff_issue_number`,
  `validation_run_id` 입력과 validation run/current `develop` SHA 전달
- `#1562` issue handoff와 `validate-full-nightly`, `record-handoff` job
- TenantContext receipt 생성·artifact upload·issue comment machinery

## 재발 방지

Snapshot policy test는 job이 `publish` 하나인지, trigger가 `workflow_dispatch`만인지,
입력·issue handoff·receipt machinery가 없는지, checkout과 검증·summary가 같은
dispatch SHA를 가리키는지를 검사한다. 안정 release의 exact-candidate 검증과
Snapshot의 반복 publication은 각각의 목적과 승인 경계를 유지한다.
