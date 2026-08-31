# Snapshot publication source identity를 checkout으로 증명하기

## 맥락

이슈 #1578의 `Publish Snapshot` 대기 실행을 검토하면서 workflow run의
`headSha`가 곧 publication source라고 판단했다. 그러나 여러 저장소의
`actions/checkout`은 `ref`를 지정하지 않았고, environment 승인 뒤 runner가
시작될 때의 기본 branch를 checkout했다.

## 잘못된 가정과 증거

- 잘못된 가정: `workflow_run`으로 시작한 run의 `headSha`가 checkout source를
  자동으로 고정한다.
- 반증: AWS와 Exposed의 triggering Nightly SHA는 대기 중인 Publish Snapshot
  run이 실제 checkout할 최신 `develop` SHA와 달랐다.
- 결과: reviewer를 제거하면 Nightly가 검증하지 않은 소스를 배포할 수 있었다.

## 결정

- `workflow_run` 경로는 `github.event.workflow_run.head_sha`를 명시적으로
  checkout한다.
- 수동 dispatch는 environment branch policy가 허용한 dispatch SHA를
  checkout한다.
- 별도 validation run을 입력받는 workflow는 해당 run의 성공 여부, branch,
  `head_sha`를 함께 읽고 그 SHA를 publication job으로 전달한다.
- checkout 직후 `git rev-parse HEAD`를 expected SHA와 비교해 source identity를
  실행 증적으로 남긴다.

## 재발 방지

Snapshot 자동화 검토에서는 workflow run metadata만 비교하지 않는다. 반드시
trigger SHA, checkout `ref`, checkout 이후 SHA 검증, environment 대기 중 branch
변경 가능성을 한 세트로 확인한다. 이 네 조건이 일치하지 않으면 reviewer 제거와
publication을 보류한다.
