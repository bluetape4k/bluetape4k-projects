# Nightly 자동 SNAPSHOT의 source identity를 고정하기

## 맥락

이슈 #1578을 진행하면서 반복적인 SNAPSHOT publication이 TenantContext 기능
handoff 절차에 묶였다. 이 결합 때문에 `Nightly`가 성공해도 별도 run ID와 issue
번호를 입력해야 했고, routine Snapshot이 자동으로 이어지지 않았다.

자동 실행만 복원하면 source identity가 다시 약해질 수 있다. `workflow_run`으로
시작한 workflow에서 `github.sha`는 triggering Nightly의 SHA를 뜻하지 않으므로,
publication source를 `github.event.workflow_run.head_sha`로 명시해야 한다.

## 결정

- `develop`의 `Nightly`가 완료되면 `Publish Snapshot`을 자동으로 시작한다.
- `publish` job은 Nightly 결론이 `success`일 때만 실행한다. `failure`와
  `cancelled`는 publication으로 이어지지 않는다.
- 운영자가 다시 실행할 수 있도록 입력 없는 `workflow_dispatch`도 유지한다.
- 자동 실행은 `github.event.workflow_run.head_sha`, 수동 실행은 `github.sha`를
  checkout한다.
- checkout 직후 `git rev-parse HEAD`를 같은 조건식의 `EXPECTED_HEAD_SHA`와
  비교한다.
- summary에는 trigger와 실제 `SOURCE_SHA`를 기록한다.

## 유지한 경계

- `maven-central-release` environment와 `CENTRAL_USERNAME`,
  `CENTRAL_PASSWORD`, `SIGNING_KEY_ID`, `SIGNING_KEY`, `SIGNING_PASSWORD`
  secrets
- `actions/checkout`, `actions/setup-java`, `gradle/actions/setup-gradle`의
  immutable commit SHA pins
- publication metadata validation과 exact Maven Snapshot publication command
- stable Release workflow의 exact-candidate 검증 경계

environment reviewer 제거는 workflow 변경과 별개의 GitHub 설정 변경이다. 이
변경이 merge된 뒤 별도 상태 변경 경계에서 처리해야 Nightly 성공 직후 runner가
대기 없이 publication을 수행한다.

## 제거한 결합

- `verified_ci_run_id`, `expected_head_sha`, `handoff_issue_number` 입력
- `#1562` 전용 `validate-full-nightly`, `record-handoff` job
- TenantContext receipt 생성, artifact upload, issue comment

## 재발 방지

Snapshot policy test는 두 trigger, 성공 결론 guard, source SHA 조건식, 단일
`publish` job, 입력과 기능 handoff의 부재를 함께 검사한다. 자동화 여부와 source
identity를 따로 판단하지 않고 하나의 계약으로 검증한다.
